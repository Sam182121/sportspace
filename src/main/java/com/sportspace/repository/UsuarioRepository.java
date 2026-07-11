package com.sportspace.repository;

import com.sportspace.entity.Rol;
import com.sportspace.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    // Buscar por número de documento unificado (DNI o CE)
    Optional<Usuario> findByNumeroDocumento(String numeroDocumento);

    boolean existsByEmail(String email);

    // Verificar si el número de documento ya está registrado (DNI o C.E.)
    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByTelefono(String telefono);

    Long countByRol(Rol rol);
    Long countByActivoTrue();
    Long countByActivoFalse();

    List<Usuario> findByRol(Rol rol);

    Long countByCreatedAtBetween(LocalDateTime inicio, LocalDateTime fin);

    Long countByBloqueadoPorSeguridadTrue();
}