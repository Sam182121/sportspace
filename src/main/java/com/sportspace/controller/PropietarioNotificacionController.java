package com.sportspace.controller;

import com.sportspace.entity.Usuario;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.ReservaRepository;
import com.sportspace.repository.UsuarioRepository;
import com.sportspace.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/propietario/notificaciones")
@PreAuthorize("hasRole('PROPIETARIO')")
@RequiredArgsConstructor
public class PropietarioNotificacionController {

    private final NotificacionService notificacionService;
    private final ReservaRepository   reservaRepo;
    private final UsuarioRepository   usuarioRepo;

    private Usuario propietarioActual(UserDetails ud) {
        return usuarioRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    // Lista de notificaciones + contador de no leidas (para la campanita)
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(@org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails ud) {
        Usuario p = propietarioActual(ud);
        return ResponseEntity.ok(notificacionService.listar(p.getId()));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id,
                                            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails ud) {
        Usuario p = propietarioActual(ud);
        notificacionService.marcarLeida(id, p.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/marcar-todas-leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails ud) {
        Usuario p = propietarioActual(ud);
        notificacionService.marcarTodasLeidas(p.getId());
        return ResponseEntity.ok().build();
    }

    // Badges al costado de "Reservas" en el sidebar: nuevas / canceladas por cliente / reembolso pendiente
    @GetMapping("/badges")
    public ResponseEntity<Map<String, Object>> badges(@org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails ud) {
        Usuario p = propietarioActual(ud);
        Long nuevas      = reservaRepo.countPendientesByPropietarioId(p.getId());
        Long canceladas  = reservaRepo.countCanceladasClienteByPropietarioId(p.getId());
        Long reembolsos  = reservaRepo.countReembolsoPendienteByPropietarioId(p.getId());
        return ResponseEntity.ok(Map.of(
                "nuevas", nuevas,
                "canceladas", canceladas,
                "reembolsoPendiente", reembolsos
        ));
    }
}