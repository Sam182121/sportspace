package com.sportspace.repository;

import com.sportspace.entity.EliminacionUsuarioLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EliminacionUsuarioLogRepository extends JpaRepository<EliminacionUsuarioLog, Long> {
    List<EliminacionUsuarioLog> findAllByOrderByCreatedAtDesc();
}