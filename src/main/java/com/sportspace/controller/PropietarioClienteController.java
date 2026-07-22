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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/propietario/clientes")
@PreAuthorize("hasRole('PROPIETARIO')")
@RequiredArgsConstructor
public class PropietarioClienteController {

    private final CanchaRepository  canchaRepo;
    private final ReservaRepository reservaRepo;
    private final PagoRepository    pagoRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @AuthenticationPrincipal UserDetails ud) {
        Long propId = getPropietario(ud).getId();
        List<Long> canchaIds = canchaRepo.findByPropietarioId(propId).stream().map(c -> c.getId()).toList();

        // Agrupar reservas por cliente
        List<Reserva> todasReservas = reservaRepo.findAll().stream()
                .filter(r -> canchaIds.contains(r.getCancha().getId()))
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .toList();

        Map<Long, List<Reserva>> porCliente = todasReservas.stream()
                .collect(Collectors.groupingBy(r -> r.getCliente().getId()));

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);

        List<Map<String, Object>> clientes = porCliente.entrySet().stream()
                .map(e -> {
                    Usuario cliente = e.getValue().get(0).getCliente();
                    List<Reserva> reservas = e.getValue();

                    // Total gastado
                    BigDecimal gastado = pagoRepo.findAll().stream()
                            .filter(p -> p.getEstado() == Pago.EstadoPago.COMPLETADO)
                            .filter(p -> reservas.stream().anyMatch(r -> r.getId().equals(p.getReserva().getId())))
                            .map(Pago::getMonto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Cancha más reservada
                    String canchaFav = reservas.stream()
                            .collect(Collectors.groupingBy(r -> r.getCancha().getNombre(), Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey).orElse("—");

                    // Última visita
                    LocalDate ultimaVisita = reservas.stream()
                            .map(Reserva::getFecha)
                            .filter(Objects::nonNull)
                            .max(Comparator.naturalOrder()).orElse(null);

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",             cliente.getId());
                    m.put("nombres",        cliente.getNombres());
                    m.put("apellidos",      cliente.getApellidos());
                    m.put("email",          cliente.getEmail());
                    m.put("telefono",       cliente.getTelefono());
                    m.put("totalReservas",  reservas.size());
                    m.put("totalGastado",   gastado);
                    m.put("canchaFavorita", canchaFav);
                    m.put("ultimaVisita",   ultimaVisita);
                    return m;
                })
                .sorted((a, b) -> Integer.compare(
                        (int) b.get("totalReservas"), (int) a.get("totalReservas")))
                .toList();

        // Estadisticas globales
        long frecuentes = clientes.stream().filter(c -> (int) c.get("totalReservas") >= 10).count();
        long nuevos     = porCliente.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(r -> r.getFecha() != null && !r.getFecha().isBefore(inicioMes)))
                .count();
        BigDecimal promedioGasto = clientes.isEmpty() ? BigDecimal.ZERO :
                clientes.stream().map(c -> (BigDecimal) c.get("totalGastado"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(clientes.size()), 2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalClientes",      clientes.size());
        result.put("clientesFrecuentes", frecuentes);
        result.put("clientesNuevosMes",  nuevos);
        result.put("gastoPromedio",      promedioGasto);
        result.put("clientes",           clientes);
        return ResponseEntity.ok(result);
    }

    private Usuario getPropietario(UserDetails ud) {
        return usuarioRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}