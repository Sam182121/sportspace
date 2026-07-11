package com.sportspace.controller;

import com.sportspace.dto.response.dashboard.admin.*;
import com.sportspace.dto.response.PagoResponse;
import com.sportspace.service.AdminDashboardService;
import com.sportspace.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final PagoService           pagoService;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> stats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/api/admin/dashboard/ingresos-semana")
    public ResponseEntity<DashboardIngresosSemanaResponse> ingresosSemana() {
        return ResponseEntity.ok(dashboardService.getIngresosSemana());
    }

    @GetMapping("/api/admin/dashboard/reservas-deporte")
    public ResponseEntity<DashboardReservasDeporteResponse> reservasPorDeporte() {
        return ResponseEntity.ok(dashboardService.getReservasPorDeporte());
    }

    @GetMapping("/api/admin/dashboard/actividad-reciente")
    public ResponseEntity<DashboardActividadResponse> actividadReciente() {
        return ResponseEntity.ok(dashboardService.getActividadReciente());
    }

    @GetMapping("/api/admin/dashboard/alertas")
    public ResponseEntity<DashboardAlertasResponse> alertas() {
        return ResponseEntity.ok(dashboardService.getAlertas());
    }

    @GetMapping("/api/admin/dashboard/ultimas-reservas")
    public ResponseEntity<DashboardUltimasReservasResponse> ultimasReservas() {
        return ResponseEntity.ok(dashboardService.getUltimasReservas());
    }

    @GetMapping("/api/admin/dashboard/ultimos-pagos")
    public ResponseEntity<DashboardUltimosPagosResponse> ultimosPagos() {
        return ResponseEntity.ok(dashboardService.getUltimosPagos());
    }

    // ── Pagos (el JS llama a /api/admin/pagos y /api/admin/pagos/ingresos-semana) ──

    /**
     * GET /api/admin/pagos
     * Lista completa de pagos que usa la página pagos.html del admin.
     * Solo lectura — el admin no puede aprobar ni rechazar.
     */
    @GetMapping("/api/admin/pagos")
    public ResponseEntity<DashboardUltimosPagosResponse> listarPagos() {
        return ResponseEntity.ok(dashboardService.getTodosPagos());
    }

    /**
     * GET /api/admin/pagos/{id}
     * Detalle de un pago (para el modal "ver comprobante").
     */
    @GetMapping("/api/admin/pagos/{id}")
    public ResponseEntity<PagoResponse> detallePago(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPorId(id));
    }

    /**
     * GET /api/admin/pagos/ingresos-semana
     * Gráfico de barras de ingresos diarios (últimos 7 días).
     */
    @GetMapping("/api/admin/pagos/ingresos-semana")
    public ResponseEntity<DashboardIngresosSemanaResponse> ingresosSemanaAdmin() {
        return ResponseEntity.ok(dashboardService.getIngresosSemana());
    }
}