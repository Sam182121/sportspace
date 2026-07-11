package com.sportspace.controller;

import com.sportspace.dto.response.dashboard.*;
import com.sportspace.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard") // RUTA DASHBOARD
@RequiredArgsConstructor // AUTOMATICO DASHBOSERVICE INYECTA
public class DashboardController {

    // JALA DATA DE LAS BD PARA LA PANTALLA
    private final DashboardService dashboardService;

    // DASHBOARD ADMIN
    // SOLO ADMIN TIENE ACCESO CONTROL TOTAL DE TODO (CANCHAS USUARIOS PROPIETARIOS ETC)
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardAdminResponse> dashboardAdmin() {
        return ResponseEntity.ok(dashboardService.getDashboardAdmin());
    }

    // DASHBOARD PROPIETARIO
    // SEGURIDAD MEDIANTE TOKEN  EVITA CAMBIAR EN LA URL
    @GetMapping("/propietario")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<DashboardPropietarioResponse> dashboardPropietario() {
        return ResponseEntity.ok(dashboardService.getDashboardPropietario());
    }

    // DASHBOARD CLIENTE
    // TRAE INFO DEL USUARIO AUTENTICADO Y MUESTRA DATA EN PANTALLA
    // RESERVAS CANCHAS FAVORITAS ETC
    @GetMapping("/cliente")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<DashboardClienteResponse> dashboardCliente() {
        return ResponseEntity.ok(dashboardService.getDashboardCliente());
    }
}