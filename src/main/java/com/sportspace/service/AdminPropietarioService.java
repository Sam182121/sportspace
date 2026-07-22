package com.sportspace.service;

import com.sportspace.dto.request.PropietarioEstadoRequest;
import com.sportspace.dto.response.PropietarioEstadisticasResponse;
import com.sportspace.dto.response.PropietarioResponse;
import com.sportspace.entity.EstadoReserva;
import com.sportspace.entity.EstadoPropietario;
import com.sportspace.entity.Rol;
import com.sportspace.entity.Reserva;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.CanchaRepository;
import com.sportspace.repository.ReservaRepository;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPropietarioService {

    private final UsuarioRepository usuarioRepository;
    private final CanchaRepository  canchaRepository;
    private final ReservaRepository  reservaRepository;
    private final EliminacionUsuarioService eliminacionUsuarioService;

    /* Listar todos los propietarios */
    @Transactional(readOnly = true)
    public List<PropietarioResponse> listarTodos() {
        return usuarioRepository.findByRol(Rol.PROPIETARIO)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* ── Obtener uno por ID */
    @Transactional(readOnly = true)
    public PropietarioResponse obtenerPorId(Long id) {
        return toResponse(buscarPropietario(id));
    }

    /* Estadísticas detalladas de un propietario
       Llamado desde el modal "Ver estadísticas" del frontend. */
    @Transactional(readOnly = true)
    public PropietarioEstadisticasResponse getEstadisticas(Long id) {
        buscarPropietario(id);

        long totalCanchas     = canchaRepository.countByPropietarioId(id);
        long canchasActivas   = canchaRepository.countByPropietarioIdAndActivaTrue(id);
        long canchasInactivas = canchaRepository.countByPropietarioIdAndActivaFalse(id);

        List<Reserva> reservas = reservaRepository.findByPropietarioId(id);
        long totalReservas      = reservas.size();
        long confirmadas        = reservas.stream().filter(r ->
                r.getEstado() == EstadoReserva.CONFIRMADA || r.getEstado() == EstadoReserva.COMPLETADA).count();
        long canceladas         = reservas.stream().filter(r -> r.getEstado() == EstadoReserva.CANCELADA).count();
        long pendientes         = reservas.stream().filter(r -> r.getEstado() == EstadoReserva.PENDIENTE).count();

        BigDecimal ingresosTotal = reservaRepository.sumIngresosConfirmados(id);
        BigDecimal ingresosMes   = reservaRepository.sumIngresosEsteMes(id);

        return PropietarioEstadisticasResponse.builder()
                .totalCanchas(totalCanchas)
                .canchasActivas(canchasActivas)
                .canchasInactivas(canchasInactivas)
                .totalReservas(totalReservas)
                .reservasConfirmadas(confirmadas)
                .reservasCanceladas(canceladas)
                .reservasPendientes(pendientes)
                .ingresosMes(ingresosMes)
                .ingresosTotal(ingresosTotal)
                .build();
    }

    /* Cambiar estado
       El frontend envía { estado: "ACTIVO" | "PENDIENTE" | "INACTIVO" }.
       Guardamos en estadoPropietario y sincronizamos activo:
         ACTIVO   → activo = true   (puede iniciar sesión y operar)
         PENDIENTE → activo = false  (no puede iniciar sesión aún)
         INACTIVO  → activo = false  (bloqueado por el admin) */
    @Transactional
    public PropietarioResponse cambiarEstado(Long id, PropietarioEstadoRequest req) {
        if (req.getEstado() == null || req.getEstado().isBlank())
            throw new BadRequestException("El campo 'estado' es requerido");

        EstadoPropietario nuevoEstado;
        try {
            nuevoEstado = EstadoPropietario.valueOf(req.getEstado().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Estado inválido: '" + req.getEstado() + "'. Valores permitidos: ACTIVO, PENDIENTE, INACTIVO");
        }

        Usuario p = buscarPropietario(id);
        p.setEstadoPropietario(nuevoEstado);

        // Sincronizar activo para que el login funcione correctamente
        p.setActivo(nuevoEstado == EstadoPropietario.ACTIVO);

        return toResponse(usuarioRepository.save(p));
    }

    /* Eliminar propietario
       Bloquea la eliminación si tiene reservas PENDIENTE o CONFIRMADA. */
    public String eliminar(Long id, String motivo, String comentario, Usuario admin, boolean forzar) {
        return eliminacionUsuarioService.eliminarOAnonimizar(id, motivo, comentario, admin, forzar);
    }

    /* Helpers privado */

    private Usuario buscarPropietario(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + id));
        if (u.getRol() != Rol.PROPIETARIO)
            throw new BadRequestException("El usuario con id " + id + " no es PROPIETARIO");
        return u;
    }

    private PropietarioResponse toResponse(Usuario u) {
        Long id = u.getId();

        long totalCanchas    = canchaRepository.countByPropietarioId(id);
        long reservasActivas = reservaRepository.findByPropietarioId(id).stream()
                .filter(r -> r.getEstado() == EstadoReserva.PENDIENTE
                        || r.getEstado() == EstadoReserva.CONFIRMADA)
                .count();
        BigDecimal ingresos  = reservaRepository.sumIngresosConfirmados(id);

        // Si el registro es antiguo y no tiene estadoPropietario, inferimos uno
        EstadoPropietario ep = u.getEstadoPropietario();
        String estadoStr = ep != null
                ? ep.name()
                : (Boolean.TRUE.equals(u.getActivo()) ? "ACTIVO" : "INACTIVO");

        return PropietarioResponse.builder()
                .id(id)
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .email(u.getEmail())
                .tipoDocumento(u.getTipoDocumento())
                .numeroDocumento(u.getNumeroDocumento())
                .telefono(u.getTelefono())
                .departamento(u.getDepartamento())
                .provincia(u.getProvincia())
                .distrito(u.getDistrito())
                .estado(estadoStr)
                .totalCanchas(totalCanchas)
                .reservasActivas(reservasActivas)
                .ingresosGenerados(ingresos)
                .createdAt(u.getCreatedAt())
                .build();
    }
}