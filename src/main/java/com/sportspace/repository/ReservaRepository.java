package com.sportspace.repository;

import com.sportspace.entity.EstadoReserva;
import com.sportspace.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Por cliente
    List<Reserva> findByClienteIdOrderByFechaDescHoraInicioDesc(Long clienteId);

    // Por cancha
    List<Reserva> findByCanchaIdOrderByFechaDescHoraInicioDesc(Long canchaId);

    //  Por cancha y fecha
    List<Reserva> findByCanchaIdAndFechaAndEstadoNot(
            Long canchaId, LocalDate fecha, EstadoReserva estado);

    // Validar solapamiento
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.cancha.id  = :canchaId
        AND   r.fecha      = :fecha
        AND   r.estado    <> 'CANCELADA'
        AND   r.horaInicio < :horaFin
        AND   r.horaFin    > :horaInicio
    """)
    boolean existeConflictoHorario(
            @Param("canchaId")   Long canchaId,
            @Param("fecha")      LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin")    LocalTime horaFin
    );

    //  Mismo cliente, misma cancha, mismo horario
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.cancha.id  = :canchaId
        AND   r.cliente.id = :clienteId
        AND   r.fecha      = :fecha
        AND   r.estado    <> 'CANCELADA'
        AND   r.horaInicio < :horaFin
        AND   r.horaFin    > :horaInicio
    """)
    boolean clienteYaReservo(
            @Param("canchaId")   Long canchaId,
            @Param("clienteId")  Long clienteId,
            @Param("fecha")      LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin")    LocalTime horaFin
    );

    // Ingresos del propietario (confirmadas)
    @Query("""
        SELECT COALESCE(SUM(r.total), 0) FROM Reserva r
        WHERE r.cancha.propietario.id = :propietarioId
        AND   r.estado IN ('CONFIRMADA','COMPLETADA')
    """)
    BigDecimal sumIngresosConfirmados(@Param("propietarioId") Long propietarioId);

    // Reservas del propietario (todas sus canchas)
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.cancha.propietario.id = :propietarioId
        ORDER BY r.fecha DESC, r.horaInicio DESC
    """)
    List<Reserva> findByPropietarioId(@Param("propietarioId") Long propietarioId);

    // Conteo por estado y cancha
    Long countByCanchaIdAndEstado(Long canchaId, EstadoReserva estado);
    Long countByCanchaId(Long canchaId);

    // Conteo global por estado
    Long countByEstado(EstadoReserva estado);

    // Todas las reservas en un estado dado (usado por el scheduler de completadas)
    List<Reserva> findByEstado(EstadoReserva estado);

    // ── Borrado en cascada (eliminación forzada de usuario) ──
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Reserva r WHERE r.cliente.id = :clienteId")
    void deleteByClienteId(@Param("clienteId") Long clienteId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Reserva r WHERE r.cancha.propietario.id = :propietarioId")
    void deleteByCanchaPropietarioId(@Param("propietarioId") Long propietarioId);

    // ── Badges de la sección Reservas (propietario) ──
    @Query("""
        SELECT COUNT(r) FROM Reserva r
        WHERE r.cancha.propietario.id = :propietarioId AND r.estado = 'PENDIENTE'
    """)
    Long countPendientesByPropietarioId(@Param("propietarioId") Long propietarioId);

    @Query("""
        SELECT COUNT(r) FROM Reserva r
        WHERE r.cancha.propietario.id = :propietarioId
        AND   r.estado = 'CANCELADA' AND r.canceladoPor = 'CLIENTE'
    """)
    Long countCanceladasClienteByPropietarioId(@Param("propietarioId") Long propietarioId);

    @Query("""
        SELECT COUNT(r) FROM Reserva r
        WHERE r.cancha.propietario.id = :propietarioId
        AND   r.estado = 'CANCELADA' AND r.reembolsoProcesado = false
    """)
    Long countReembolsoPendienteByPropietarioId(@Param("propietarioId") Long propietarioId);

    //  Ingresos por cancha (confirmadas)
    @Query("""
        SELECT COALESCE(SUM(r.total), 0) FROM Reserva r
        WHERE r.cancha.id = :canchaId
        AND   r.estado   = 'CONFIRMADA'
    """)
    BigDecimal sumIngresosPorCancha(@Param("canchaId") Long canchaId);

    //  Ingresos totales plataforma
    @Query("""
        SELECT COALESCE(SUM(r.total), 0) FROM Reserva r
        WHERE r.estado IN ('CONFIRMADA','COMPLETADA')
    """)
    BigDecimal sumIngresosTotalesPlataforma();

    // Ingresos este mes para un propietario
    @Query("""
        SELECT COALESCE(SUM(r.total), 0) FROM Reserva r
        WHERE r.cancha.propietario.id = :propietarioId
        AND   r.estado               IN ('CONFIRMADA','COMPLETADA')
        AND   YEAR(r.fecha)          = YEAR(CURRENT_DATE)
        AND   MONTH(r.fecha)         = MONTH(CURRENT_DATE)
    """)
    BigDecimal sumIngresosEsteMes(@Param("propietarioId") Long propietarioId);

    // Total gastado por cliente
    @Query("""
        SELECT COALESCE(SUM(r.total), 0) FROM Reserva r
        WHERE r.cliente.id = :clienteId
        AND   r.estado     = 'CONFIRMADA'
    """)
    BigDecimal sumTotalGastadoByClienteId(@Param("clienteId") Long clienteId);

    //  Próximas reservas del cliente
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.cliente.id = :clienteId
        AND   r.fecha      >= CURRENT_DATE
        AND   r.estado    <> 'CANCELADA'
        ORDER BY r.fecha ASC, r.horaInicio ASC
    """)
    List<Reserva> findProximasReservasByClienteId(@Param("clienteId") Long clienteId);

    // Historial del cliente (pasadas)
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.cliente.id = :clienteId
        AND   r.fecha      < CURRENT_DATE
        ORDER BY r.fecha DESC, r.horaInicio DESC
    """)
    List<Reserva> findHistorialByClienteId(@Param("clienteId") Long clienteId);

    // Por fecha
    Long countByFecha(LocalDate fecha);
    Long countByFechaAndEstado(LocalDate fecha, EstadoReserva estado);

    // Últimas reservas (todas, ordenadas por creación)
    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.cliente c
        JOIN FETCH r.cancha ca
        ORDER BY r.createdAt DESC
    """)
    List<Reserva> findAllOrderByCreatedAtDesc();

    //Alias para actividad reciente
    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.cliente c
        JOIN FETCH r.cancha ca
        ORDER BY r.createdAt DESC
    """)
    List<Reserva> findTopRecientes();

    // Agrupado por deporte
    @Query("""
        SELECT r.cancha.deporte, COUNT(r)
        FROM Reserva r
        GROUP BY r.cancha.deporte
        ORDER BY COUNT(r) DESC
    """)
    List<Object[]> countByDeporte();
}