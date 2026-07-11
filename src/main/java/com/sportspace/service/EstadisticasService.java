package com.sportspace.service;

import com.sportspace.entity.*;
import com.sportspace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstadisticasService {

    private final UsuarioRepository  usuarioRepository;
    private final ReservaRepository  reservaRepository;
    private final PagoRepository     pagoRepository;
    private final CanchaRepository   canchaRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // 1. RESUMEN — /admin/estadisticas/resumen
    // ══════════════════════════════════════════════════════════════════════════

    public Map<String, Object> getResumen() {
        YearMonth mes = YearMonth.now();

        long totalUsuarios   = usuarioRepository.count();
        long totalReservas   = reservaRepository.count();
        long canchasActivas  = canchaRepository.countByActivaTrue();

        // Ingresos: solo pagos COMPLETADO con reserva NO cancelada
        BigDecimal ingresosTotal = pagoRepository.findAllOrderByCreatedAtDesc().stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETADO
                        && p.getReserva().getEstado() != EstadoReserva.CANCELADA)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ingresos del mes actual
        LocalDate inicioMes = mes.atDay(1);
        LocalDate finMes    = mes.atEndOfMonth();
        BigDecimal ingresosMes = pagoRepository.findAllOrderByCreatedAtDesc().stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETADO
                        && p.getReserva().getEstado() != EstadoReserva.CANCELADA
                        && p.getFechaPago() != null
                        && !p.getFechaPago().toLocalDate().isBefore(inicioMes)
                        && !p.getFechaPago().toLocalDate().isAfter(finMes))
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Nuevos usuarios este mes
        long nuevosUsuariosMes = usuarioRepository.countByCreatedAtBetween(
                mes.atDay(1).atStartOfDay(),
                mes.atEndOfMonth().atTime(23, 59, 59));

        // Reservas este mes
        long reservasMes = reservaRepository.findAll().stream()
                .filter(r -> {
                    LocalDate f = r.getFecha();
                    return f != null && !f.isBefore(inicioMes) && !f.isAfter(finMes);
                }).count();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalUsuarios",    totalUsuarios);
        res.put("totalReservas",    totalReservas);
        res.put("canchasActivas",   canchasActivas);
        res.put("ingresosTotal",    ingresosTotal);
        res.put("ingresosMes",      ingresosMes);
        res.put("nuevosUsuariosMes", nuevosUsuariosMes);
        res.put("reservasMes",      reservasMes);
        return res;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. USUARIOS MENSUAL — /admin/estadisticas/usuarios-mensual
    //    Últimos 6 meses
    // ══════════════════════════════════════════════════════════════════════════

    public Map<String, Object> getUsuariosMensual() {
        Locale localeEs = new Locale("es", "PE");
        List<Map<String, Object>> meses = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            long cantidad = usuarioRepository.countByCreatedAtBetween(
                    ym.atDay(1).atStartOfDay(),
                    ym.atEndOfMonth().atTime(23, 59, 59));

            String label = ym.getMonth()
                    .getDisplayName(TextStyle.SHORT, localeEs);
            label = Character.toUpperCase(label.charAt(0))
                    + label.substring(1).replace(".", "");

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label",    label);
            m.put("cantidad", cantidad);
            meses.add(m);
        }

        YearMonth inicio = YearMonth.now().minusMonths(5);
        YearMonth fin    = YearMonth.now();
        String rango = mesLabel(inicio, localeEs) + " – " + mesLabel(fin, localeEs);

        return Map.of("meses", meses, "rango", rango);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. INGRESOS MENSUAL — /admin/estadisticas/ingresos-mensual
    //    Últimos 6 meses (solo pagos aprobados, reservas no canceladas)
    // ══════════════════════════════════════════════════════════════════════════

    public Map<String, Object> getIngresosMensual() {
        Locale localeEs = new Locale("es", "PE");

        // Traer todos los pagos aprobados con reserva no cancelada una sola vez
        List<Pago> aprobados = pagoRepository.findAllOrderByCreatedAtDesc().stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETADO
                        && p.getReserva().getEstado() != EstadoReserva.CANCELADA
                        && p.getFechaPago() != null)
                .collect(Collectors.toList());

        List<Map<String, Object>> meses = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDate inicio = ym.atDay(1);
            LocalDate fin    = ym.atEndOfMonth();

            BigDecimal monto = aprobados.stream()
                    .filter(p -> {
                        LocalDate fp = p.getFechaPago().toLocalDate();
                        return !fp.isBefore(inicio) && !fp.isAfter(fin);
                    })
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String label = ym.getMonth()
                    .getDisplayName(TextStyle.SHORT, localeEs);
            label = Character.toUpperCase(label.charAt(0))
                    + label.substring(1).replace(".", "");

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", label);
            m.put("monto", monto);
            meses.add(m);
        }

        YearMonth inicio = YearMonth.now().minusMonths(5);
        YearMonth finYm  = YearMonth.now();
        String rango = mesLabel(inicio, localeEs) + " – " + mesLabel(finYm, localeEs);

        return Map.of("meses", meses, "rango", rango);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. CANCHAS TOP — /admin/estadisticas/canchas-top
    //    Top 5 canchas con más reservas
    // ══════════════════════════════════════════════════════════════════════════

    public Map<String, Object> getCanchasTop() {
        List<Reserva> todas = reservaRepository.findAll();

        Map<Long, Long> conteo = todas.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCancha().getId(), Collectors.counting()));

        List<Map<String, Object>> canchas = conteo.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Cancha ca = canchaRepository.findById(e.getKey()).orElse(null);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("nombre",   ca != null ? ca.getNombre() : "Cancha " + e.getKey());
                    item.put("deporte",  ca != null ? ca.getDeporte() : "");
                    item.put("reservas", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        return Map.of("canchas", canchas);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. DEPORTES — /admin/estadisticas/deportes
    // ══════════════════════════════════════════════════════════════════════════

    public Map<String, Object> getDeportes() {
        List<Object[]> rows = reservaRepository.countByDeporte();

        List<Map<String, Object>> deportes = rows.stream()
                .map(r -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("deporte",  r[0]);
                    item.put("reservas", ((Number) r[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());

        return Map.of("deportes", deportes);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. HORARIOS — /admin/estadisticas/horarios
    //    Top 5 bloques horarios más usados
    // ══════════════════════════════════════════════════════════════════════════

    public Map<String, Object> getHorarios() {
        List<Reserva> todas = reservaRepository.findAll().stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .collect(Collectors.toList());

        // Agrupar por bloque de hora de inicio (ej: "08:00 - 10:00")
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (Reserva r : todas) {
            if (r.getHoraInicio() == null || r.getHoraFin() == null) continue;
            String bloque = r.getHoraInicio() + " - " + r.getHoraFin();
            conteo.merge(bloque, 1L, Long::sum);
        }

        List<Map<String, Object>> horarios = conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("horario",  e.getKey());
                    item.put("reservas", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        return Map.of("horarios", horarios);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. DIAS SEMANA — /admin/estadisticas/dias-semana
    //    Reservas por día de la semana (Lun–Dom)
    // ══════════════════════════════════════════════════════════════════════════

    public Map<String, Object> getDiasSemana() {
        Locale localeEs = new Locale("es", "PE");

        List<Reserva> todas = reservaRepository.findAll().stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA
                        && r.getFecha() != null)
                .collect(Collectors.toList());

        // Orden Lun → Dom
        DayOfWeek[] orden = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };

        Map<DayOfWeek, Long> conteo = todas.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getFecha().getDayOfWeek(), Collectors.counting()));

        List<Map<String, Object>> dias = Arrays.stream(orden)
                .map(dow -> {
                    String label = dow.getDisplayName(TextStyle.SHORT, localeEs);
                    label = Character.toUpperCase(label.charAt(0))
                            + label.substring(1).replace(".", "");
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("dia",      label);
                    item.put("reservas", conteo.getOrDefault(dow, 0L));
                    return item;
                })
                .collect(Collectors.toList());

        return Map.of("dias", dias);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String mesLabel(YearMonth ym, Locale locale) {
        String mes = ym.getMonth().getDisplayName(TextStyle.SHORT, locale);
        return Character.toUpperCase(mes.charAt(0))
                + mes.substring(1).replace(".", "")
                + " " + ym.getYear();
    }
}