package com.sportspace.repository;

import com.sportspace.entity.CodigoVerificacion;
import com.sportspace.entity.TipoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    // Busca el código más reciente de un usuario para un tipo
    Optional<CodigoVerificacion> findTopByUsuarioIdAndTipoOrderByExpiracionDesc(
            Long usuarioId, TipoVerificacion tipo);

    // Elimina códigos anteriores del mismo usuario y tipo antes de generar uno nuevo
    @Modifying
    @Query("DELETE FROM CodigoVerificacion c WHERE c.usuario.id = :usuarioId AND c.tipo = :tipo")
    void deleteByUsuarioIdAndTipo(@Param("usuarioId") Long usuarioId,
                                  @Param("tipo") TipoVerificacion tipo);

    @Modifying
    @Query("DELETE FROM CodigoVerificacion c WHERE c.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}