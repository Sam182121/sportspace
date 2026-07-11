package com.sportspace.controller;

import com.sportspace.dto.response.CanchaResponse;
import com.sportspace.service.CanchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/canchas")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCanchaController {

    private final CanchaService canchaService;

    /** GET /api/admin/canchas — listar todas (activas, inactivas, pendientes, destacadas) */
    @GetMapping
    public ResponseEntity<List<CanchaResponse>> listarTodas() {
        return ResponseEntity.ok(canchaService.listarTodas());
    }

    /** GET /api/admin/canchas/{id} — detalle de una cancha */
    @GetMapping("/{id}")
    public ResponseEntity<CanchaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(canchaService.obtenerPorId(id));
    }


    @PatchMapping("/{id}/estado")
    public ResponseEntity<CanchaResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String estado = body.get("estado");
        if (estado == null || estado.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(canchaService.cambiarEstado(id, estado.toUpperCase()));
    }

    /** DELETE /api/admin/canchas/{id} — eliminar cancha */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        canchaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}