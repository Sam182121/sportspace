package com.sportspace.controller;

import com.sportspace.entity.Nacionalidad;
import com.sportspace.repository.NacionalidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Endpoint público para cargar el combobox de nacionalidades en el registro.
// Solo se muestra cuando la API no devuelve la nacionalidad (usuarios con C.E.)
@RestController
@RequestMapping("/api/nacionalidades")
@RequiredArgsConstructor
public class NacionalidadController {

    private final NacionalidadRepository nacionalidadRepository;

    // GET /api/nacionalidades
    // Devuelve la lista completa de países ordenada A-Z
    @GetMapping
    public ResponseEntity<List<Nacionalidad>> listar() {
        return ResponseEntity.ok(nacionalidadRepository.findAllByOrderByPaisAsc());
    }
}