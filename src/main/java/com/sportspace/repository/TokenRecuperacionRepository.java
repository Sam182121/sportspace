package com.sportspace.repository;

import com.sportspace.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    // Buscar token por su valor UUID
    Optional<TokenRecuperacion> findByToken(String token);

    // Eliminar TODOS los tokens de un email

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenRecuperacion t WHERE t.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenRecuperacion t WHERE t.email = :email AND t.tipo = :tipo")
    void deleteByEmailAndTipo(@Param("email") String email, @Param("tipo") String tipo);

    // Limpieza programada borrar tokens expirados
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenRecuperacion t WHERE t.expiracion < :ahora")
    void deleteByExpiracionBefore(@Param("ahora") LocalDateTime ahora);
}