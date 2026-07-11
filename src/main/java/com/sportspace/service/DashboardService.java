package com.sportspace.service;

import com.sportspace.dto.response.dashboard.*;
import com.sportspace.dto.response.dashboard.DashboardAdminResponse.*;
import com.sportspace.dto.response.dashboard.DashboardPropietarioResponse.*;
import com.sportspace.dto.response.dashboard.DashboardClienteResponse.*;
import com.sportspace.entity.*;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final CanchaRepository  canchaRepository;
    private final ReservaRepository  reservaRepository;

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA  = DateTimeFormatter.ofPattern("HH:mm");

    // ADMIN

    public DashboardAdminResponse getDashboardAdmin() {

        // Usuarios
        Long totalUsuarios     = usuarioRepository.count();
        Long totalClientes     = usuarioRepository.countByRol(Rol.CLIENTE);
        Long totalPropietarios = usuarioRepository.countByRol(Rol.PROPIETARIO);
        Long usuariosActivos   = usuarioRepository.countByActivoTrue();
        Long usuariosInactivos = usuarioRepository.countByActivoFalse();

        // Canchas
        Long totalCanchas    = canchaRepository.count();
        Long canchasActivas  = canchaRepository.countByActivaTrue();
        Long canchasInactivas = canchaRepository.countByActivaFalse();

        // Reservas
        Long totalReservas        = reservaRepository.count();
        Long reservasPendientes   = reservaRepository.countByEstado(EstadoReserva.PENDIENTE);
        Long reservasConfirmadas  = reservaRepository.countByEstado(EstadoReserva.CONFIRMADA);
        Long reservasCanceladas   = reservaRepository.countByEstado(EstadoReserva.CANCELADA);

        BigDecimal ingresosTotales = reservaRepository.sumIngresosTotalesPlataforma();

        // Top propietarios
        List<ResumenPropietario> topPropietarios =
                usuarioRepository.findByRol(Rol.PROPIETARIO)
                        .stream()
                        .map(p -> ResumenPropietario.builder()
                                .id(p.getId())
                                .nombreCompleto(p.getNombres() + " " + p.getApellidos())
                                .email(p.getEmail())
                                .totalCanchas(canchaRepository.countByPropietarioId(p.getId()))
                                .totalReservas(contarReservasPropietario(p.getId()))
                                .ingresos(reservaRepository.sumIngresosConfirmados(p.getId()))
                                .build())
                        .sorted((a, b) -> b.getIngresos().compareTo(a.getIngresos()))
                        .limit(5)
                        .toList();

        // Canchas mas reservadas
        List<ResumenCancha> canchasMasReservadas =
                canchaRepository.findAll()
                        .stream()
                        .map(c -> ResumenCancha.builder()
                                .id(c.getId())
                                .nombre(c.getNombre())
                                .deporte(c.getDeporte())
                                .distrito(c.getDistrito())
                                .totalReservas(reservaRepository.countByCanchaId(c.getId()))
                                .ingresos(reservaRepository.sumIngresosPorCancha(c.getId()))
                                .build())
                        .sorted((a, b) -> b.getTotalReservas().compareTo(a.getTotalReservas()))
                        .limit(5)
                        .toList();

        return DashboardAdminResponse.builder()
                .totalUsuarios(totalUsuarios)
                .totalClientes(totalClientes)
                .totalPropietarios(totalPropietarios)
                .usuariosActivos(usuariosActivos)
                .usuariosInactivos(usuariosInactivos)
                .totalCanchas(totalCanchas)
                .canchasActivas(canchasActivas)
                .canchasInactivas(canchasInactivas)
                .totalReservas(totalReservas)
                .reservasPendientes(reservasPendientes)
                .reservasConfirmadas(reservasConfirmadas)
                .reservasCanceladas(reservasCanceladas)
                .ingresosTotalesPlataforma(ingresosTotales)
                .topPropietarios(topPropietarios)
                .canchasMasReservadas(canchasMasReservadas)
                .build();
    }

    // PROPIETARIO

    public DashboardPropietarioResponse getDashboardPropietario() {

        Usuario propietario = getUsuarioAutenticado();

        List<Cancha> canchas = canchaRepository.findByPropietarioId(propietario.getId());

        Long totalCanchas     = canchaRepository.countByPropietarioId(propietario.getId());
        Long canchasActivas   = canchaRepository.countByPropietarioIdAndActivaTrue(propietario.getId());
        Long canchasInactivas = canchaRepository.countByPropietarioIdAndActivaFalse(propietario.getId());

        List<Reserva> todasReservas = reservaRepository.findByPropietarioId(propietario.getId());

        Long totalReservas       = (long) todasReservas.size();
        Long reservasPendientes  = todasReservas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.PENDIENTE).count();
        Long reservasConfirmadas = todasReservas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA).count();
        Long reservasCanceladas  = todasReservas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.CANCELADA).count();

        BigDecimal ingresosTotales = reservaRepository
                .sumIngresosConfirmados(propietario.getId());
        BigDecimal ingresosEsteMes = reservaRepository
                .sumIngresosEsteMes(propietario.getId());

        // Detalle por cancha
        List<ResumenCanchaDetalle> detalleCanchas = canchas.stream()
                .map(c -> ResumenCanchaDetalle.builder()
                        .id(c.getId())
                        .nombre(c.getNombre())
                        .deporte(c.getDeporte())
                        .distrito(c.getDistrito())
                        .precioHora(c.getPrecioHora())
                        .activa(c.getActiva())
                        .totalReservas(reservaRepository.countByCanchaId(c.getId()))
                        .reservasPendientes(reservaRepository
                                .countByCanchaIdAndEstado(c.getId(), EstadoReserva.PENDIENTE))
                        .reservasConfirmadas(reservaRepository
                                .countByCanchaIdAndEstado(c.getId(), EstadoReserva.CONFIRMADA))
                        .ingresos(reservaRepository.sumIngresosPorCancha(c.getId()))
                        .build())
                .toList();

        // Ultimas 10 reservas
        List<UltimaReserva> ultimasReservas = todasReservas.stream()
                .limit(10)
                .map(r -> UltimaReserva.builder()
                        .id(r.getId())
                        .clienteNombre(r.getCliente().getNombres()
                                + " " + r.getCliente().getApellidos())
                        .canchaNombre(r.getCancha().getNombre())
                        .fecha(r.getFecha().format(FMT_FECHA))
                        .horaInicio(r.getHoraInicio().format(FMT_HORA))
                        .horaFin(r.getHoraFin().format(FMT_HORA))
                        .estado(r.getEstado().name())
                        .total(r.getTotal())
                        .build())
                .toList();

        return DashboardPropietarioResponse.builder()
                .totalCanchas(totalCanchas)
                .canchasActivas(canchasActivas)
                .canchasInactivas(canchasInactivas)
                .totalReservas(totalReservas)
                .reservasPendientes(reservasPendientes)
                .reservasConfirmadas(reservasConfirmadas)
                .reservasCanceladas(reservasCanceladas)
                .ingresosTotales(ingresosTotales)
                .ingresosEsteMes(ingresosEsteMes)
                .canchas(detalleCanchas)
                .ultimasReservas(ultimasReservas)
                .build();
    }

    // CLIENTE

    public DashboardClienteResponse getDashboardCliente() {

        Usuario cliente = getUsuarioAutenticado();

        List<Reserva> proximas  = reservaRepository
                .findProximasReservasByClienteId(cliente.getId());
        List<Reserva> historial = reservaRepository
                .findHistorialByClienteId(cliente.getId());

        List<Reserva> todas = reservaRepository
                .findByClienteIdOrderByFechaDescHoraInicioDesc(cliente.getId());

        Long totalReservas       = (long) todas.size();
        Long reservasPendientes  = todas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.PENDIENTE).count();
        Long reservasConfirmadas = todas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA).count();
        Long reservasCanceladas  = todas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.CANCELADA).count();

        BigDecimal totalGastado = reservaRepository
                .sumTotalGastadoByClienteId(cliente.getId());

        List<ProximaReserva> proximasDTO = proximas.stream()
                .map(r -> ProximaReserva.builder()
                        .id(r.getId())
                        .canchaNombre(r.getCancha().getNombre())
                        .canchaDeporte(r.getCancha().getDeporte())
                        .canchaDistrito(r.getCancha().getDistrito())
                        .fecha(r.getFecha().format(FMT_FECHA))
                        .horaInicio(r.getHoraInicio().format(FMT_HORA))
                        .horaFin(r.getHoraFin().format(FMT_HORA))
                        .estado(r.getEstado().name())
                        .total(r.getTotal())
                        .build())
                .toList();

        List<HistorialReserva> historialDTO = historial.stream()
                .map(r -> HistorialReserva.builder()
                        .id(r.getId())
                        .canchaNombre(r.getCancha().getNombre())
                        .canchaDeporte(r.getCancha().getDeporte())
                        .fecha(r.getFecha().format(FMT_FECHA))
                        .horaInicio(r.getHoraInicio().format(FMT_HORA))
                        .horaFin(r.getHoraFin().format(FMT_HORA))
                        .estado(r.getEstado().name())
                        .total(r.getTotal())
                        .build())
                .toList();

        return DashboardClienteResponse.builder()
                .nombreCompleto(cliente.getNombres() + " " + cliente.getApellidos())
                .email(cliente.getEmail())
                .tipoDocumento(cliente.getTipoDocumento())
                .numeroDocumento(cliente.getNumeroDocumento())
                .totalReservas(totalReservas)
                .reservasPendientes(reservasPendientes)
                .reservasConfirmadas(reservasConfirmadas)
                .reservasCanceladas(reservasCanceladas)
                .totalGastado(totalGastado)
                .proximasReservas(proximasDTO)
                .historialReservas(historialDTO)
                .build();
    }

    private Long contarReservasPropietario(Long propietarioId) {
        return (long) reservaRepository
                .findByPropietarioId(propietarioId).size();
    }

    private Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado"));
    }
}