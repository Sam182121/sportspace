package com.sportspace.repository;

import com.sportspace.entity.FechaBloqueada;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FechaBloqueadaRepository extends JpaRepository<FechaBloqueada, Long> {
    List<FechaBloqueada> findByCanchaIdOrderByFechaAsc(Long canchaId);
    void deleteByCanchaId(Long canchaId);
}