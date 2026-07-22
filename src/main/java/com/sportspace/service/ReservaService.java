package com.sportspace.service;

import com.sportspace.dto.request.CambioEstadoRequest;
import com.sportspace.dto.request.ReservaRequest;
import com.sportspace.dto.response.DisponibilidadResponse;
import com.sportspace.dto.response.DisponibilidadResponse.SlotDisponibilidad;
import com.sportspace.dto.response.ReservaResponse;
import com.sportspace.entity.*;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.CanchaRepository;
import com.sportspace.repository.FechaBloqueadaRepository;
import com.sportspace.repository.HorarioSlotRepository;
import com.sportspace.repository.PagoRepository;
import com.sportspace.repository.ReservaRepository;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository        reservaRepository;
    private final CanchaRepository         canchaRepository;
    private final UsuarioRepository        usuarioRepository;
    private final HorarioSlotRepository    horarioSlotRepository;
    private final FechaBloqueadaRepository fechaBloqueadaRepository;
    private final PagoRepository           pagoRepository;
    private final NotificacionService      notificacionService;

    //  CLIENTE: crear reserva

    @Transactional
    public ReservaResponse crear(ReservaRequest request) {
        Usuario cliente = getUsuarioAutenticado();
        validarRol(cliente, Rol.CLIENTE);

        Cancha cancha = canchaRepository.findById(request.getCanchaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cancha no encontrada con id: " + request.getCanchaId()));

        if (!cancha.getActiva()) {
            throw new BadRequestException("La cancha no está disponible");
        }

        validarHorario(request.getHoraInicio(), request.getHoraFin());
        validarFechaNoAnterior(request.getFecha());
        validarSlotOfertado(cancha.getId(), request.getFecha(),
                request.getHoraInicio(), request.getHoraFin());

        if (reservaRepository.existeConflictoHorario(
                cancha.getId(), request.getFecha(),
                request.getHoraInicio(), request.getHoraFin())) {
            throw new BadRequestException(
                    "El horario seleccionado ya está reservado. Elige otro horario.");
        }

        if (reservaRepository.clienteYaReservo(
                cancha.getId(), cliente.getId(), request.getFecha(),
                request.getHoraInicio(), request.getHoraFin())) {
            throw new BadRequestException(
                    "Ya tienes una reserva en este horario para esta cancha.");
        }

        BigDecimal total = calcularTotal(
                cancha.getPrecioHora(),
                request.getHoraInicio(),
                request.getHoraFin());

        Reserva reserva = Reserva.builder()
                .cliente(cliente)
                .cancha(cancha)
                .fecha(request.getFecha())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .estado(EstadoReserva.PENDIENTE)
                .total(total)
                .build();

        Reserva guardada = reservaRepository.save(reserva);

        try {
            notificacionService.crear(
                    cancha.getPropietario(),
                    Notificacion.TipoNotificacion.NUEVA_RESERVA,
                    "Nueva reserva recibida",
                    "%s reservó \"%s\" el %s de %s a %s".formatted(
                            cliente.getNombres() + " " + cliente.getApellidos(),
                            cancha.getNombre(),
                            request.getFecha(),
                            request.getHoraInicio(),
                            request.getHoraFin()),
                    guardada.getId()
            );
        } catch (Exception ignored) { /* la notificación nunca debe romper la reserva */ }

        return toResponse(guardada);
    }

    // CLIENTE: ver mis reservas

    public List<ReservaResponse> misReservas() {
        Usuario cliente = getUsuarioAutenticado();
        return reservaRepository
                .findByClienteIdOrderByFechaDescHoraInicioDesc(cliente.getId())
                .stream().map(this::toResponse).toList();
    }

    // CLIENTE: cancelar mi reserva

    @Transactional
    public ReservaResponse cancelarMiReserva(Long id) {
        Usuario cliente = getUsuarioAutenticado();
        Reserva reserva = buscarPorId(id);

        if (!reserva.getCliente().getId().equals(cliente.getId()))
            throw new BadRequestException("No puedes cancelar una reserva que no es tuya");
        if (reserva.getEstado() == EstadoReserva.CANCELADA)
            throw new BadRequestException("La reserva ya está cancelada");
        if (reserva.getEstado() == EstadoReserva.COMPLETADA)
            throw new BadRequestException("No puedes cancelar una reserva ya completada");

        LocalDateTime inicioPartido = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());

        // PENDIENTE → no se puede cancelar: el propietario aún no ha aprobado.
        //   El cliente debe esperar a que el propietario apruebe o rechace.
        // CONFIRMADA → se puede cancelar con más de 24h de anticipación.
        // En ambos casos: si el partido ya comenzó, tampoco se puede cancelar.

        if (reserva.getEstado() == EstadoReserva.PENDIENTE)
            throw new BadRequestException(
                    "No puedes cancelar una reserva pendiente de aprobación. " +
                            "Espera a que el propietario la apruebe o rechace.");

        if (!inicioPartido.isAfter(LocalDateTime.now()))
            throw new BadRequestException(
                    "No puedes cancelar: el horario de esta reserva ya comenzó o pasó");

        if (reserva.getEstado() == EstadoReserva.CONFIRMADA
                && reserva.getFecha().isBefore(LocalDate.now().plusDays(1)))
            throw new BadRequestException(
                    "No puedes cancelar una reserva confirmada con menos de 24 horas de anticipación");

        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva.setCanceladoPor(Reserva.CanceladoPor.CLIENTE);
        Reserva guardada = reservaRepository.save(reserva);

        try {
            notificacionService.crear(
                    reserva.getCancha().getPropietario(),
                    Notificacion.TipoNotificacion.RESERVA_CANCELADA_CLIENTE,
                    "Reserva cancelada por el cliente",
                    "%s canceló su reserva en \"%s\" del %s de %s a %s".formatted(
                            cliente.getNombres() + " " + cliente.getApellidos(),
                            reserva.getCancha().getNombre(),
                            reserva.getFecha(),
                            reserva.getHoraInicio(),
                            reserva.getHoraFin()),
                    guardada.getId()
            );
        } catch (Exception ignored) { /* la notificación nunca debe romper la cancelación */ }

        return toResponse(guardada);
    }

    // PROPIETARIO: reservas de su cancha

    public List<ReservaResponse> reservasPorCancha(Long canchaId) {
        Usuario propietario = getUsuarioAutenticado();
        Cancha cancha = canchaRepository.findById(canchaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cancha no encontrada con id: " + canchaId));

        if (!cancha.getPropietario().getId().equals(propietario.getId())
                && propietario.getRol() != Rol.ADMIN)
            throw new BadRequestException("No tienes permiso para ver estas reservas");

        return reservaRepository
                .findByCanchaIdOrderByFechaDescHoraInicioDesc(canchaId)
                .stream().map(this::toResponse).toList();
    }

    public List<ReservaResponse> misReservasPropietario() {
        Usuario propietario = getUsuarioAutenticado();
        return reservaRepository.findByPropietarioId(propietario.getId())
                .stream().map(this::toResponse).toList();
    }

    // PROPIETARIO / ADMIN: cambiar estado

    @Transactional
    public ReservaResponse cambiarEstado(Long id, CambioEstadoRequest request) {
        Usuario usuario = getUsuarioAutenticado();
        Reserva reserva = buscarPorId(id);

        if (usuario.getRol() == Rol.PROPIETARIO) {
            if (!reserva.getCancha().getPropietario().getId().equals(usuario.getId()))
                throw new BadRequestException("No puedes gestionar esta reserva");
        }

        validarTransicionEstado(reserva.getEstado(), request.getEstado());
        reserva.setEstado(request.getEstado());
        return toResponse(reservaRepository.save(reserva));
    }

    // ADMIN: cambiar estado

    @Transactional
    public ReservaResponse cambiarEstadoAdmin(Long id, String estadoStr) {
        EstadoReserva nuevoEstado;
        try {
            nuevoEstado = EstadoReserva.valueOf(estadoStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Estado inválido: " + estadoStr +
                            ". Use: PENDIENTE, CONFIRMADA, CANCELADA o COMPLETADA");
        }

        Reserva reserva = buscarPorId(id);
        validarTransicionEstado(reserva.getEstado(), nuevoEstado);
        reserva.setEstado(nuevoEstado);
        return toResponse(reservaRepository.save(reserva));
    }

    // ADMIN: procesar reembolso

    @Transactional
    public ReservaResponse procesarReembolso(Long id) {
        Reserva reserva = buscarPorId(id);

        if (reserva.getEstado() != EstadoReserva.CANCELADA
                && reserva.getEstado() != EstadoReserva.COMPLETADA) {
            throw new BadRequestException(
                    "Solo se puede procesar reembolso de reservas canceladas o completadas");
        }

        if (Boolean.TRUE.equals(reserva.getReembolsoProcesado())) {
            throw new BadRequestException("Esta reserva ya tiene un reembolso procesado");
        }

        reserva.setReembolsoProcesado(true);
        return toResponse(reservaRepository.save(reserva));
    }

    // DISPONIBILIDAD

    /**
     * Calcula la disponibilidad real de una cancha para una fecha:
     *   1. Si el propietario bloqueó esa fecha (mantenimiento, feriado...) → sin slots.
     *   2. Toma el horario semanal configurado por el propietario para ese día
     *      (solo las horas marcadas DISPONIBLE aparecen como opción).
     *   3. Marca como ocupado cualquier slot que se cruce con una reserva activa
     *      (PENDIENTE o CONFIRMADA — solo CANCELADA libera el horario).
     *   4. Si la fecha es hoy, las horas que ya pasaron también quedan bloqueadas.
     */
    public DisponibilidadResponse consultarDisponibilidad(Long canchaId, LocalDate fecha) {
        Cancha cancha = canchaRepository.findById(canchaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cancha no encontrada con id: " + canchaId));

        Optional<FechaBloqueada> bloqueo = fechaBloqueadaRepository
                .findByCanchaIdOrderByFechaAsc(canchaId).stream()
                .filter(fb -> fb.getFecha().equals(fecha))
                .findFirst();

        if (bloqueo.isPresent()) {
            return DisponibilidadResponse.builder()
                    .canchaId(canchaId)
                    .canchaNombre(cancha.getNombre())
                    .fecha(fecha)
                    .bloqueada(true)
                    .motivoBloqueo(bloqueo.get().getMotivo())
                    .slots(List.of())
                    .build();
        }

        int diaSemana = fecha.getDayOfWeek().getValue() - 1; // 0=Lun ... 6=Dom

        List<HorarioSlot> slotsConfigurados = horarioSlotRepository.findByCanchaId(canchaId).stream()
                .filter(s -> s.getDiaSemana() != null && s.getDiaSemana() == diaSemana)
                .filter(s -> "DISPONIBLE".equals(s.getEstado()))
                .sorted((a, b) -> a.getHora().compareTo(b.getHora()))
                .toList();

        List<Reserva> reservasDelDia = reservaRepository
                .findByCanchaIdAndFechaAndEstadoNot(canchaId, fecha, EstadoReserva.CANCELADA);

        boolean esHoy = fecha.isEqual(LocalDate.now());
        LocalTime ahora = LocalTime.now();

        List<SlotDisponibilidad> slots = slotsConfigurados.stream().map(s -> {
            LocalTime inicio = LocalTime.of(s.getHora(), 0);
            LocalTime fin    = inicio.plusHours(1);

            boolean ocupado = reservasDelDia.stream().anyMatch(r ->
                    inicio.isBefore(r.getHoraFin()) && r.getHoraInicio().isBefore(fin));
            boolean yaPaso = esHoy && !inicio.isAfter(ahora);

            return SlotDisponibilidad.builder()
                    .inicio(inicio)
                    .fin(fin)
                    .disponible(!ocupado && !yaPaso)
                    .build();
        }).toList();

        return DisponibilidadResponse.builder()
                .canchaId(canchaId)
                .canchaNombre(cancha.getNombre())
                .fecha(fecha)
                .bloqueada(false)
                .slots(slots)
                .build();
    }

    // ADMIN: listar todas

    public List<ReservaResponse> listarTodas() {
        return reservaRepository.findAllOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public ReservaResponse obtenerPorId(Long id) {
        return toResponse(buscarPorId(id));
    }

    // INGRESOS

    public BigDecimal misIngresos() {
        Usuario propietario = getUsuarioAutenticado();
        return reservaRepository.sumIngresosConfirmados(propietario.getId());
    }

    // HELPERS PRIVADOS

    private Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva no encontrada con id: " + id));
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
            throw new BadRequestException("Solo los clientes pueden realizar reservas");
    }

    private void validarHorario(LocalTime inicio, LocalTime fin) {
        if (!fin.isAfter(inicio))
            throw new BadRequestException(
                    "La hora de fin debe ser posterior a la hora de inicio");
        if (inicio.isBefore(LocalTime.of(6, 0)) || fin.isAfter(LocalTime.of(23, 0)))
            throw new BadRequestException(
                    "El horario debe estar entre las 06:00 y las 23:00");
    }

    private void validarFechaNoAnterior(LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now()))
            throw new BadRequestException(
                    "No se pueden crear reservas para fechas pasadas");
    }

    /**
     * Verifica que el horario solicitado corresponda a un slot que el propietario
     * realmente ofrece (horario semanal) y que la fecha no esté bloqueada.
     * Evita que un cliente reserve una hora que nunca fue publicada.
     */
    private void validarSlotOfertado(Long canchaId, LocalDate fecha,
                                     LocalTime horaInicio, LocalTime horaFin) {
        boolean fechaBloqueada = fechaBloqueadaRepository
                .findByCanchaIdOrderByFechaAsc(canchaId).stream()
                .anyMatch(fb -> fb.getFecha().equals(fecha));
        if (fechaBloqueada)
            throw new BadRequestException("Esta fecha no está disponible para reservas");

        int diaSemana = fecha.getDayOfWeek().getValue() - 1;
        boolean ofertado = horarioSlotRepository.findByCanchaId(canchaId).stream()
                .anyMatch(s -> s.getDiaSemana() != null && s.getDiaSemana() == diaSemana
                        && "DISPONIBLE".equals(s.getEstado())
                        && s.getHora() != null && s.getHora() == horaInicio.getHour());
        if (!ofertado)
            throw new BadRequestException(
                    "El horario seleccionado no está disponible. Elige otro horario.");

        if (fecha.isEqual(LocalDate.now()) && !horaInicio.isAfter(LocalTime.now()))
            throw new BadRequestException("Esa hora ya pasó. Elige otro horario.");
    }

    private void validarTransicionEstado(EstadoReserva actual, EstadoReserva nuevo) {
        if (actual == EstadoReserva.CANCELADA)
            throw new BadRequestException(
                    "No se puede cambiar el estado de una reserva cancelada");
        if (actual == EstadoReserva.CONFIRMADA && nuevo == EstadoReserva.PENDIENTE)
            throw new BadRequestException(
                    "No se puede volver a estado PENDIENTE desde CONFIRMADA");
    }

    private BigDecimal calcularTotal(BigDecimal precioHora,
                                     LocalTime inicio, LocalTime fin) {
        long minutos = Duration.between(inicio, fin).toMinutes();
        BigDecimal horas = BigDecimal.valueOf(minutos)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return precioHora.multiply(horas).setScale(2, RoundingMode.HALF_UP);
    }

    ReservaResponse toResponse(Reserva r) {
        String clienteNombre = r.getCliente().getNombres()
                + " " + r.getCliente().getApellidos();
        String clienteEmail  = r.getCliente().getEmail();

        String propietarioNombre = r.getCancha().getPropietario().getNombres()
                + " " + r.getCancha().getPropietario().getApellidos();

        double duracionHoras = Duration
                .between(r.getHoraInicio(), r.getHoraFin())
                .toMinutes() / 60.0;

        return ReservaResponse.builder()
                .id(r.getId())
                .estado(r.getEstado())
                .fecha(r.getFecha())
                .horaInicio(r.getHoraInicio())
                .horaFin(r.getHoraFin())
                .total(r.getTotal())
                .createdAt(r.getCreatedAt())
                .reembolsoProcesado(r.getReembolsoProcesado())
                .canceladoPor(r.getCanceladoPor() != null ? r.getCanceladoPor().name() : null)
                // Cancha
                .canchaId(r.getCancha().getId())
                .canchaNombre(r.getCancha().getNombre())
                .canchaDeporte(r.getCancha().getDeporte())
                .deporte(r.getCancha().getDeporte())          // alias sin prefijo
                .canchaDistrito(r.getCancha().getDistrito())
                .canhaDistrito(r.getCancha().getDistrito())   // alias typo del JS
                .canchaDireccion(r.getCancha().getDireccion())
                .canchaPrecioHora(r.getCancha().getPrecioHora())
                // Cliente
                .clienteId(r.getCliente().getId())
                .clienteNombre(clienteNombre)
                .clienteEmail(clienteEmail)
                .clienteTelefono(r.getCliente().getTelefono())
                .usuarioNombre(clienteNombre)                  // alias para el JS
                .usuarioEmail(clienteEmail)                    // alias para el JS
                // Propietario
                .propietarioId(r.getCancha().getPropietario().getId())
                .propietarioNombre(propietarioNombre)
                .propietarioEmail(r.getCancha().getPropietario().getEmail())
                // Calculados
                .duracionHoras(duracionHoras)
                .build();
    }
}