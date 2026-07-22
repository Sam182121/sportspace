package com.sportspace.controller;

import com.sportspace.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Endpoints de reportes — solo ADMIN.
 * Todos devuelven un archivo Excel (.xlsx) para descargar.
 *
 *  GET /api/admin/reportes/usuarios
 *  GET /api/admin/reportes/reservas
 *  GET /api/admin/reportes/ingresos
 *  GET /api/admin/reportes/canchas
 *  GET /api/admin/reportes/propietarios
 *
 *  Params opcionales: fechaInicio=2026-01-01 & fechaFin=2026-06-30
 */
@RestController
@RequestMapping("/api/admin/reportes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/usuarios")
    public ResponseEntity<byte[]> reporteUsuarios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] bytes = reporteService.reporteUsuarios(fechaInicio, fechaFin);
            return excelResponse(bytes, "reporte_usuarios.xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reservas")
    public ResponseEntity<byte[]> reporteReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] bytes = reporteService.reporteReservas(fechaInicio, fechaFin);
            return excelResponse(bytes, "reporte_reservas.xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/ingresos")
    public ResponseEntity<byte[]> reporteIngresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] bytes = reporteService.reporteIngresos(fechaInicio, fechaFin);
            return excelResponse(bytes, "reporte_ingresos.xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/canchas")
    public ResponseEntity<byte[]> reporteCanchas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] bytes = reporteService.reporteCanchas(fechaInicio, fechaFin);
            return excelResponse(bytes, "reporte_canchas.xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/propietarios")
    public ResponseEntity<byte[]> reportePropietarios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            byte[] bytes = reporteService.reportePropietarios(fechaInicio, fechaFin);
            return excelResponse(bytes, "reporte_propietarios.xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helpers

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}