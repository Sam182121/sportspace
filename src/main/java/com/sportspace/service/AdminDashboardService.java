package com.sportspace.service;

import com.sportspace.dto.response.dashboard.admin.*;
import com.sportspace.entity.*;
import com.sportspace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UsuarioRepository  usuarioRepository;
    private final CanchaRepository   canchaRepository;
    private final ReservaRepository  reservaRepository;
    private final PagoRepository     pagoRepository;

    private static final DateTimeFormatter FMT_FECHA     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_ISO       = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DashboardStatsResponse getStats() {

        LocalDate hoy = LocalDate.now();
        YearMonth mes = YearMonth.now();

        Long totalUsuarios     = usuarioRepository.count();
        Long totalPropietarios = usuarioRepository.countByRol(Rol.PROPIETARIO);
        Long nuevosEsteMes     = usuarioRepository.countByCreatedAtBetween(
                mes.atDay(1).atStartOfDay(),
                mes.atEndOfMonth().atTime(23, 59, 59));

        Long totalCanchas   = canchaRepository.count();
        Long canchasActivas = canchaRepository.countByActivaTrue();

        Long reservasHoy        = reservaRepository.countByFecha(hoy);
        Long reservasPendientes = reservaRepository.countByEstado(EstadoReserva.PENDIENTE);
        Long partidosActivos    = reservaRepository.countByFechaAndEstado(hoy, EstadoReserva.CONFIRMADA);

        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia    = hoy.atTime(23, 59, 59);
        LocalDateTime inicioMes = mes.atDay(1).atStartOfDay();
        LocalDateTime finMes    = mes.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal ingresosDia = pagoRepository.sumIngresosEnPeriodo(inicioDia, finDia);
        BigDecimal ingresosMes = pagoRepository.sumIngresosEnPeriodo(inicioMes, finMes);

        boolean tieneAlertas = reservasPendientes > 0 || canchaRepository.countByActivaFalse() > 0;

        return DashboardStatsResponse.builder()
                .totalUsuarios(totalUsuarios)
                .totalPropietarios(totalPropietarios)
                .totalCanchas(totalCanchas)
                .canchasActivas(canchasActivas)
                .reservasHoy(reservasHoy)
                .reservasPendientes(reservasPendientes)
                .ingresosDia(ingresosDia)
                .ingresosMes(ingresosMes)
                .partidosActivos(partidosActivos)
                .usuariosConectados(0L)
                .nuevosEsteMes(nuevosEsteMes)
                .propietariosPendientes(0L)
                .tieneAlertas(tieneAlertas)
                .build();
    }

    public DashboardIngresosSemanaResponse getIngresosSemana() {

        LocalDate hoy    = LocalDate.now();
        LocalDate inicio = hoy.minusDays(6);
        LocalDateTime desde = inicio.atStartOfDay();

        List<Pago> pagos = pagoRepository.findCompletadosDesde(desde);

        Map<LocalDate, BigDecimal> mapaIngresos = new HashMap<>();
        for (Pago p : pagos) {
            if (p.getFechaPago() == null) continue;
            LocalDate dia = p.getFechaPago().toLocalDate();
            mapaIngresos.merge(dia, p.getMonto(), BigDecimal::add);
        }

        List<DashboardIngresosSemanaResponse.DiaMonto> dias = new ArrayList<>();
        Locale localeEs = new Locale("es", "PE");
        for (int i = 0; i < 7; i++) {
            LocalDate dia = inicio.plusDays(i);
            String label = dia.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, localeEs);
            label = Character.toUpperCase(label.charAt(0)) + label.substring(1).replace(".", "");
            dias.add(DashboardIngresosSemanaResponse.DiaMonto.builder()
                    .label(label)
                    .monto(mapaIngresos.getOrDefault(dia, BigDecimal.ZERO))
                    .build());
        }

        DateTimeFormatter fmtCorto = DateTimeFormatter.ofPattern("d MMM", localeEs);
        String rango = inicio.format(fmtCorto) + " – " + hoy.format(fmtCorto);

        return DashboardIngresosSemanaResponse.builder()
                .rango(rango)
                .dias(dias)
                .build();
    }

    public DashboardReservasDeporteResponse getReservasPorDeporte() {

        List<Object[]> rows = reservaRepository.countByDeporte();
        long granTotal = rows.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        List<DashboardReservasDeporteResponse.DeporteStats> deportes = rows.stream()
                .map(r -> {
                    String deporte  = (String) r[0];
                    long   total    = ((Number) r[1]).longValue();
                    int porcentaje  = granTotal > 0
                            ? (int) Math.round((total * 100.0) / granTotal) : 0;
                    return DashboardReservasDeporteResponse.DeporteStats.builder()
                            .deporte(deporte)
                            .total(total)
                            .porcentaje(porcentaje)
                            .build();
                })
                .collect(Collectors.toList());

        return DashboardReservasDeporteResponse.builder()
                .deportes(deportes)
                .build();
    }

    public DashboardActividadResponse getActividadReciente() {

        List<DashboardActividadResponse.ActividadItem> actividad =
                reservaRepository.findTopRecientes().stream()
                        .limit(10)
                        .map(r -> {
                            boolean cancelada = r.getEstado() == EstadoReserva.CANCELADA;
                            return DashboardActividadResponse.ActividadItem.builder()
                                    .tipo(cancelada ? "cancelacion" : "reserva")
                                    .titulo(cancelada ? "Reserva cancelada" : "Nueva reserva")
                                    .subtitulo(r.getCliente().getNombres() + " — "
                                            + r.getCancha().getNombre()
                                            + " " + r.getFecha().format(FMT_FECHA))
                                    .tiempo(tiempoRelativo(r.getCreatedAt()))
                                    .color(cancelada ? "red" : "blue")
                                    .build();
                        })
                        .collect(Collectors.toList());

        return DashboardActividadResponse.builder().actividad(actividad).build();
    }

    public DashboardAlertasResponse getAlertas() {

        List<DashboardAlertasResponse.AlertaItem> alertas = new ArrayList<>();

        Long pendientes = reservaRepository.countByEstado(EstadoReserva.PENDIENTE);
        if (pendientes > 0)
            alertas.add(alerta("warning", pendientes + " reserva(s) pendiente(s) de confirmación"));

        Long canchasInactivas = canchaRepository.countByActivaFalse();
        if (canchasInactivas > 0)
            alertas.add(alerta("info", canchasInactivas + " cancha(s) desactivada(s)"));

        Long usuariosInactivos = usuarioRepository.countByActivoFalse();
        if (usuariosInactivos > 0)
            alertas.add(alerta("info", usuariosInactivos + " usuario(s) inactivo(s)"));

        // Alerta de pagos pendientes
        long pagosPendientes = pagoRepository.findAllOrderByCreatedAtDesc().stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.PENDIENTE)
                .count();
        if (pagosPendientes > 0)
            alertas.add(alerta("warning", pagosPendientes + " pago(s) pendiente(s) de verificación"));

        if (alertas.isEmpty())
            alertas.add(alerta("info", "Todo está en orden. Sin alertas pendientes."));

        return DashboardAlertasResponse.builder().alertas(alertas).build();
    }

    public DashboardUltimasReservasResponse getUltimasReservas() {

        List<DashboardUltimasReservasResponse.ReservaRow> rows =
                reservaRepository.findAllOrderByCreatedAtDesc().stream()
                        .limit(10)
                        .map(r -> DashboardUltimasReservasResponse.ReservaRow.builder()
                                .id(r.getId())
                                .usuarioNombre(r.getCliente().getNombres()
                                        + " " + r.getCliente().getApellidos())
                                .canchaNombre(r.getCancha().getNombre())
                                .fecha(r.getFecha().format(FMT_FECHA))
                                .total(r.getTotal())
                                .estado(r.getEstado().name())
                                .build())
                        .collect(Collectors.toList());

        return DashboardUltimasReservasResponse.builder().reservas(rows).build();
    }

    /**
     * Últimos pagos para el dashboard.
     * FIX: se agregan usuarioEmail, reservaId, fecha y se mapea
     *      COMPLETADO → APROBADO para que el JS del frontend lo reconozca.
     */
    public DashboardUltimosPagosResponse getUltimosPagos() {
        List<DashboardUltimosPagosResponse.PagoRow> rows =
                pagoRepository.findAllOrderByCreatedAtDesc().stream()
                        .limit(10)
                        .map(this::mapPagoRow)
                        .collect(Collectors.toList());
        return DashboardUltimosPagosResponse.builder().pagos(rows).build();
    }

    /**
     * Todos los pagos sin limite — usado por la pagina pagos.html del admin.
     */
    public DashboardUltimosPagosResponse getTodosPagos() {
        List<DashboardUltimosPagosResponse.PagoRow> rows =
                pagoRepository.findAllOrderByCreatedAtDesc().stream()
                        .map(this::mapPagoRow)
                        .collect(Collectors.toList());
        return DashboardUltimosPagosResponse.builder().pagos(rows).build();
    }

    /**
     * Logica de mapeo de estado para el frontend:
     *   COMPLETADO + reserva CONFIRMADA  -> APROBADO      (ingreso real)
     *   COMPLETADO + reserva CANCELADA   -> EN_REEMBOLSO  (no cuenta como ingreso)
     *   REEMBOLSADO                      -> REEMBOLSADO
     *   PENDIENTE / RECHAZADO            -> sin cambio
     */
    private DashboardUltimosPagosResponse.PagoRow mapPagoRow(Pago p) {
        boolean reservaCancelada = p.getReserva().getEstado() == EstadoReserva.CANCELADA;

        String estadoJs;
        if (p.getEstado() == Pago.EstadoPago.REEMBOLSADO) {
            estadoJs = "REEMBOLSADO";
        } else if (p.getEstado() == Pago.EstadoPago.COMPLETADO && reservaCancelada) {
            estadoJs = "EN_REEMBOLSO";
        } else if (p.getEstado() == Pago.EstadoPago.COMPLETADO) {
            estadoJs = "APROBADO";
        } else {
            estadoJs = p.getEstado().name();
        }

        String fecha = p.getFechaPago() != null
                ? p.getFechaPago().format(FMT_ISO)
                : (p.getCreatedAt() != null ? p.getCreatedAt().format(FMT_ISO) : "");

        return DashboardUltimosPagosResponse.PagoRow.builder()
                .id(p.getId())
                .usuarioNombre(p.getReserva().getCliente().getNombres()
                        + " " + p.getReserva().getCliente().getApellidos())
                .usuarioEmail(p.getReserva().getCliente().getEmail())
                .metodo(p.getMetodo() != null ? p.getMetodo().name() : "—")
                .monto(p.getMonto())
                .estado(estadoJs)
                .fecha(fecha)
                .reservaId(p.getReserva().getId())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DashboardAlertasResponse.AlertaItem alerta(String tipo, String mensaje) {
        return DashboardAlertasResponse.AlertaItem.builder()
                .tipo(tipo).mensaje(mensaje).build();
    }

    private String tiempoRelativo(LocalDateTime dt) {
        if (dt == null) return "";
        long mins = Duration.between(dt, LocalDateTime.now()).toMinutes();
        if (mins < 1)   return "Ahora mismo";
        if (mins < 60)  return "Hace " + mins + " min";
        long horas = mins / 60;
        if (horas < 24) return "Hace " + horas + "h";
        return "Hace " + (horas / 24) + " día(s)";
    }
}