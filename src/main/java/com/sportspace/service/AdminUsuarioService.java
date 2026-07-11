package com.sportspace.service;

import com.sportspace.dto.request.UsuarioEditRequest;
import com.sportspace.dto.response.UsuarioResponse;
import com.sportspace.dto.response.UsuarioStatsResponse;
import com.sportspace.entity.Rol;
import com.sportspace.entity.Usuario;
import com.sportspace.entity.EstadoReserva;
import com.sportspace.entity.Reserva;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.UsuarioRepository;
import com.sportspace.repository.ReservaRepository;
import com.sportspace.repository.CanchaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final CanchaRepository  canchaRepository;
    private final EliminacionUsuarioService eliminacionUsuarioService;
    private final BloqueoUsuarioService bloqueoUsuarioService;

    /*Listar todos  */

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        // Obtiene todos los usuarios y los convierte al DTO de respuesta
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* Estadísticas para las tarjetas  del panel  */

    @Transactional(readOnly = true)
    public UsuarioStatsResponse getStats() {
        long total        = usuarioRepository.count();
        long activos      = usuarioRepository.countByActivoTrue();
        long inactivos    = usuarioRepository.countByActivoFalse();
        long propietarios = usuarioRepository.countByRol(Rol.PROPIETARIO);
        long clientes     = usuarioRepository.countByRol(Rol.CLIENTE);
        long admins       = usuarioRepository.countByRol(Rol.ADMIN);

        return UsuarioStatsResponse.builder()
                .total(total)
                .activos(activos)
                .inactivos(inactivos)
                .propietarios(propietarios)
                .clientes(clientes)
                .admins(admins)
                .build();
    }

    /* Obtener un usuario por id */

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return toResponse(buscarPorId(id));
    }

    /* Editar usuario  */

    @Transactional
    public UsuarioResponse editar(Long id, UsuarioEditRequest req) {
        Usuario u = buscarPorId(id);

        //  Validar email
        // Solo verificar unicidad si el email cambio
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            if (!u.getEmail().equalsIgnoreCase(req.getEmail())) {
                if (usuarioRepository.existsByEmail(req.getEmail())) {
                    throw new BadRequestException("El correo ya está registrado por otro usuario");
                }
            }
            u.setEmail(req.getEmail().trim().toLowerCase());
        }

        //  Telefono
        u.setTelefono(req.getTelefono() != null && !req.getTelefono().isBlank()
                ? req.getTelefono().trim()
                : null);

        //  Rol
        if (req.getRol() != null && !req.getRol().isBlank()) {
            Rol nuevoRol;
            try {
                nuevoRol = Rol.valueOf(req.getRol().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Rol inválido: " + req.getRol());
            }

            if (nuevoRol == Rol.ADMIN) {
                throw new BadRequestException(
                        "No se puede asignar el rol ADMIN. Solo se permite CLIENTE o PROPIETARIO."
                );
            }

            u.setRol(nuevoRol);
        }

        return toResponse(usuarioRepository.save(u));
    }

    /*  Cambiar estado activo / bloqueado */
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, Boolean activo, String motivo, String comentario, Usuario admin) {
        if (activo == null) {
            throw new BadRequestException("El campo 'activo' es requerido");
        }
        Usuario u = bloqueoUsuarioService.cambiarEstado(id, activo, motivo, comentario, admin);
        return toResponse(u);
    }

    /** Activa/desactiva los roles Cliente y/o Propietario de la cuenta (usado para doble rol). */
    @Transactional
    public UsuarioResponse cambiarRoles(Long id, Boolean esCliente, Boolean esPropietario) {
        Usuario u = buscarPorId(id);

        if (u.getRol() == Rol.ADMIN)
            throw new BadRequestException("No se pueden gestionar roles de una cuenta ADMIN");

        boolean nuevoCliente     = Boolean.TRUE.equals(esCliente);
        boolean nuevoPropietario = Boolean.TRUE.equals(esPropietario);

        if (!nuevoCliente && !nuevoPropietario)
            throw new BadRequestException("El usuario debe tener al menos un rol activo");

        u.setEsCliente(nuevoCliente);
        u.setEsPropietario(nuevoPropietario);

        // Si el rol ACTIVO actual ya no está habilitado, cambiarlo al que sí lo esté
        if (u.getRol() == Rol.CLIENTE && !nuevoCliente) {
            u.setRol(Rol.PROPIETARIO);
        } else if (u.getRol() == Rol.PROPIETARIO && !nuevoPropietario) {
            u.setRol(Rol.CLIENTE);
        }

        return toResponse(usuarioRepository.save(u));
    }

    /* Eliminar usuario (delega en EliminacionUsuarioService: bloquea si hay
       reservas activas, anonimiza si hay historial, borra si está limpio) */

    public String eliminar(Long id, String motivo, String comentario, Usuario admin, boolean forzar) {
        return eliminacionUsuarioService.eliminarOAnonimizar(id, motivo, comentario, admin, forzar);
    }

    /** Busca un usuario por id o lanza excepción 404 si no existe */
    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + id));
    }

    /** Convierte la entidad Usuario al dto de respuesta */
    private UsuarioResponse toResponse(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .email(u.getEmail())
                .tipoDocumento(u.getTipoDocumento())
                .numeroDocumento(u.getNumeroDocumento())
                .nacionalidad(u.getNacionalidad())
                .telefono(u.getTelefono())
                .rol(u.getRol().name())
                .esCliente(u.tieneRolCliente())
                .esPropietario(u.tieneRolPropietario())
                .activo(u.getActivo())
                .estado(Boolean.TRUE.equals(u.getActivo()) ? "ACTIVO" : "INACTIVO")
                .createdAt(u.getCreatedAt())
                .fechaNacimiento(u.getFechaNacimiento())
                .departamento(u.getDepartamento())
                .provincia(u.getProvincia())
                .distrito(u.getDistrito())
                .direccion(u.getDireccion())
                .ubigeo(u.getUbigeo())
                .build();
    }
}