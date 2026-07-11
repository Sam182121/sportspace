package com.sportspace.controller;

import com.sportspace.entity.*;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/propietario/horarios")
@PreAuthorize("hasRole('PROPIETARIO')")
@RequiredArgsConstructor
public class PropietarioHorarioController {

    private final CanchaRepository         canchaRepo;
    private final HorarioSlotRepository    slotRepo;
    private final FechaBloqueadaRepository fechaRepo;
    private final UsuarioRepository        usuarioRepo;

    // ── GET /{canchaId} ──────────────────────────────────────────
    @GetMapping("/{canchaId}")
    public ResponseEntity<Map<String, Object>> getHorario(
            @PathVariable Long canchaId,
            @AuthenticationPrincipal UserDetails ud) {
        validarPropietario(canchaId, ud);
        List<HorarioSlot> slots = slotRepo.findByCanchaId(canchaId);
        List<Map<String, Object>> slotsMapped = slots.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        s.getId());
            m.put("diaSemana", s.getDiaSemana());   // Integer 0-6
            m.put("hora",      s.getHora());         // Integer 0-23
            m.put("estado",    s.getEstado());
            return m;
        }).toList();
        return ResponseEntity.ok(Map.of("slots", slotsMapped));
    }

    // ── POST /{canchaId} — guardar horarios ──────────────────────
    @PostMapping("/{canchaId}")
    public ResponseEntity<Map<String, Object>> guardarHorario(
            @PathVariable Long canchaId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails ud) {

        Cancha cancha = validarPropietario(canchaId, ud);

        // BLOQUEO: admin desactivó la cancha
        if ("INACTIVA".equals(cancha.getEstado())) {
            throw new BadRequestException(
                    "Tu cancha fue desactivada por el administrador. No puedes modificar sus horarios.");
        }
        if ("PENDIENTE".equals(cancha.getEstado())) {
            throw new BadRequestException(
                    "Esta cancha aún no ha sido aprobada por el administrador. No puedes configurar horarios hasta que sea aprobada.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slotsList =
                (List<Map<String, Object>>) body.get("slots");
        if (slotsList == null) throw new BadRequestException("slots requeridos");

        // Borrar los slots actuales de esta cancha
        slotRepo.deleteByCanchaId(canchaId);

        List<HorarioSlot> nuevos = slotsList.stream().map(s -> {
            HorarioSlot slot = new HorarioSlot();
            slot.setCancha(cancha);
            // diaSemana y hora son Integer en la entidad — parsear correctamente
            slot.setDiaSemana(toInt(s.get("diaSemana")));
            slot.setHora(toInt(s.get("hora")));
            Object estadoObj = s.get("estado");
            slot.setEstado(estadoObj != null ? estadoObj.toString() : "DISPONIBLE");
            return slot;
        }).toList();

        slotRepo.saveAll(nuevos);
        return ResponseEntity.ok(Map.of(
                "mensaje",   "Horarios guardados correctamente",
                "cantidad",  nuevos.size()));
    }

    // ── GET /{canchaId}/fechas-bloqueadas ────────────────────────
    @GetMapping("/{canchaId}/fechas-bloqueadas")
    public ResponseEntity<List<Map<String, Object>>> getFechasBloqueadas(
            @PathVariable Long canchaId,
            @AuthenticationPrincipal UserDetails ud) {
        validarPropietario(canchaId, ud);
        // Usar el método real del repositorio
        List<Map<String, Object>> result = fechaRepo
                .findByCanchaIdOrderByFechaAsc(canchaId)
                .stream()
                .map(fb -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",     fb.getId());
                    m.put("fecha",  fb.getFecha());
                    m.put("motivo", fb.getMotivo());
                    m.put("nota",   fb.getNota());
                    return m;
                }).toList();
        return ResponseEntity.ok(result);
    }

    // ── POST /{canchaId}/fechas-bloqueadas ───────────────────────
    @PostMapping("/{canchaId}/fechas-bloqueadas")
    public ResponseEntity<Map<String, Object>> bloquearFecha(
            @PathVariable Long canchaId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {

        Cancha cancha = validarPropietario(canchaId, ud);

        if ("INACTIVA".equals(cancha.getEstado())) {
            throw new BadRequestException(
                    "Tu cancha fue desactivada por el administrador.");
        }
        if ("PENDIENTE".equals(cancha.getEstado())) {
            throw new BadRequestException(
                    "Esta cancha aún no ha sido aprobada por el administrador.");
        }

        String fechaStr = body.get("fecha");
        if (fechaStr == null || fechaStr.isBlank())
            throw new BadRequestException("fecha requerida (YYYY-MM-DD)");

        LocalDate fecha = LocalDate.parse(fechaStr);
        if (fecha.isBefore(LocalDate.now()))
            throw new BadRequestException("No puedes bloquear fechas pasadas");

        FechaBloqueada fb = new FechaBloqueada();
        fb.setCancha(cancha);
        fb.setFecha(fecha);
        fb.setMotivo(body.getOrDefault("motivo", "Bloqueado por propietario"));
        fb.setNota(body.get("nota"));
        fechaRepo.save(fb);

        return ResponseEntity.ok(Map.of("mensaje", "Fecha bloqueada correctamente"));
    }

    // ── DELETE /{canchaId}/fechas-bloqueadas/{id} ─────────────────
    // Se elimina por ID del registro, no por fecha (más seguro y simple)
    @DeleteMapping("/{canchaId}/fechas-bloqueadas/{fechaId}")
    public ResponseEntity<Map<String, Object>> desbloquearFecha(
            @PathVariable Long canchaId,
            @PathVariable Long fechaId,
            @AuthenticationPrincipal UserDetails ud) {

        Cancha cancha = validarPropietario(canchaId, ud);
        if ("INACTIVA".equals(cancha.getEstado())) {
            throw new BadRequestException("Tu cancha fue desactivada por el administrador.");
        }

        FechaBloqueada fb = fechaRepo.findById(fechaId)
                .orElseThrow(() -> new ResourceNotFoundException("Fecha bloqueada no encontrada"));

        // Verificar que la fecha bloqueada pertenece a esta cancha
        if (!fb.getCancha().getId().equals(canchaId)) {
            throw new BadRequestException("Esta fecha no pertenece a tu cancha");
        }

        fechaRepo.delete(fb);
        return ResponseEntity.ok(Map.of("mensaje", "Fecha desbloqueada correctamente"));
    }

    // ── Helpers ──────────────────────────────────────────────────
    private Cancha validarPropietario(Long canchaId, UserDetails ud) {
        Cancha c = canchaRepo.findById(canchaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        Long propId = usuarioRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"))
                .getId();
        if (!c.getPropietario().getId().equals(propId))
            throw new BadRequestException("No tienes permisos sobre esta cancha");
        return c;
    }

    /** Convierte Integer, Double, String → Integer de forma segura */
    private Integer toInt(Object v) {
        if (v == null) throw new BadRequestException("diaSemana y hora son obligatorios");
        if (v instanceof Integer i)  return i;
        if (v instanceof Double  d)  return d.intValue();
        if (v instanceof Long    l)  return l.intValue();
        try { return Integer.parseInt(v.toString()); }
        catch (NumberFormatException e) {
            throw new BadRequestException("Valor numérico inválido: " + v);
        }
    }
}