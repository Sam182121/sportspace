package com.sportspace.controller;

import com.sportspace.entity.*;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/propietario/pagos")
@PreAuthorize("hasRole('PROPIETARIO')")
@RequiredArgsConstructor
public class PropietarioPagoController {

    private final CanchaRepository  canchaRepo;
    private final PagoRepository    pagoRepo;
    private final ReservaRepository reservaRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> resumen(
            @RequestParam(defaultValue = "mes") String periodo,
            @AuthenticationPrincipal UserDetails ud) {
        Long propId = getPropietario(ud).getId();
        List<Long> canchaIds = canchaRepo.findByPropietarioId(propId).stream().map(c -> c.getId()).toList();

        // Todos los pagos de las canchas del propietario
        List<Pago> pagos = pagoRepo.findAll().stream()
                .filter(p -> canchaIds.contains(p.getReserva().getCancha().getId()))
                .toList();

        LocalDate hoy    = LocalDate.now();
        LocalDate lunSem = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1L);
        LocalDate iniMes = hoy.withDayOfMonth(1);
        LocalDate iniMesAnt = iniMes.minusMonths(1);
        LocalDate finMesAnt = iniMes.minusDays(1);
        LocalDate iniSemAnt = lunSem.minusWeeks(1);
        LocalDate finSemAnt = lunSem.minusDays(1);

        // Solo pagos COMPLETADOS para ingresos reales (canceladas no cuentan)
        BigDecimal hoyTotal    = sumar(pagos, hoy, hoy);
        BigDecimal semTotal    = sumar(pagos, lunSem, hoy);
        BigDecimal mesTotal    = sumar(pagos, iniMes, hoy);
        BigDecimal semAntTotal = sumar(pagos, iniSemAnt, finSemAnt);
        BigDecimal mesAntTotal = sumar(pagos, iniMesAnt, finMesAnt);

        BigDecimal pendiente = pagos.stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.PENDIENTE)
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        long reservasPendVer = pagos.stream().filter(p -> p.getEstado() == Pago.EstadoPago.PENDIENTE).count();

        int varSem = porcentaje(semAntTotal, semTotal);
        int varMes = porcentaje(mesAntTotal, mesTotal);

        // Ingresos por cancha (mes actual, solo COMPLETADO — no reembolsos)
        Map<Long, BigDecimal> porCancha = pagos.stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETADO &&
                        p.getFechaPago() != null &&
                        !p.getFechaPago().toLocalDate().isBefore(iniMes))
                .collect(Collectors.groupingBy(
                        p -> p.getReserva().getCancha().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Pago::getMonto, BigDecimal::add)));

        List<Map<String, Object>> ingCancha = canchaRepo.findByPropietarioId(propId).stream()
                .map(c -> {
                    BigDecimal t = porCancha.getOrDefault(c.getId(), BigDecimal.ZERO);
                    long res = pagos.stream()
                            .filter(p -> p.getReserva().getCancha().getId().equals(c.getId()) &&
                                    p.getEstado() == Pago.EstadoPago.COMPLETADO &&
                                    p.getFechaPago() != null &&
                                    !p.getFechaPago().toLocalDate().isBefore(iniMes))
                            .count();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",       c.getId());
                    m.put("nombre",   c.getNombre());
                    m.put("total",    t);
                    m.put("reservas", res);
                    return m;
                }).sorted((a, b) -> ((BigDecimal) b.get("total")).compareTo((BigDecimal) a.get("total"))).toList();

        BigDecimal maxIng = ingCancha.stream().map(m -> (BigDecimal) m.get("total"))
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);

        // Determinar rango según periodo elegido
        LocalDate desdeFiltro = switch (periodo) {
            case "hoy"    -> hoy;
            case "semana" -> lunSem;
            case "3meses" -> iniMes.minusMonths(2);
            default       -> iniMes; // "mes"
        };

        // Últimos pagos filtrados por periodo — incluye TODOS los estados para mostrar reembolsos
        List<Map<String, Object>> ultimos = pagos.stream()
                .filter(p -> {
                    LocalDate refDate = p.getFechaPago() != null
                            ? p.getFechaPago().toLocalDate()
                            : (p.getCreatedAt() != null ? p.getCreatedAt().toLocalDate() : null);
                    if (refDate == null) return false;
                    return !refDate.isBefore(desdeFiltro) && !refDate.isAfter(hoy);
                })
                .sorted((a, b) -> {
                    LocalDate da = a.getFechaPago() != null ? a.getFechaPago().toLocalDate() : a.getCreatedAt().toLocalDate();
                    LocalDate db = b.getFechaPago() != null ? b.getFechaPago().toLocalDate() : b.getCreatedAt().toLocalDate();
                    return db.compareTo(da);
                })
                .limit(30)
                .map(p -> {
                    boolean cancelada = p.getReserva().getEstado() == EstadoReserva.CANCELADA;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",             p.getId());
                    m.put("reservaId",      p.getReserva().getId());
                    m.put("clienteNombre",  p.getReserva().getCliente().getNombres() + " " + p.getReserva().getCliente().getApellidos());
                    m.put("clienteEmail",   p.getReserva().getCliente().getEmail());
                    m.put("canchaName",     p.getReserva().getCancha().getNombre());
                    m.put("fecha",          p.getFechaPago() != null ? p.getFechaPago().toLocalDate() : (p.getCreatedAt() != null ? p.getCreatedAt().toLocalDate() : hoy));
                    m.put("metodoPago",     p.getMetodo());
                    m.put("monto",          p.getMonto());
                    m.put("estadoPago",     p.getEstado().name());       // COMPLETADO, PENDIENTE, REEMBOLSADO, RECHAZADO
                    m.put("estadoReserva",  p.getReserva().getEstado().name()); // CANCELADA, etc.
                    m.put("cancelada",      cancelada);
                    m.put("verificado",     p.getEstado() == Pago.EstadoPago.COMPLETADO && !cancelada);
                    return m;
                }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ingresoHoy",           hoyTotal);
        result.put("reservasHoy",          pagos.stream().filter(p -> p.getFechaPago() != null && p.getFechaPago().toLocalDate().equals(hoy) && p.getEstado() == Pago.EstadoPago.COMPLETADO).count());
        result.put("ingresoSemana",        semTotal);
        result.put("variacionSemana",      varSem);
        result.put("pctSemana",            semAntTotal.compareTo(BigDecimal.ZERO) > 0 ? semTotal.multiply(BigDecimal.valueOf(100)).divide(semAntTotal.add(semTotal), 0, RoundingMode.HALF_UP).intValue() : 100);
        result.put("ingresoMes",           mesTotal);
        result.put("variacionMes",         varMes);
        result.put("pctMes",               mesAntTotal.compareTo(BigDecimal.ZERO) > 0 ? mesTotal.multiply(BigDecimal.valueOf(100)).divide(mesAntTotal.add(mesTotal), 0, RoundingMode.HALF_UP).intValue() : 100);
        result.put("ingresoPendiente",     pendiente);
        result.put("reservasPendientesVerif", reservasPendVer);
        result.put("ingresosPorCancha",    ingCancha);
        result.put("maxIngreso",           maxIng);
        result.put("ultimosPagos",         ultimos);
        result.put("periodo",              periodo);
        return ResponseEntity.ok(result);
    }

    private BigDecimal sumar(List<Pago> pagos, LocalDate desde, LocalDate hasta) {
        return pagos.stream()
                // Solo COMPLETADO y cuya reserva NO esté cancelada
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETADO &&
                        p.getFechaPago() != null &&
                        p.getReserva().getEstado() != EstadoReserva.CANCELADA)
                .filter(p -> {
                    LocalDate d = p.getFechaPago().toLocalDate();
                    return !d.isBefore(desde) && !d.isAfter(hasta);
                })
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int porcentaje(BigDecimal antes, BigDecimal ahora) {
        if (antes.compareTo(BigDecimal.ZERO) == 0) return ahora.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        return ahora.subtract(antes).multiply(BigDecimal.valueOf(100)).divide(antes, 0, RoundingMode.HALF_UP).intValue();
    }

    private Usuario getPropietario(UserDetails ud) {
        return usuarioRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}