package com.sportspace.controller;

import com.sportspace.dto.response.ReservaResponse;
import com.sportspace.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Panel de administración de reservas.
 *
 * El reembolso lo gestiona el PROPIETARIO, no el admin.
 * Por eso el endpoint /reembolso fue eliminado de aquí.
 */
@RestController
@RequestMapping("/api/admin/reservas")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReservaController {

    private final ReservaService reservaService;

    /** GET /api/admin/reservas — todas las reservas del sistema */
    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    /** GET /api/admin/reservas/{id} — detalle de una reserva */
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    /**
     * PATCH /api/admin/reservas/{id}/estado
     * Body: { "estado": "CONFIRMADA" | "CANCELADA" }
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReservaResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String estadoStr = body.get("estado");
        if (estadoStr == null || estadoStr.isBlank())
            return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(
                reservaService.cambiarEstadoAdmin(id, estadoStr.toUpperCase()));
    }
}