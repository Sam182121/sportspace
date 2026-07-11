package com.sportspace.repository;

import com.sportspace.entity.Cancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CanchaRepository extends JpaRepository<Cancha, Long> {

    // FIX — antes solo se chequeaba "estado", por lo que despublicar()
    // (que solo cambia el campo "activa") no tenía ningún efecto sobre
    // lo que veía el cliente: una cancha despublicada seguía teniendo
    // estado='ACTIVA' y por lo tanto seguía apareciendo en /canchas/publico.
    // Ahora se exige TANTO el nivel de aprobación del admin (estado)
    // COMO el toggle de visibilidad del propietario (activa).
    @Query("SELECT c FROM Cancha c WHERE c.estado IN ('ACTIVA','DESTACADA') AND c.activa = true")
    List<Cancha> findByActivaTrue();

    List<Cancha> findByPropietarioId(Long propietarioId);

    @Query("SELECT c FROM Cancha c WHERE c.estado IN ('ACTIVA','DESTACADA') AND c.activa = true AND LOWER(c.deporte) = LOWER(:deporte)")
    List<Cancha> findByDeporteIgnoreCaseAndActivaTrue(@Param("deporte") String deporte);

    // Buscar por distrito y deporte (solo canchas visibles)
    @Query("""
        SELECT c FROM Cancha c
        WHERE c.estado IN ('ACTIVA','DESTACADA')
        AND c.activa = true
        AND (:distrito IS NULL OR LOWER(c.distrito)    = LOWER(:distrito))
        AND (:deporte  IS NULL OR LOWER(c.deporte)     = LOWER(:deporte))
    """)
    List<Cancha> buscarFiltrado(
            @Param("distrito") String distrito,
            @Param("deporte")  String deporte
    );

    Long countByPropietarioId(Long id);

    @Query("SELECT COUNT(c) FROM Cancha c WHERE c.estado IN ('ACTIVA','DESTACADA') AND c.activa = true")
    Long countByActivaTrue();

    @Query("SELECT COUNT(c) FROM Cancha c WHERE c.estado IN ('PENDIENTE','INACTIVA') OR c.activa = false")
    Long countByActivaFalse();

    @Query("SELECT COUNT(c) FROM Cancha c WHERE c.propietario.id = :id AND c.estado IN ('ACTIVA','DESTACADA') AND c.activa = true")
    Long countByPropietarioIdAndActivaTrue(@Param("id") Long id);

    @Query("SELECT COUNT(c) FROM Cancha c WHERE c.propietario.id = :id AND (c.estado IN ('PENDIENTE','INACTIVA') OR c.activa = false)")
    Long countByPropietarioIdAndActivaFalse(@Param("id") Long id);

    @Query("SELECT COUNT(c) FROM Cancha c WHERE c.estado = 'PENDIENTE'")
    Long countByEstadoPendiente();

    @Query("SELECT COUNT(c) FROM Cancha c WHERE c.estado = 'DESTACADA'")
    Long countByEstadoDestacada();

    /** Incrementa en 1 el contador totalReservas cuando se crea una reserva */
    @Modifying
    @Transactional
    @Query("UPDATE Cancha c SET c.totalReservas = c.totalReservas + 1 WHERE c.id = :id")
    void incrementarTotalReservas(@Param("id") Long id);

    /** Decrementa en 1 cuando se cancela una reserva (mínimo 0) */
    @Modifying
    @Transactional
    @Query("UPDATE Cancha c SET c.totalReservas = GREATEST(c.totalReservas - 1, 0) WHERE c.id = :id")
    void decrementarTotalReservas(@Param("id") Long id);
}