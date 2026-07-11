package com.sportspace.controller;

import com.sportspace.service.EstadisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints de estadísticas — solo ADMIN.
 *
 *  GET /api/admin/estadisticas/resumen
 *  GET /api/admin/estadisticas/usuarios-mensual
 *  GET /api/admin/estadisticas/ingresos-mensual
 *  GET /api/admin/estadisticas/canchas-top
 *  GET /api/admin/estadisticas/deportes
 *  GET /api/admin/estadisticas/horarios
 *  GET /api/admin/estadisticas/dias-semana
 */
@RestController
@RequestMapping("/api/admin/estadisticas")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> resumen() {
        return ResponseEntity.ok(estadisticasService.getResumen());
    }

    @GetMapping("/usuarios-mensual")
    public ResponseEntity<Map<String, Object>> usuariosMensual() {
        return ResponseEntity.ok(estadisticasService.getUsuariosMensual());
    }

    @GetMapping("/ingresos-mensual")
    public ResponseEntity<Map<String, Object>> ingresosMensual() {
        return ResponseEntity.ok(estadisticasService.getIngresosMensual());
    }

    @GetMapping("/canchas-top")
    public ResponseEntity<Map<String, Object>> canchasTop() {
        return ResponseEntity.ok(estadisticasService.getCanchasTop());
    }

    @GetMapping("/deportes")
    public ResponseEntity<Map<String, Object>> deportes() {
        return ResponseEntity.ok(estadisticasService.getDeportes());
    }

    @GetMapping("/horarios")
    public ResponseEntity<Map<String, Object>> horarios() {
        return ResponseEntity.ok(estadisticasService.getHorarios());
    }

    @GetMapping("/dias-semana")
    public ResponseEntity<Map<String, Object>> diasSemana() {
        return ResponseEntity.ok(estadisticasService.getDiasSemana());
    }
}