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
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/propietario/dashboard")
@PreAuthorize("hasRole('PROPIETARIO')")
@RequiredArgsConstructor
public class PropietarioDashboardController {

    private final CanchaRepository   canchaRepo;
    private final ReservaRepository  reservaRepo;
    private final PagoRepository     pagoRepo;
    private final UsuarioRepository  usuarioRepo;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(
            @AuthenticationPrincipal UserDetails ud) {
        Usuario prop = getPropietario(ud);
        List<Long> canchaIds = canchaIds(prop.getId());

        LocalDate hoy    = LocalDate.now();
        LocalDate inicio = hoy.withDayOfMonth(1);

        List<Reserva> reservasMes = reservaRepo.findAll().stream()
                .filter(r -> canchaIds.contains(r.getCancha().getId()))
                .filter(r -> !r.getFecha().isBefore(inicio) && !r.getFecha().isAfter(hoy))
                .toList();

        // Solo pagos COMPLETADOS de reservas NO canceladas
        BigDecimal ingresosMes = pagoRepo.findAll().stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETADO)
                .filter(p -> p.getReserva().getEstado() != EstadoReserva.CANCELADA)
                .filter(p -> canchaIds.contains(p.getReserva().getCancha().getId()))
                .filter(p -> p.getFechaPago() != null &&
                        !p.getFechaPago().toLocalDate().isBefore(inicio))
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate inicioAnt = inicio.minusMonths(1);
        BigDecimal ingresosMesAnt = pagoRepo.findAll().stream()
                .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETADO)
                .filter(p -> p.getReserva().getEstado() != EstadoReserva.CANCELADA)
                .filter(p -> canchaIds.contains(p.getReserva().getCancha().getId()))
                .filter(p -> p.getFechaPago() != null &&
                        !p.getFechaPago().toLocalDate().isBefore(inicioAnt) &&
                        p.getFechaPago().toLocalDate().isBefore(inicio))
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long reservasHoy = reservaRepo.findAll().stream()
                .filter(r -> canchaIds.contains(r.getCancha().getId()))
                .filter(r -> r.getFecha().equals(hoy)).count();

        long pendientes = reservaRepo.findAll().stream()
                .filter(r -> canchaIds.contains(r.getCancha().getId()))
                .filter(r -> r.getEstado() == EstadoReserva.PENDIENTE).count();

        long canchasActivas = canchaRepo.findByPropietarioId(prop.getId()).stream()
                .filter(c -> "ACTIVA".equals(c.getEstado()) || "DESTACADA".equals(c.getEstado())).count();

        long canchasTotal = canchaRepo.countByPropietarioId(prop.getId());

        Set<Long> clientesUnicos = reservasMes.stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .map(r -> r.getCliente().getId())
                .collect(java.util.stream.Collectors.toSet());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ingresosMes",         ingresosMes);
        m.put("ingresosMesAnterior", ingresosMesAnt);
        m.put("reservasHoy",         reservasHoy);
        m.put("reservasPendientes",  pendientes);
        m.put("canchasActivas",      canchasActivas);
        m.put("canchasTotal",        canchasTotal);
        m.put("clientesUnicos",      clientesUnicos.size());
        return ResponseEntity.ok(m);
    }

    @GetMapping("/ingresos-semana")
    public ResponseEntity<Map<String, Object>> ingresosSemana(
            @AuthenticationPrincipal UserDetails ud) {
        Long propId = getPropietario(ud).getId();
        List<Long> canchaIds = canchaIds(propId);

        List<Map<String, Object>> dias = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate fecha = LocalDate.now().minusDays(i);
            // Solo confirmadas, no canceladas
            BigDecimal total = pagoRepo.findAll().stream()
                    .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETADO)
                    .filter(p -> p.getReserva().getEstado() != EstadoReserva.CANCELADA)
                    .filter(p -> canchaIds.contains(p.getReserva().getCancha().getId()))
                    .filter(p -> p.getFechaPago() != null &&
                            p.getFechaPago().toLocalDate().equals(fecha))
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("fecha", fecha);
            d.put("total", total);
            dias.add(d);
        }
        return ResponseEntity.ok(Map.of("dias", dias));
    }

    @GetMapping("/ocupacion-hoy")
    public ResponseEntity<List<Map<String, Object>>> ocupacionHoy(
            @AuthenticationPrincipal UserDetails ud) {
        Usuario prop = getPropietario(ud);
        LocalDate hoy = LocalDate.now();
        List<Map<String, Object>> result = canchaRepo.findByPropietarioId(prop.getId())
                .stream()
                .filter(c -> "ACTIVA".equals(c.getEstado()) || "DESTACADA".equals(c.getEstado()))
                .map(c -> {
                    long ocupadas = reservaRepo
                            .findByCanchaIdAndFechaAndEstadoNot(c.getId(), hoy, EstadoReserva.CANCELADA)
                            .stream()
                            .filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA)
                            .count();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",             c.getId());
                    m.put("nombre",         c.getNombre());
                    m.put("horasOcupadas",  ocupadas);
                    m.put("totalHoras",     8);
                    return m;
                }).toList();
        return ResponseEntity.ok(result);
    }

    private List<Long> canchaIds(Long propId) {
        return canchaRepo.findByPropietarioId(propId).stream().map(c -> c.getId()).toList();
    }

    private Usuario getPropietario(UserDetails ud) {
        return usuarioRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}