package com.sportspace.repository;

import com.sportspace.entity.CodigoPreRegistro;
import com.sportspace.entity.TipoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodigoPreRegistroRepository extends JpaRepository<CodigoPreRegistro, Long> {

    // Busca el código más reciente para un email y tipo dado
    Optional<CodigoPreRegistro> findTopByEmailAndTipoOrderByExpiracionDesc(
            String email, TipoVerificacion tipo);

    // Elimina códigos anteriores del mismo email y tipo antes de generar uno nuevo
    @Modifying
    @Query("DELETE FROM CodigoPreRegistro c WHERE c.email = :email AND c.tipo = :tipo")
    void deleteByEmailAndTipo(@Param("email") String email,
                              @Param("tipo") TipoVerificacion tipo);

    // Elimina todos los códigos de un email al completar el registro
    @Modifying
    @Query("DELETE FROM CodigoPreRegistro c WHERE c.email = :email")
    void deleteAllByEmail(@Param("email") String email);
}