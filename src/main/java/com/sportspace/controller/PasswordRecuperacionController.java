package com.sportspace.controller;

import com.sportspace.dto.request.RecuperarPasswordRequest;
import com.sportspace.dto.request.ResetPasswordRequest;
import com.sportspace.service.PasswordRecuperacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordRecuperacionController {

    private final PasswordRecuperacionService recuperacionService;


    @PostMapping("/recuperar-password")
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(
            @Valid @RequestBody RecuperarPasswordRequest request) {

        recuperacionService.solicitarRecuperacion(request);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Si el correo existe en nuestro sistema, recibirás un enlace para restablecer tu contraseña."
        ));
    }


    @GetMapping("/validar-token")
    public ResponseEntity<Map<String, String>> validarToken(
            @RequestParam String token) {

        recuperacionService.validarToken(token);
        return ResponseEntity.ok(Map.of("mensaje", "Token válido."));
    }


    @GetMapping("/validar-token-bloqueo")
    public ResponseEntity<Map<String, String>> validarTokenBloqueo(
            @RequestParam String token) {

        recuperacionService.validarTokenBloqueo(token);
        return ResponseEntity.ok(Map.of("mensaje", "Token de bloqueo válido."));
    }


    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetearPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        Map<String, String> resultado = recuperacionService.resetearPassword(request);
        return ResponseEntity.ok(resultado);
    }


    @PostMapping("/bloquear-cuenta")
    public ResponseEntity<Map<String, String>> bloquearCuenta(
            @RequestBody Map<String, String> body) {

        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "Token de bloqueo requerido."));
        }

        Map<String, String> resultado = recuperacionService.bloquearCuenta(token);
        return ResponseEntity.ok(resultado);
    }
}