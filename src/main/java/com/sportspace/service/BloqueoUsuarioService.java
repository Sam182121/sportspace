package com.sportspace.service;

import com.sportspace.entity.EliminacionUsuarioLog;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.EliminacionUsuarioLogRepository;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Bloquea o desbloquea una cuenta (activo = false/true). A diferencia de
 * eliminar, la cuenta y todos sus datos quedan intactos: solo no puede
 * iniciar sesión mientras esté bloqueada.
 */
@Service
@RequiredArgsConstructor
public class BloqueoUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EliminacionUsuarioLogRepository logRepository;
    private final EmailService emailService;

    private static final Set<String> MOTIVOS_BLOQUEO = Set.of(
            "SOLICITADO_POR_USUARIO",
            "ACTIVIDAD_SOSPECHOSA_FRAUDE",
            "INCUMPLIMIENTO_TERMINOS",
            "QUEJAS_REITERADAS",
            "DATOS_NO_VERIFICADOS",
            "OTRO"
    );

    @Transactional
    public Usuario cambiarEstado(Long id, boolean activo, String motivo, String comentario, Usuario admin) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        String motivoNormalizado = null;

        if (!activo) {
            // Bloquear SÍ requiere motivo válido
            motivoNormalizado = (motivo == null) ? "" : motivo.trim().toUpperCase();
            if (!MOTIVOS_BLOQUEO.contains(motivoNormalizado))
                throw new BadRequestException("Motivo inválido");
            if (motivoNormalizado.equals("OTRO") && (comentario == null || comentario.isBlank()))
                throw new BadRequestException("Debes escribir un comentario cuando el motivo es 'Otro'");
        }

        u.setActivo(activo);
        if (activo) {
            u.setBloqueadoPorSeguridad(false);
            u.setFechaBloqueSeguridad(null);
        }
        usuarioRepository.save(u);

        registrarLog(u, motivoNormalizado, comentario, activo ? "DESBLOQUEADO" : "BLOQUEADO", admin);

        try {
            if (!activo) {
                emailService.enviarCorreoBloqueoCuenta(
                        u.getEmail(),
                        nombreMostrar(u),
                        motivoTexto(motivoNormalizado),
                        comentario
                );
            } else {
                emailService.enviarCorreoDesbloqueoCuenta(u.getEmail(), nombreMostrar(u));
            }
        } catch (Exception e) {
            // el correo nunca debe romper el bloqueo/desbloqueo
        }

        return u;
    }

    private String nombreMostrar(Usuario u) {
        String primerNombre   = u.getNombres()   == null ? "" : u.getNombres().trim().split("\\s+")[0];
        String primerApellido = u.getApellidos() == null ? "" : u.getApellidos().trim().split("\\s+")[0];
        return (primerNombre + " " + primerApellido).trim();
    }

    private String motivoTexto(String motivo) {
        return switch (motivo) {
            case "SOLICITADO_POR_USUARIO"        -> "Solicitado por el usuario";
            case "ACTIVIDAD_SOSPECHOSA_FRAUDE"   -> "Actividad sospechosa o posible fraude";
            case "INCUMPLIMIENTO_TERMINOS"       -> "Incumplimiento de los términos y condiciones";
            case "QUEJAS_REITERADAS"             -> "Quejas o reportes reiterados de otros usuarios";
            case "DATOS_NO_VERIFICADOS"          -> "No se pudo verificar la identidad / datos de la cuenta";
            default                              -> "Otro";
        };
    }

    private void registrarLog(Usuario u, String motivo, String comentario, String tipoAccion, Usuario admin) {
        EliminacionUsuarioLog log = EliminacionUsuarioLog.builder()
                .usuarioId(u.getId())
                .emailSnapshot(u.getEmail())
                .nombreSnapshot((u.getNombres() + " " + u.getApellidos()).trim())
                .rol(u.getRol() != null ? u.getRol().name() : null)
                .motivo(motivo == null ? "N/A" : motivo)
                .comentario(comentario)
                .tipoAccion(tipoAccion)
                .adminId(admin != null ? admin.getId() : null)
                .adminNombre(admin != null ? (admin.getNombres() + " " + admin.getApellidos()).trim() : null)
                .createdAt(LocalDateTime.now())
                .build();
        logRepository.save(log);
    }
}