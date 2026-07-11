package com.sportspace.repository;

import com.sportspace.entity.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepartamentoRepository extends JpaRepository<Departamento, String> {
    // Devuelve todos los departamentos ordenados alfabéticamente A-Z
    List<Departamento> findAllByOrderByNameAsc();
}