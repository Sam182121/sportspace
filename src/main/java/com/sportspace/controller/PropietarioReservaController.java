package com.sportspace.controller;

import com.sportspace.entity.*;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.*;
import com.sportspace.service.EmailService;
import com.sportspace.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/propietario/reservas")
@PreAuthorize("hasRole('PROPIETARIO')")
@RequiredArgsConstructor
public class PropietarioReservaController {

    private final ReservaRepository  reservaRepo;
    private final CanchaRepository   canchaRepo;
    private final UsuarioRepository  usuarioRepo;
    private final PagoRepository     pagoRepo;
    private final EmailService       emailService;
    private final NotificacionService notificacionService;

    // ── GET /api/propietario/reservas?estado=PENDIENTE ───────────
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(
            @RequestParam(required = false) String estado,
            @AuthenticationPrincipal UserDetails ud) {

        Long propId = getPropietario(ud).getId();
        List<Long> canchaIds = canchaRepo.findByPropietarioId(propId)
                .stream().map(c -> c.getId()).toList();

        List<Reserva> reservas = reservaRepo.findAll().stream()
                .filter(r -> canchaIds.contains(r.getCancha().getId()))
                .filter(r -> {
                    if (estado == null || estado.isBlank()) return true;
                    String[] estados = estado.split(",");
                    for (String e : estados) {
                        try { if (r.getEstado() == EstadoReserva.valueOf(e.trim())) return true; }
                        catch (Exception ex) { /* ignora */ }
                    }
                    return false;
                })
                .sorted((a, b) -> {
                    if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                    int cmp = b.getFecha().compareTo(a.getFecha());
                    return cmp != 0 ? cmp : b.getHoraInicio().compareTo(a.getHoraInicio());
                })
                .toList();

        return ResponseEntity.ok(reservas.stream().map(this::toMap).toList());
    }

    // ── GET /api/propietario/reservas/{id} ───────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detalle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        Reserva r = validarAcceso(id, ud);
        return ResponseEntity.ok(toMapDetalle(r));
    }

    // ── PATCH /api/propietario/reservas/{id}/aprobar ─────────────
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        Reserva r = validarAcceso(id, ud);
        if (r.getEstado() != EstadoReserva.PENDIENTE)
            throw new BadRequestException("Solo se pueden aprobar reservas PENDIENTE");
        r.setEstado(EstadoReserva.CONFIRMADA);
        r.setFechaRespuestaPropietario(LocalDateTime.now());
        reservaRepo.save(r);
        pagoRepo.findByReservaId(r.getId()).ifPresent(p -> {
            p.setEstado(Pago.EstadoPago.COMPLETADO);
            if (p.getFechaPago() == null) p.setFechaPago(LocalDateTime.now());
            pagoRepo.save(p);
        });

        try {
            emailService.enviarReservaAprobada(
                    r.getCliente().getEmail(),
                    nombreMostrar(r.getCliente()),
                    codigoReserva(r),
                    r.getCancha().getNombre(),
                    r.getCancha().getDeporte(),
                    ubicacion(r.getCancha()),
                    fechaFormateada(r),
                    horario(r),
                    "S/ " + r.getTotal()
            );
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de reserva aprobada: {}", e.getMessage());
        }

        try {
            notificacionService.crear(
                    r.getCliente(),
                    Notificacion.TipoNotificacion.RESERVA_APROBADA,
                    "✅ Tu reserva fue aprobada",
                    "%s aprobó tu reserva en \"%s\" para el %s de %s a %s".formatted(
                            codigoReserva(r), r.getCancha().getNombre(),
                            r.getFecha(), r.getHoraInicio(), r.getHoraFin()),
                    r.getId()
            );
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de reserva aprobada: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of("mensaje", "Reserva aprobada"));
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazar(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails ud) {
        Reserva r = validarAcceso(id, ud);
        if (r.getEstado() != EstadoReserva.PENDIENTE)
            throw new BadRequestException("Solo se pueden rechazar reservas PENDIENTE");

        boolean reembolsar = Boolean.TRUE.equals(body.get("reembolsar"));
        String motivo  = String.valueOf(body.getOrDefault("motivo", "")).trim();
        String mensaje = String.valueOf(body.getOrDefault("mensaje", "")).trim();

        if (motivo.isBlank())
            throw new BadRequestException("Debes elegir un motivo de rechazo");
        if (motivo.equalsIgnoreCase("otro") && mensaje.isBlank())
            throw new BadRequestException("Debes escribir un mensaje cuando el motivo es 'Otro'");

        r.setEstado(EstadoReserva.CANCELADA);
        r.setCanceladoPor(Reserva.CanceladoPor.PROPIETARIO);
        r.setFechaRespuestaPropietario(LocalDateTime.now());
        r.setMotivoRechazo((motivo + " " + mensaje).trim());
        reservaRepo.save(r);

        pagoRepo.findByReservaId(r.getId()).ifPresent(p -> {
            p.setEstado(reembolsar ? Pago.EstadoPago.COMPLETADO : Pago.EstadoPago.RECHAZADO);
            if (reembolsar && p.getFechaPago() == null) p.setFechaPago(LocalDateTime.now());
            p.setNotas((motivo + " " + mensaje).trim());
            pagoRepo.save(p);
        });

        try {
            emailService.enviarReservaRechazada(
                    r.getCliente().getEmail(),
                    nombreMostrar(r.getCliente()),
                    codigoReserva(r),
                    r.getCancha().getNombre(),
                    ubicacion(r.getCancha()),
                    fechaFormateada(r),
                    horario(r),
                    motivo,
                    mensaje
            );
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de reserva rechazada: {}", e.getMessage());
        }

        try {
            notificacionService.crear(
                    r.getCliente(),
                    Notificacion.TipoNotificacion.RESERVA_RECHAZADA,
                    "❌ Tu reserva fue rechazada",
                    "%s rechazó tu reserva en \"%s\" del %s. Motivo: %s".formatted(
                            codigoReserva(r), r.getCancha().getNombre(), r.getFecha(), motivo),
                    r.getId()
            );
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de reserva rechazada: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "mensaje", "Reserva rechazada",
                "enReembolso", reembolsar
        ));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Map<String, Object>> cancelarConfirmada(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails ud) {
        Reserva r = validarAcceso(id, ud);

        if (r.getEstado() != EstadoReserva.CONFIRMADA)
            throw new BadRequestException(
                    "Solo se pueden cancelar reservas ya CONFIRMADAS. " +
                            "Para una reserva PENDIENTE usa 'rechazar'.");

        LocalDateTime inicioPartido = LocalDateTime.of(r.getFecha(), r.getHoraInicio());
        if (!inicioPartido.isAfter(LocalDateTime.now()))
            throw new BadRequestException(
                    "No puedes cancelar: el horario de esta reserva ya comenzó o pasó");

        String motivo  = String.valueOf(body.getOrDefault("motivo", "")).trim();
        String mensaje = String.valueOf(body.getOrDefault("mensaje", "")).trim();
        if (motivo.isBlank())
            throw new BadRequestException("Debes indicar el motivo de la cancelación");
        if (motivo.equalsIgnoreCase("otro") && mensaje.isBlank())
            throw new BadRequestException("Debes escribir un mensaje cuando el motivo es 'Otro'");

        r.setEstado(EstadoReserva.CANCELADA);
        r.setCanceladoPor(Reserva.CanceladoPor.PROPIETARIO);
        r.setFechaRespuestaPropietario(LocalDateTime.now());
        r.setMotivoRechazo((motivo + " " + mensaje).trim());
        reservaRepo.save(r);

        // El pago se mantiene en COMPLETADO → queda automáticamente "en
        // reembolso" mediante el mismo mecanismo usado cuando cancela el cliente.
        pagoRepo.findByReservaId(r.getId()).ifPresent(p -> {
            p.setNotas((motivo + " " + mensaje).trim());
            pagoRepo.save(p);
        });

        try {
            emailService.enviarReservaCanceladaPorPropietario(
                    r.getCliente().getEmail(),
                    nombreMostrar(r.getCliente()),
                    r.getCancha().getPropietario().getNombres() + " " + r.getCancha().getPropietario().getApellidos(),
                    codigoReserva(r),
                    r.getCancha().getNombre(),
                    ubicacion(r.getCancha()),
                    fechaFormateada(r),
                    horario(r),
                    motivo,
                    mensaje
            );
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de reserva cancelada: {}", e.getMessage());
        }

        try {
            notificacionService.crear(
                    r.getCliente(),
                    Notificacion.TipoNotificacion.RESERVA_CANCELADA_PROPIETARIO,
                    "⚠️ Tu reserva fue cancelada por el propietario",
                    "%s canceló tu reserva en \"%s\" del %s. Motivo: %s".formatted(
                            codigoReserva(r), r.getCancha().getNombre(), r.getFecha(), motivo),
                    r.getId()
            );
        } catch (Exception e) {
            log.error("No se pudo crear la notificación de reserva cancelada: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of("mensaje", "Reserva cancelada, pendiente de reembolso"));
    }

    // ── Helpers ──────────────────────────────────────────────────
    private Map<String, Object> toMap(Reserva r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             r.getId());
        m.put("canchaId",       r.getCancha().getId());
        m.put("canchaName",     r.getCancha().getNombre());
        m.put("clienteNombre",  r.getCliente().getNombres() + " " + r.getCliente().getApellidos());
        m.put("clienteEmail",   r.getCliente().getEmail());
        m.put("fecha",          r.getFecha());
        m.put("horaInicio",     r.getHoraInicio());
        m.put("horaFin",        r.getHoraFin());
        m.put("estado",         r.getEstado());
        m.put("canceladoPor",   r.getCanceladoPor());
        m.put("montoTotal",     r.getTotal());
        m.put("createdAt",      r.getCreatedAt());
        m.put("fechaRespuestaPropietario", r.getFechaRespuestaPropietario());
        m.put("motivoRechazo",  r.getMotivoRechazo());
        pagoRepo.findByReservaId(r.getId()).ifPresent(p -> {
            m.put("pagoId",      p.getId());
            m.put("metodoPago",  p.getMetodo());
            m.put("voucherUrl",  p.getVoucherUrl());
            m.put("estadoPago",  p.getEstado());
            m.put("notasPago",   p.getNotas());
            m.put("voucherReembolsoUrl", p.getVoucherReembolsoUrl());
        });
        return m;
    }

    private Map<String, Object> toMapDetalle(Reserva r) {
        Map<String, Object> m = toMap(r);
        m.put("clienteTelefono", r.getCliente().getTelefono());
        return m;
    }

    private Reserva validarAcceso(Long reservaId, UserDetails ud) {
        Reserva r = reservaRepo.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        Long propId = getPropietario(ud).getId();
        if (!r.getCancha().getPropietario().getId().equals(propId))
            throw new BadRequestException("No tienes acceso a esta reserva");
        return r;
    }

    private Usuario getPropietario(UserDetails ud) {
        return usuarioRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    /** "RES-000123" */
    private String codigoReserva(Reserva r) {
        return "RES-" + String.format("%06d", r.getId());
    }

    private String nombreMostrar(Usuario u) {
        String primerNombre   = u.getNombres()   == null ? "" : u.getNombres().trim().split("\\s+")[0];
        String primerApellido = u.getApellidos() == null ? "" : u.getApellidos().trim().split("\\s+")[0];
        return (primerNombre + " " + primerApellido).trim();
    }

    private String ubicacion(Cancha c) {
        StringBuilder sb = new StringBuilder();
        if (c.getDireccion() != null && !c.getDireccion().isBlank()) sb.append(c.getDireccion());
        if (c.getDistrito()  != null && !c.getDistrito().isBlank())
            sb.append(sb.length() > 0 ? ", " : "").append(c.getDistrito());
        return sb.length() > 0 ? sb.toString() : "No especificada";
    }

    private String fechaFormateada(Reserva r) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "PE"));
        String dia = r.getFecha().getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "PE"));
        dia = Character.toUpperCase(dia.charAt(0)) + dia.substring(1);
        return dia + ", " + r.getFecha().format(fmt);
    }

    private String horario(Reserva r) {
        return r.getHoraInicio() + " - " + r.getHoraFin();
    }
}