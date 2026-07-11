package com.sportspace.controller;

import com.sportspace.dto.response.UsuarioResponse;
import com.sportspace.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // El propio usuario (CLIENTE o PROPIETARIO) consulta sus datos y roles
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(usuarioService.obtenerPorEmail(auth.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, activo));
    }

    // Verifica si un número de documento (DNI o C.E.) ya está registrado
    @GetMapping("/publico/existe-documento/{numeroDocumento}")
    public ResponseEntity<Map<String, Boolean>> existeDocumento(
            @PathVariable String numeroDocumento) {
        return ResponseEntity.ok(
                Map.of("existe", usuarioService.existeDocumento(numeroDocumento)));
    }

    @GetMapping("/publico/existe-email/{email}")
    public ResponseEntity<Map<String, Boolean>> existeEmail(
            @PathVariable String email) {
        return ResponseEntity.ok(
                Map.of("existe", usuarioService.existeEmail(email)));
    }

    @GetMapping("/publico/existe-telefono/{telefono}")
    public ResponseEntity<Map<String, Boolean>> existeTelefono(
            @PathVariable String telefono) {
        return ResponseEntity.ok(
                Map.of("existe", usuarioService.existeTelefono(telefono)));
    }
}