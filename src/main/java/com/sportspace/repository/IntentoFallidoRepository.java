package com.sportspace.repository;

import com.sportspace.entity.IntentoFallido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IntentoFallidoRepository extends JpaRepository<IntentoFallido, Long> {

    Optional<IntentoFallido> findByIpAndCorreoIntentado(String ip, String correoIntentado);

    Optional<IntentoFallido> findByIp(String ip);

    List<IntentoFallido> findAllByOrderByUltimoIntentoDesc();

    long countByBloqueadaTrue();

    @Query("SELECT COUNT(i) FROM IntentoFallido i WHERE i.ultimoIntento >= :desde")
    long countByUltimoIntentoAfter(@Param("desde") LocalDateTime desde);
}