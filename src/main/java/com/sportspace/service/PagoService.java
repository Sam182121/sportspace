package com.sportspace.service;

import com.sportspace.dto.request.PagoRequest;
import com.sportspace.dto.response.PagoResponse;
import com.sportspace.entity.*;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.PagoRepository;
import com.sportspace.repository.ReservaRepository;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository    pagoRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // CLIENTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * El cliente registra un pago (con voucher) para su reserva.
     *
     * Flujo:
     *  1. Verifica que la reserva exista y pertenezca al cliente autenticado.
     *  2. Verifica que no haya otro pago ya registrado para la misma reserva.
     *  3. Crea el pago en estado PENDIENTE con el monto de la reserva y el voucher.
     *
     * La reserva PERMANECE en estado PENDIENTE — el propietario debe aprobarla
     * o rechazarla desde /api/propietario/reservas/{id}/aprobar|rechazar.
     */
    @Transactional
    public PagoResponse registrarPago(PagoRequest request) {
        Usuario cliente = getUsuarioAutenticado();
        validarRol(cliente, Rol.CLIENTE);

        Reserva reserva = reservaRepository.findById(request.getReservaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva no encontrada con id: " + request.getReservaId()));

        if (!reserva.getCliente().getId().equals(cliente.getId()))
            throw new BadRequestException("No puedes pagar una reserva que no es tuya");

        if (reserva.getEstado() != EstadoReserva.PENDIENTE)
            throw new BadRequestException(
                    "Solo se puede registrar un pago para reservas en estado PENDIENTE. " +
                            "Estado actual: " + reserva.getEstado());

        if (pagoRepository.existsByReservaId(reserva.getId()))
            throw new BadRequestException(
                    "Ya existe un pago registrado para esta reserva");

        boolean requiereVoucher = request.getMetodo() != Pago.MetodoPago.EFECTIVO;
        if (requiereVoucher && (request.getVoucherUrl() == null || request.getVoucherUrl().isBlank()))
            throw new BadRequestException(
                    "Debes adjuntar el comprobante (voucher) de pago para este método");

        Pago pago = Pago.builder()
                .reserva(reserva)
                .monto(reserva.getTotal())
                .metodo(request.getMetodo())
                .estado(Pago.EstadoPago.PENDIENTE)
                .voucherUrl(request.getVoucherUrl())
                .build();

        // La reserva queda en PENDIENTE: el propietario debe aprobar o rechazar.
        return toResponse(pagoRepository.save(pago));
    }

    /**
     * El cliente consulta el pago de una reserva específica.
     */
    public PagoResponse miPago(Long reservaId) {
        Usuario cliente = getUsuarioAutenticado();

        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva no encontrada con id: " + reservaId));

        if (!reserva.getCliente().getId().equals(cliente.getId()))
            throw new BadRequestException("No tienes acceso a esta reserva");

        Pago pago = pagoRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró pago para la reserva con id: " + reservaId));

        return toResponse(pago);
    }

    /**
     * El cliente consulta todos sus pagos.
     */
    public List<PagoResponse> misPagos() {
        Usuario cliente = getUsuarioAutenticado();
        return pagoRepository.findByClienteId(cliente.getId())
                .stream().map(this::toResponse).toList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROPIETARIO
    // El trato es entre propietario y cliente. El propietario gestiona
    // confirmaciones, rechazos y reembolsos directamente.
    //
    // NOTA: la aprobación/rechazo de la RESERVA (con motivo corto) vive en
    // PropietarioReservaController (/api/propietario/reservas/{id}/aprobar|rechazar),
    // que también sincroniza el estado del Pago. Los métodos de abajo
    // (confirmarPago/rechazarPago) quedan como API alternativa de solo-pago,
    // pero el flujo principal de la app usa el controller de reservas.
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public PagoResponse confirmarPago(Long pagoId, String notas) {
        Usuario propietario = getUsuarioAutenticado();
        validarRol(propietario, Rol.PROPIETARIO);

        Pago pago = buscarPorId(pagoId);
        validarPropietarioDePago(pago, propietario);

        if (pago.getEstado() != Pago.EstadoPago.PENDIENTE)
            throw new BadRequestException(
                    "Solo se pueden confirmar pagos en estado PENDIENTE. " +
                            "Estado actual: " + pago.getEstado());

        pago.setEstado(Pago.EstadoPago.COMPLETADO);
        pago.setFechaPago(LocalDateTime.now());
        pago.setNotas(notas);

        Reserva reserva = pago.getReserva();
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);

        return toResponse(pagoRepository.save(pago));
    }

    /**
     * El propietario rechaza el pago (transferencia no verificada, monto incorrecto, etc).
     * La reserva pasa a CANCELADA y el horario queda libre para otro cliente.
     */
    @Transactional
    public PagoResponse rechazarPago(Long pagoId, String notas) {
        Usuario propietario = getUsuarioAutenticado();
        validarRol(propietario, Rol.PROPIETARIO);

        Pago pago = buscarPorId(pagoId);
        validarPropietarioDePago(pago, propietario);

        if (pago.getEstado() != Pago.EstadoPago.PENDIENTE)
            throw new BadRequestException(
                    "Solo se pueden rechazar pagos en estado PENDIENTE");

        pago.setEstado(Pago.EstadoPago.RECHAZADO);
        pago.setNotas(notas);

        Reserva reserva = pago.getReserva();
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);

        return toResponse(pagoRepository.save(pago));
    }

    /**
     * El propietario procesa el reembolso al cliente (acuerdo entre ellos),
     * adjuntando un comprobante de la devolución y un mensaje corto.
     * Requiere: pago COMPLETADO + reserva CANCELADA (cancelada por el cliente).
     */
    @Transactional
    public PagoResponse procesarReembolso(Long pagoId, String notas, String voucherReembolsoUrl) {
        Usuario propietario = getUsuarioAutenticado();
        validarRol(propietario, Rol.PROPIETARIO);

        Pago pago = buscarPorId(pagoId);
        validarPropietarioDePago(pago, propietario);

        if (pago.getEstado() != Pago.EstadoPago.COMPLETADO)
            throw new BadRequestException(
                    "Solo se puede reembolsar un pago COMPLETADO. " +
                            "Estado actual: " + pago.getEstado());

        Reserva reserva = pago.getReserva();

        if (reserva.getEstado() != EstadoReserva.CANCELADA)
            throw new BadRequestException(
                    "El reembolso solo aplica a reservas CANCELADAS por el cliente");

        if (Boolean.TRUE.equals(reserva.getReembolsoProcesado()))
            throw new BadRequestException(
                    "Esta reserva ya tiene un reembolso procesado");

        if (voucherReembolsoUrl == null || voucherReembolsoUrl.isBlank())
            throw new BadRequestException(
                    "Debes adjuntar el comprobante de la devolución");

        pago.setEstado(Pago.EstadoPago.REEMBOLSADO);
        pago.setNotas(notas);
        pago.setVoucherReembolsoUrl(voucherReembolsoUrl);

        reserva.setReembolsoProcesado(true);
        reservaRepository.save(reserva);

        return toResponse(pagoRepository.save(pago));
    }

    /**
     * El propietario lista todos los pagos de sus canchas.
     */
    public List<PagoResponse> pagosDeMisCanchas() {
        Usuario propietario = getUsuarioAutenticado();
        validarRol(propietario, Rol.PROPIETARIO);
        return pagoRepository.findByPropietarioId(propietario.getId())
                .stream().map(this::toResponse).toList();
    }

    /**
     * Ingresos totales del propietario (solo pagos COMPLETADOS).
     */
    public Map<String, BigDecimal> misIngresos() {
        Usuario propietario = getUsuarioAutenticado();
        validarRol(propietario, Rol.PROPIETARIO);
        BigDecimal total = pagoRepository
                .sumIngresosCompletadosByPropietario(propietario.getId());
        return Map.of("totalIngresos", total);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADMIN — solo lectura para supervisión
    // ══════════════════════════════════════════════════════════════════════════

    public List<PagoResponse> listarTodos() {
        return pagoRepository.findAllOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public PagoResponse obtenerPorId(Long id) {
        return toResponse(buscarPorId(id));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════════════

    private Pago buscarPorId(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pago no encontrado con id: " + id));
    }

    private Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado"));
    }

    private void validarRol(Usuario usuario, Rol rolRequerido) {
        if (usuario.getRol() != rolRequerido)
            throw new BadRequestException(
                    "Acción no permitida para el rol: " + usuario.getRol());
    }

    /** Verifica que el pago pertenezca a una cancha del propietario autenticado. */
    private void validarPropietarioDePago(Pago pago, Usuario propietario) {
        if (!pago.getReserva().getCancha().getPropietario()
                .getId().equals(propietario.getId()))
            throw new BadRequestException(
                    "No tienes permiso para gestionar este pago");
    }

    PagoResponse toResponse(Pago p) {
        Reserva r  = p.getReserva();
        Cancha  ca = r.getCancha();

        String clienteNombre = r.getCliente().getNombres()
                + " " + r.getCliente().getApellidos();

        return PagoResponse.builder()
                .id(p.getId())
                .monto(p.getMonto())
                .metodo(p.getMetodo())
                .estado(p.getEstado())
                .fechaPago(p.getFechaPago())
                .notas(p.getNotas())
                .voucherUrl(p.getVoucherUrl())
                .voucherReembolsoUrl(p.getVoucherReembolsoUrl())
                .reservaId(r.getId())
                .reservaEstado(r.getEstado().name())
                .reservaFecha(r.getFecha())
                .reservaHoraInicio(r.getHoraInicio())
                .reservaHoraFin(r.getHoraFin())
                .canchaId(ca.getId())
                .canchaNombre(ca.getNombre())
                .canchaDeporte(ca.getDeporte())
                .canchaDistrito(ca.getDistrito())
                .clienteId(r.getCliente().getId())
                .clienteNombre(clienteNombre)
                .clienteEmail(r.getCliente().getEmail())
                .build();
    }
}