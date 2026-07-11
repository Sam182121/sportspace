package com.sportspace.repository;

import com.sportspace.entity.Distrito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DistritoRepository extends JpaRepository<Distrito, String> {
    // Filtra distritos por provincia, ordenados A-Z
    List<Distrito> findByProvinceIdOrderByNameAsc(String provinceId);
}