package com.sportspace.service;

import com.sportspace.entity.*;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class EliminacionUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final CanchaRepository  canchaRepository;
    private final PagoRepository    pagoRepository;
    private final HorarioSlotRepository horarioSlotRepository;
    private final FechaBloqueadaRepository fechaBloqueadaRepository;
    private final MetodoPagoPropietarioRepository metodoPagoPropietarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final CodigoVerificacionRepository codigoVerificacionRepository;
    private final EliminacionUsuarioLogRepository logRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final Set<String> MOTIVOS_VALIDOS = Set.of(
            "SOLICITADO_POR_USUARIO", "MAL_USO_PLATAFORMA", "CUENTA_DUPLICADA_PRUEBA", "OTRO");

    @Transactional
    public String eliminarOAnonimizar(Long id, String motivo, String comentario, Usuario admin, boolean forzar) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        String motivoNormalizado = (motivo == null) ? "" : motivo.trim().toUpperCase();
        if (!MOTIVOS_VALIDOS.contains(motivoNormalizado))
            throw new BadRequestException("Motivo invalido");
        if (motivoNormalizado.equals("OTRO") && (comentario == null || comentario.isBlank()))
            throw new BadRequestException("Debes escribir un comentario cuando el motivo es 'Otro'");

        // Reservas activas: bloqueo total, SIEMPRE, incluso si se pide forzar
        List<Reserva> reservasComoCliente = reservaRepository.findByClienteIdOrderByFechaDescHoraInicioDesc(id);
        long activasComoCliente = reservasComoCliente.stream().filter(this::esActiva).count();
        if (activasComoCliente > 0) {
            throw new BadRequestException(
                    "No se puede eliminar: tiene " + activasComoCliente +
                            " reserva(s) activa(s) como cliente. Resuelvelas (aprobar/rechazar/cancelar) primero.");
        }

        long canchasCount = canchaRepository.countByPropietarioId(id);
        long activasComoPropietario = reservaRepository.findByPropietarioId(id).stream()
                .filter(this::esActiva).count();
        if (activasComoPropietario > 0) {
            throw new BadRequestException(
                    "No se puede eliminar: sus canchas tienen " + activasComoPropietario +
                            " reserva(s) activa(s). Resuelvelas primero.");
        }

        String emailSnapshot  = u.getEmail();
        String nombreSnapshot = (u.getNombres() + " " + u.getApellidos()).trim();
        String rolSnapshot    = u.getRol() != null ? u.getRol().name() : null;
        Long   idSnapshot     = u.getId();

        String tipoAccion;
        if (forzar) {
            eliminarTotalEnCascada(id);
            tipoAccion = "ELIMINADO_TOTAL_FORZADO";
        } else {
            boolean tieneHistorial = !reservasComoCliente.isEmpty() || canchasCount > 0;
            if (!tieneHistorial) {
                usuarioRepository.delete(u);
                tipoAccion = "ELIMINADO_TOTAL";
            } else {
                anonimizar(u, canchasCount);
                tipoAccion = "ANONIMIZADO";
            }
        }

        registrarLog(emailSnapshot, nombreSnapshot, rolSnapshot, idSnapshot,
                motivoNormalizado, comentario, tipoAccion, admin);

        try {
            boolean datosConservados = tipoAccion.equals("ANONIMIZADO");
            emailService.enviarCorreoEliminacionCuenta(
                    emailSnapshot,
                    nombreSnapshot.isBlank() ? "usuario" : nombreSnapshot,
                    motivoTexto(motivoNormalizado),
                    comentario,
                    datosConservados
            );
        } catch (Exception e) {
            // no debe romper la eliminación si el correo falla
        }

        return tipoAccion;
    }

    private String motivoTexto(String motivo) {
        return switch (motivo) {
            case "SOLICITADO_POR_USUARIO"    -> "Solicitado por el usuario";
            case "MAL_USO_PLATAFORMA"        -> "Mal uso de la plataforma";
            case "CUENTA_DUPLICADA_PRUEBA"   -> "Cuenta duplicada o de prueba";
            default                          -> "Otro";
        };
    }

    private boolean esActiva(Reserva r) {
        return r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.CONFIRMADA;
    }

    /** Borra en cascada TODO lo relacionado y finalmente al usuario. Irreversible. */
    private void eliminarTotalEnCascada(Long id) {
        // 1) Como CLIENTE: sus pagos y reservas
        pagoRepository.deleteByReservaClienteId(id);
        reservaRepository.deleteByClienteId(id);

        // 2) Como PROPIETARIO: pagos y reservas de sus canchas, horarios, fechas
        //    bloqueadas, metodos de pago y finalmente las canchas
        pagoRepository.deleteByReservaCanchaPropietarioId(id);
        reservaRepository.deleteByCanchaPropietarioId(id);

        List<Cancha> canchas = canchaRepository.findByPropietarioId(id);
        for (Cancha c : canchas) {
            horarioSlotRepository.deleteByCanchaId(c.getId());
            fechaBloqueadaRepository.deleteByCanchaId(c.getId());
        }
        metodoPagoPropietarioRepository.deleteByPropietarioId(id);
        canchaRepository.deleteAll(canchas);

        // 3) Datos propios del usuario que también son FK
        notificacionRepository.deleteByUsuarioId(id);
        codigoVerificacionRepository.deleteByUsuarioId(id);

        // 4) Por ultimo, el usuario
        usuarioRepository.deleteById(id);
    }

    private void anonimizar(Usuario u, long canchasCount) {
        String marcador = "eliminado_" + u.getId() + "_" + UUID.randomUUID().toString().substring(0, 8);

        u.setNombres("Usuario eliminado");
        u.setApellidos("");
        u.setEmail(marcador + "@sportspace.local");
        u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        u.setTelefono(null);
        u.setNumeroDocumento(null);
        u.setTipoDocumento(null);
        u.setDireccion(null);
        u.setActivo(false);
        usuarioRepository.save(u);

        if (canchasCount > 0) {
            List<Cancha> canchas = canchaRepository.findByPropietarioId(u.getId());
            for (Cancha c : canchas) {
                c.setActiva(false);
                c.setEstado("INACTIVA");
            }
            canchaRepository.saveAll(canchas);
        }
    }

    private void registrarLog(String emailSnapshot, String nombreSnapshot, String rol, Long usuarioId,
                              String motivo, String comentario, String tipoAccion, Usuario admin) {
        EliminacionUsuarioLog log = EliminacionUsuarioLog.builder()
                .usuarioId(usuarioId)
                .emailSnapshot(emailSnapshot)
                .nombreSnapshot(nombreSnapshot)
                .rol(rol)
                .motivo(motivo)
                .comentario(comentario)
                .tipoAccion(tipoAccion)
                .adminId(admin != null ? admin.getId() : null)
                .adminNombre(admin != null ? (admin.getNombres() + " " + admin.getApellidos()).trim() : null)
                .createdAt(LocalDateTime.now())
                .build();
        logRepository.save(log);
    }
}