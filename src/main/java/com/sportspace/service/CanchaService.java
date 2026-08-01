package com.sportspace.service;

import com.sportspace.dto.request.CanchaRequest;
import com.sportspace.dto.response.CanchaResponse;
import com.sportspace.entity.Cancha;
import com.sportspace.entity.Rol;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.CanchaRepository;
import com.sportspace.repository.UsuarioRepository;
import com.sportspace.repository.ReservaRepository;
import com.sportspace.repository.HorarioSlotRepository;
import com.sportspace.repository.FechaBloqueadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CanchaService {

    private final CanchaRepository  canchaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final HorarioSlotRepository horarioSlotRepository;
    private final FechaBloqueadaRepository fechaBloqueadaRepository;

    // ── PÚBLICO ───────────────────────────────────────────────────────────────

    public List<CanchaResponse> listarActivas() {
        return canchaRepository.findByActivaTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CanchaResponse> listarPorDeporte(String deporte) {
        return canchaRepository.findByDeporteIgnoreCaseAndActivaTrue(deporte)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CanchaResponse obtenerPorId(Long id) {
        return toResponse(buscarPorId(id));
    }

    public List<CanchaResponse> buscarFiltrado(String distrito, String deporte) {
        return canchaRepository.buscarFiltrado(distrito, deporte)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── PROPIETARIO ───────────────────────────────────────────────────────────

    @Transactional
    public CanchaResponse crear(CanchaRequest request) {
        Usuario propietario = getUsuarioAutenticado();
        validarRol(propietario, Rol.PROPIETARIO);

        Cancha cancha = Cancha.builder()
                .propietario(propietario)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .deporte(request.getDeporte())
                .precioHora(request.getPrecioHora())
                .direccion(request.getDireccion())
                .departamento(request.getDepartamento())
                .provincia(request.getProvincia())
                .distrito(request.getDistrito())
                .capacidad(request.getCapacidad())
                .estado("ACTIVA")
                .build();

        return toResponse(canchaRepository.save(cancha));
    }

    @Transactional
    public CanchaResponse actualizar(Long id, CanchaRequest request) {
        Usuario propietario = getUsuarioAutenticado();
        Cancha cancha = buscarPorId(id);
        validarPropietario(cancha, propietario);

        cancha.setNombre(request.getNombre());
        cancha.setDescripcion(request.getDescripcion());
        cancha.setDeporte(request.getDeporte());
        cancha.setPrecioHora(request.getPrecioHora());
        cancha.setDireccion(request.getDireccion());
        cancha.setDepartamento(request.getDepartamento());
        cancha.setProvincia(request.getProvincia());
        cancha.setDistrito(request.getDistrito());
        cancha.setCapacidad(request.getCapacidad());

        return toResponse(canchaRepository.save(cancha));
    }

    public List<CanchaResponse> listarMisCanchas() {
        Usuario propietario = getUsuarioAutenticado();
        return canchaRepository.findByPropietarioId(propietario.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    public List<CanchaResponse> listarTodas() {
        return canchaRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CanchaResponse cambiarEstado(Long id, String nuevoEstado) {
        Usuario usuario = getUsuarioAutenticado();
        Cancha cancha   = buscarPorId(id);

        List<String> estadosValidos = List.of("PENDIENTE", "ACTIVA", "DESTACADA", "INACTIVA");
        if (!estadosValidos.contains(nuevoEstado)) {
            throw new BadRequestException(
                    "Estado inválido. Use: PENDIENTE, ACTIVA, DESTACADA o INACTIVA");
        }

        if (usuario.getRol() == Rol.PROPIETARIO) {
            validarPropietario(cancha, usuario);
            if (!List.of("ACTIVA", "INACTIVA").contains(nuevoEstado)) {
                throw new BadRequestException(
                        "El propietario solo puede activar o desactivar su cancha");
            }
        }

        cancha.setEstado(nuevoEstado);
        // Sincronizar campo activa con el estado
        cancha.setActiva("ACTIVA".equals(nuevoEstado) || "DESTACADA".equals(nuevoEstado));
        return toResponse(canchaRepository.save(cancha));
    }

    @Transactional
    public void eliminar(Long id) {
        Cancha cancha = buscarPorId(id);

        long totalReservas = reservaRepository.countByCanchaId(id);
        if (totalReservas > 0) {
            throw new BadRequestException(
                    "No se puede eliminar: esta cancha tiene " + totalReservas +
                            " reserva(s) en su historial (activas o pasadas). " +
                            "Para no perder ese historial, desactívala en vez de eliminarla.");
        }

        // Limpiar configuración dependiente que no tiene datos históricos que preservar
        horarioSlotRepository.deleteByCanchaId(id);
        fechaBloqueadaRepository.deleteByCanchaId(id);

        try {
            canchaRepository.delete(cancha);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BadRequestException(
                    "No se puede eliminar esta cancha porque tiene información relacionada. " +
                            "Intenta desactivarla en vez de eliminarla.");
        }
    }

    // ── HELPERS PRIVADOS ──────────────────────────────────────────────────────

    private Cancha buscarPorId(Long id) {
        return canchaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cancha no encontrada con id: " + id));
    }

    private Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado"));
    }

    private void validarRol(Usuario usuario, Rol rolRequerido) {
        if (usuario.getRol() != rolRequerido) {
            throw new BadRequestException(
                    "Acción no permitida para el rol: " + usuario.getRol());
        }
    }

    private void validarPropietario(Cancha cancha, Usuario usuario) {
        if (!cancha.getPropietario().getId().equals(usuario.getId())) {
            throw new BadRequestException(
                    "No tienes permiso para modificar esta cancha");
        }
    }

    // ── toResponse: ahora incluye fotos ───────────────────────────────────────
    public CanchaResponse toResponse(Cancha c) {
        return CanchaResponse.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .descripcion(c.getDescripcion())
                .deporte(c.getDeporte())
                .precioPorHora(c.getPrecioHora())
                .direccion(c.getDireccion())
                .departamento(c.getDepartamento())
                .provincia(c.getProvincia())
                .distrito(c.getDistrito())
                .capacidad(c.getCapacidad())
                .estado(c.getEstado())
                .activa(c.getActiva())
                .totalReservas(c.getTotalReservas())
                .createdAt(c.getCreatedAt())
                // ─── FOTOS: mapear la lista completa ───────────────────────
                .fotos(c.getFotos() != null ? c.getFotos() : java.util.List.of())
                // ──────────────────────────────────────────────────────────
                .propietarioId(c.getPropietario().getId())
                .propietarioNombre(c.getPropietario().getNombres()
                        + " " + c.getPropietario().getApellidos())
                .propietarioEmail(c.getPropietario().getEmail())
                .propietarioTelefono(c.getPropietario().getTelefono())
                .build();
    }
}