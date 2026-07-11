package com.sportspace.controller;

import com.sportspace.dto.request.LoginRequest;
import com.sportspace.dto.request.RegistroRequest;
import com.sportspace.dto.response.AuthResponse;
import com.sportspace.dto.response.UsuarioResponse;
import com.sportspace.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // REGISTRAR NUEVO USUARIO
    @PostMapping(value = "/registro", consumes = "application/json")
    public ResponseEntity<UsuarioResponse> registro(
            @Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registro(request));
    }

    // INICIAR SESION
    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ELEGIR ROL (solo cuando el usuario tiene doble rol: cliente + propietario)
    @PostMapping(value = "/seleccionar-rol", consumes = "application/json")
    public ResponseEntity<AuthResponse> seleccionarRol(
            @jakarta.validation.Valid @RequestBody
            com.sportspace.dto.request.SeleccionRolRequest request) {
        return ResponseEntity.ok(authService.seleccionarRol(request));
    }

    @GetMapping("/verificar")
    public ResponseEntity<Map<String, Object>> verificar(Authentication auth) {
        return ResponseEntity.ok(Map.of(
                "valido", true,
                "email",  auth.getName()
        ));
    }
}