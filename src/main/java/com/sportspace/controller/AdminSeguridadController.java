package com.sportspace.controller;

import com.sportspace.service.SeguridadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints del panel de Seguridad (solo ADMIN).
 * Base: /api/admin/seguridad
 */
@RestController
@RequestMapping("/api/admin/seguridad")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSeguridadController {

    private final SeguridadService seguridadService;

    // ── STATS ───────────────────────────────────────────────────────

    /** GET /api/admin/seguridad/stats */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(seguridadService.getStats());
    }

    // ── INTENTOS FALLIDOS ───────────────────────────────────────────

    /** GET /api/admin/seguridad/intentos-fallidos */
    @GetMapping("/intentos-fallidos")
    public ResponseEntity<Map<String, Object>> listarIntentos() {
        List<Map<String, Object>> intentos = seguridadService.listarIntentos();
        return ResponseEntity.ok(Map.of("intentos", intentos));
    }

    /** DELETE /api/admin/seguridad/intentos/{id} */
    @DeleteMapping("/intentos/{id}")
    public ResponseEntity<Map<String, Object>> eliminarIntento(@PathVariable Long id) {
        seguridadService.eliminarIntento(id);
        return ResponseEntity.ok(Map.of("mensaje", "Registro eliminado correctamente"));
    }

    // ── BLOQUEO DE IPs ──────────────────────────────────────────────

    /** POST /api/admin/seguridad/bloquear-ip */
    @PostMapping("/bloquear-ip")
    public ResponseEntity<Map<String, Object>> bloquearIP(
            @RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        if (ip == null || ip.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "IP requerida"));
        }
        seguridadService.bloquearIP(ip.trim());
        return ResponseEntity.ok(Map.of("mensaje", "IP bloqueada correctamente"));
    }

    /** POST /api/admin/seguridad/desbloquear-ip */
    @PostMapping("/desbloquear-ip")
    public ResponseEntity<Map<String, Object>> desbloquearIP(
            @RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        if (ip == null || ip.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "IP requerida"));
        }
        seguridadService.desbloquearIP(ip.trim());
        return ResponseEntity.ok(Map.of("mensaje", "IP desbloqueada correctamente"));
    }

    // ── SESIONES ACTIVAS ────────────────────────────────────────────

    /** GET /api/admin/seguridad/sesiones-activas */
    @GetMapping("/sesiones-activas")
    public ResponseEntity<Map<String, Object>> listarSesiones(HttpServletRequest request) {
        String token = extraerToken(request);
        List<Map<String, Object>> sesiones = seguridadService.listarSesiones(token);
        return ResponseEntity.ok(Map.of("sesiones", sesiones));
    }

    /** POST /api/admin/seguridad/cerrar-sesion */
    @PostMapping("/cerrar-sesion")
    public ResponseEntity<Map<String, Object>> cerrarSesion(
            @RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "sessionId requerido"));
        }
        seguridadService.cerrarSesion(sessionId.trim());
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
    }

    /** POST /api/admin/seguridad/cerrar-todas-sesiones */
    @PostMapping("/cerrar-todas-sesiones")
    public ResponseEntity<Map<String, Object>> cerrarTodasLasSesiones(
            HttpServletRequest request) {
        String token = extraerToken(request);
        seguridadService.cerrarTodasLasSesiones(token);
        return ResponseEntity.ok(Map.of("mensaje", "Todas las sesiones cerradas"));
    }

    // ── UTILIDAD ────────────────────────────────────────────────────

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return "";
    }
}