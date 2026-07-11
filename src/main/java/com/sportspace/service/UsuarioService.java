package com.sportspace.service;

import com.sportspace.dto.response.UsuarioResponse;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UsuarioResponse obtenerPorId(Long id) {
        return toResponse(buscarPorId(id));
    }

    /** Usado por /api/usuarios/me: cualquier usuario autenticado consulta sus propios datos y roles. */
    public UsuarioResponse obtenerPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
        return toResponse(usuario);
    }

    public UsuarioResponse cambiarEstado(Long id, Boolean activo) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(activo);
        return toResponse(usuarioRepository.save(usuario));
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + id));
    }

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
                .build();
    }

    public boolean existeDocumento(String numeroDocumento) {
        return usuarioRepository.existsByNumeroDocumento(numeroDocumento);
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public boolean existeTelefono(String telefono) {
        return usuarioRepository.existsByTelefono(telefono);
    }
}