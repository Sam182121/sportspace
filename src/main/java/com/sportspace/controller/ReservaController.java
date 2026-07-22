package com.sportspace.controller;

import com.sportspace.dto.request.CambioEstadoRequest;
import com.sportspace.dto.request.ReservaRequest;
import com.sportspace.dto.response.DisponibilidadResponse;
import com.sportspace.dto.response.ReservaResponse;
import com.sportspace.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    //  CLIENTE ALQUILAN CANCHA
    // POST EL CLIENTE ENVIA DATOS DE LA RESERVA, CANCHA HORARIO FECHA

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ReservaResponse> crear(
            @Valid @RequestBody ReservaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.crear(request));
    }

    //  VER HISTORIAL DE RESERVAS DEL CLIENTE
    //  NO PIDE ID PQ EL SERVICIO EXTRAE QUIEN ES EL CLIENTE
    @GetMapping("/mis-reservas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<ReservaResponse>> misReservas() {
        return ResponseEntity.ok(reservaService.misReservas());
    }

    // CANCELAR RESERVA
    // SOLO CAMBIA A CANCELADO NO BORRA DE LA BD
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ReservaResponse> cancelarMiReserva(
            @PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelarMiReserva(id));
    }

    //  PROPIETARIO

    // VER RESERVAS DE UNA CANCHA
    @GetMapping("/cancha/{canchaId}")
    @PreAuthorize("hasAnyRole('PROPIETARIO', 'ADMIN')")
    public ResponseEntity<List<ReservaResponse>> reservasPorCancha(
            @PathVariable Long canchaId) {
        return ResponseEntity.ok(reservaService.reservasPorCancha(canchaId));
    }

    // VER TODAS LAS RESERVAS DE TODAS LA CANCHAS
    @GetMapping("/mis-canchas-reservas")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<List<ReservaResponse>> misReservasPropietario() {
        return ResponseEntity.ok(reservaService.misReservasPropietario());
    }

    // CAMBIAR ESTADO DE UNA RESERVA
    // PASA DE PENDIENTE A CONFIRMADA CUANDO EL CLIENTE PAGA
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('PROPIETARIO', 'ADMIN')")
    public ResponseEntity<ReservaResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest request) {
        return ResponseEntity.ok(reservaService.cambiarEstado(id, request));
    }

    // CALCULA INGRESOS TOTALES
    // MUESTRA CUANTO DINERO GANO EL PROPIETARIO
    @GetMapping("/mis-ingresos")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<Map<String, BigDecimal>> misIngresos() {
        return ResponseEntity.ok(
                Map.of("totalIngresos", reservaService.misIngresos()));
    }

    // DISPONIBILIDAD

    // CONSULTA SI UNA CANCHA ESTA LIBRE
    // SE USA LOCALDATE PARA TIEMPO REAL
    @GetMapping("/disponibilidad/{canchaId}")
    public ResponseEntity<DisponibilidadResponse> disponibilidad(
            @PathVariable Long canchaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {
        return ResponseEntity.ok(
                reservaService.consultarDisponibilidad(canchaId, fecha));
    }

    // ADMIN
    // LISTA TODO EL HISTORIAL DEL SISTEMA
    @GetMapping("/admin/todas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponse>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    // DETALLE DE UNA RESERVA PARA EL ADMIN
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponse> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }
}