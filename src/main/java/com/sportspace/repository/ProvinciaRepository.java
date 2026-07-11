package com.sportspace.repository;

import com.sportspace.entity.Provincia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProvinciaRepository extends JpaRepository<Provincia, String> {
    // Filtra provincias por departamento, ordenadas A-Z
    List<Provincia> findByDepartmentIdOrderByNameAsc(String departmentId);
}