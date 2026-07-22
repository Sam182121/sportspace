package com.sportspace.repository;

import com.sportspace.entity.SesionActiva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SesionActivaRepository extends JpaRepository<SesionActiva, Long> {
    Optional<SesionActiva> findByToken(String token);
    void deleteByToken(String token);
}