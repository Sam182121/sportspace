package com.sportspace.controller;

import com.sportspace.entity.Usuario;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.UsuarioRepository;
import com.sportspace.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cliente/notificaciones")
@PreAuthorize("hasRole('CLIENTE')")
@RequiredArgsConstructor
public class ClienteNotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioRepository   usuarioRepo;

    private Usuario clienteActual(UserDetails ud) {
        return usuarioRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    // Lista de notificaciones + contador de no leídas (para la campanita)
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(@AuthenticationPrincipal UserDetails ud) {
        Usuario c = clienteActual(ud);
        return ResponseEntity.ok(notificacionService.listar(c.getId()));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        Usuario c = clienteActual(ud);
        notificacionService.marcarLeida(id, c.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/marcar-todas-leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticationPrincipal UserDetails ud) {
        Usuario c = clienteActual(ud);
        notificacionService.marcarTodasLeidas(c.getId());
        return ResponseEntity.ok().build();
    }
}