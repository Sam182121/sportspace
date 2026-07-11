package com.sportspace.repository;

import com.sportspace.entity.Nacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NacionalidadRepository extends JpaRepository<Nacionalidad, String> {
    // Devuelve todas las nacionalidades ordenadas por nombre de país A-Z
    List<Nacionalidad> findAllByOrderByPaisAsc();
}