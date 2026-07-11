package com.sportspace.service;

import com.sportspace.entity.Notificacion;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.BadRequestException;
import com.sportspace.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Notificaciones para cualquier usuario (PROPIETARIO o CLIENTE). */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public void crear(Usuario destinatario, Notificacion.TipoNotificacion tipo,
                      String titulo, String mensaje, Long reservaId) {
        Notificacion n = Notificacion.builder()
                .usuario(destinatario)
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .reservaId(reservaId)
                .leida(false)
                .build();
        notificacionRepository.save(n);
    }

    public Map<String, Object> listar(Long usuarioId) {
        List<Notificacion> lista = notificacionRepository
                .findTop30ByUsuarioIdOrderByCreatedAtDesc(usuarioId);
        Long noLeidas = notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
        return Map.of("notificaciones", lista, "noLeidas", noLeidas);
    }

    public void marcarLeida(Long id, Long usuarioId) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Notificación no encontrada"));
        if (!n.getUsuario().getId().equals(usuarioId))
            throw new BadRequestException("Esta notificación no te pertenece");
        n.setLeida(true);
        notificacionRepository.save(n);
    }

    public void marcarTodasLeidas(Long usuarioId) {
        notificacionRepository.marcarTodasLeidas(usuarioId);
    }
}