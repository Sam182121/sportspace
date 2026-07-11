package com.sportspace.repository;

import com.sportspace.entity.MetodoPagoPropietario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetodoPagoPropietarioRepository extends JpaRepository<MetodoPagoPropietario, Long> {

    /** Todos los métodos de un propietario (activos e inactivos) */
    List<MetodoPagoPropietario> findByPropietarioId(Long propietarioId);

    /** Solo los métodos activos — para mostrar al cliente al reservar */
    List<MetodoPagoPropietario> findByPropietarioIdAndActivoTrue(Long propietarioId);

    /** Buscar por propietario y tipo (TRANSFERENCIA, YAPE, PLIN) */
    Optional<MetodoPagoPropietario> findByPropietarioIdAndTipo(Long propietarioId, String tipo);

    void deleteByPropietarioId(Long propietarioId);
}