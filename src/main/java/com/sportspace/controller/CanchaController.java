package com.sportspace.controller;

import com.sportspace.dto.request.CanchaRequest;
import com.sportspace.dto.response.CanchaResponse;
import com.sportspace.service.CanchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/canchas")
@RequiredArgsConstructor
public class CanchaController {

    private final CanchaService canchaService;

    // ── PÚBLICO ───────────────────────────────────────────────────────────────

    @GetMapping("/publico")
    public ResponseEntity<List<CanchaResponse>> listarActivas() {
        return ResponseEntity.ok(canchaService.listarActivas());
    }

    @GetMapping("/publico/{id}")
    public ResponseEntity<CanchaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(canchaService.obtenerPorId(id));
    }

    @GetMapping("/publico/deporte/{deporte}")
    public ResponseEntity<List<CanchaResponse>> listarPorDeporte(
            @PathVariable String deporte) {
        return ResponseEntity.ok(canchaService.listarPorDeporte(deporte));
    }

    @GetMapping("/publico/buscar")
    public ResponseEntity<List<CanchaResponse>> buscar(
            @RequestParam(required = false) String distrito,
            @RequestParam(required = false) String deporte) {
        return ResponseEntity.ok(canchaService.buscarFiltrado(distrito, deporte));
    }

    // ── PROPIETARIO ───────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<CanchaResponse> crear(
            @Valid @RequestBody CanchaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(canchaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<CanchaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CanchaRequest request) {
        return ResponseEntity.ok(canchaService.actualizar(id, request));
    }

    @GetMapping("/mis-canchas")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<List<CanchaResponse>> misCanchas() {
        return ResponseEntity.ok(canchaService.listarMisCanchas());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('PROPIETARIO', 'ADMIN')")
    public ResponseEntity<CanchaResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String estado = body.get("estado");
        if (estado == null || estado.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(canchaService.cambiarEstado(id, estado.toUpperCase()));
    }

    @GetMapping("/admin/todas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CanchaResponse>> listarTodas() {
        return ResponseEntity.ok(canchaService.listarTodas());
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CanchaResponse> obtenerAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(canchaService.obtenerPorId(id));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        canchaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}