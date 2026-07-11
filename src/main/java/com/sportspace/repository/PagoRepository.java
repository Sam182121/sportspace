package com.sportspace.repository;

import com.sportspace.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    // ── Búsquedas básicas ─────────────────────────────────────────────────────

    Optional<Pago> findByReservaId(Long reservaId);

    boolean existsByReservaId(Long reservaId);

    // ── Borrado en cascada (eliminación forzada de usuario) ──
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Pago p WHERE p.reserva.cliente.id = :clienteId")
    void deleteByReservaClienteId(@Param("clienteId") Long clienteId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Pago p WHERE p.reserva.cancha.propietario.id = :propietarioId")
    void deleteByReservaCanchaPropietarioId(@Param("propietarioId") Long propietarioId);

    // ── Dashboard admin ───────────────────────────────────────────────────────

    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.reserva r
        JOIN FETCH r.cliente c
        JOIN FETCH r.cancha ca
        ORDER BY p.createdAt DESC
    """)
    List<Pago> findAllOrderByCreatedAtDesc();

    /** Alias para compatibilidad con AdminDashboardService */
    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.reserva r
        JOIN FETCH r.cliente c
        JOIN FETCH r.cancha ca
        ORDER BY p.createdAt DESC
    """)
    List<Pago> findAllOrderByFechaPagoDesc();

    // ── Ingresos ──────────────────────────────────────────────────────────────

    /**
     * Suma ingresos reales en un período.
     * FIX: excluye pagos cuya reserva esté CANCELADA — esos están en proceso
     *      de reembolso y no deben contar como ingreso.
     */
    @Query("""
        SELECT COALESCE(SUM(p.monto), 0) FROM Pago p
        JOIN p.reserva r
        WHERE p.estado    = 'COMPLETADO'
        AND   r.estado   != 'CANCELADA'
        AND   p.fechaPago >= :inicio
        AND   p.fechaPago <= :fin
    """)
    BigDecimal sumIngresosEnPeriodo(@Param("inicio") LocalDateTime inicio,
                                    @Param("fin")    LocalDateTime fin);

    /**
     * Pagos COMPLETADOS de reservas NO canceladas desde una fecha.
     * FIX: excluye reservas CANCELADAS para que el gráfico de ingresos
     *      diarios no cuente montos en proceso de reembolso.
     */
    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.reserva r
        JOIN FETCH r.cancha ca
        WHERE p.estado    = 'COMPLETADO'
        AND   r.estado   != 'CANCELADA'
        AND   p.fechaPago >= :desde
        ORDER BY p.fechaPago ASC
    """)
    List<Pago> findCompletadosDesde(@Param("desde") LocalDateTime desde);

    // ── Por propietario ───────────────────────────────────────────────────────

    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.reserva r
        JOIN FETCH r.cancha ca
        JOIN FETCH r.cliente c
        WHERE ca.propietario.id = :propietarioId
        ORDER BY p.createdAt DESC
    """)
    List<Pago> findByPropietarioId(@Param("propietarioId") Long propietarioId);

    /**
     * Ingresos del propietario.
     * FIX: excluye reservas CANCELADAS igual que el admin.
     */
    @Query("""
        SELECT COALESCE(SUM(p.monto), 0) FROM Pago p
        JOIN p.reserva r
        JOIN r.cancha ca
        WHERE ca.propietario.id = :propietarioId
        AND   p.estado          = 'COMPLETADO'
        AND   r.estado         != 'CANCELADA'
    """)
    BigDecimal sumIngresosCompletadosByPropietario(
            @Param("propietarioId") Long propietarioId);

    // ── Por cliente ───────────────────────────────────────────────────────────

    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.reserva r
        JOIN FETCH r.cancha ca
        WHERE r.cliente.id = :clienteId
        ORDER BY p.createdAt DESC
    """)
    List<Pago> findByClienteId(@Param("clienteId") Long clienteId);
}