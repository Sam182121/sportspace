package com.sportspace.controller;

import com.sportspace.dto.request.ConfirmarPagoRequest;
import com.sportspace.dto.request.PagoRequest;
import com.sportspace.dto.response.PagoResponse;
import com.sportspace.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    // CLIENTE  →  /api/pagos

    @PostMapping("/api/pagos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagoResponse> registrarPago(
            @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoService.registrarPago(request));
    }

    @GetMapping("/api/pagos/mis-pagos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<PagoResponse>> misPagos() {
        return ResponseEntity.ok(pagoService.misPagos());
    }

    @GetMapping("/api/pagos/reserva/{reservaId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagoResponse> pagoDeReserva(
            @PathVariable Long reservaId) {
        return ResponseEntity.ok(pagoService.miPago(reservaId));
    }

    // PROPIETARIO  →  /api/pagos/propietario

    @GetMapping("/api/pagos/propietario")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<List<PagoResponse>> pagosDeMisCanchas() {
        return ResponseEntity.ok(pagoService.pagosDeMisCanchas());
    }

    @GetMapping("/api/pagos/propietario/ingresos")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<Map<String, BigDecimal>> misIngresos() {
        return ResponseEntity.ok(pagoService.misIngresos());
    }

    @PatchMapping("/api/pagos/{id}/confirmar")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<PagoResponse> confirmarPago(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmarPagoRequest body) {
        String notas = (body != null) ? body.getNotas() : null;
        return ResponseEntity.ok(pagoService.confirmarPago(id, notas));
    }

    @PatchMapping("/api/pagos/{id}/rechazar")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<PagoResponse> rechazarPago(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmarPagoRequest body) {
        String notas = (body != null) ? body.getNotas() : null;
        return ResponseEntity.ok(pagoService.rechazarPago(id, notas));
    }

    /**
     * El propietario procesa el reembolso de una reserva cancelada por el cliente.
     * Requiere adjuntar el comprobante (voucherUrl) de la devolución + un mensaje corto.
     */
    @PatchMapping("/api/pagos/{id}/reembolso")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<PagoResponse> procesarReembolso(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmarPagoRequest body) {
        String notas      = (body != null) ? body.getNotas()      : null;
        String voucherUrl = (body != null) ? body.getVoucherUrl() : null;
        return ResponseEntity.ok(pagoService.procesarReembolso(id, notas, voucherUrl));
    }

}