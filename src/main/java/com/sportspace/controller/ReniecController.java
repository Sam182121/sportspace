package com.sportspace.controller;

import com.sportspace.dto.response.ReniecResponse;
import com.sportspace.service.ReniecService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reniec")
@RequiredArgsConstructor
public class ReniecController {

    private final ReniecService reniecService;

    // GET /api/reniec/dni/{dni}
    // Consulta datos por DNI usando Factiliza
    @GetMapping("/dni/{dni}")
    public ResponseEntity<ReniecResponse> consultarDni(@PathVariable String dni) {
        return ResponseEntity.ok(reniecService.consultarPorDni(dni));
    }

    // GET /api/reniec/ce/{ce}
    // Consulta datos por Carnet de Extranjería usando Factiliza
    // Solo devuelve: nombres, apellido paterno y materno
    @GetMapping("/ce/{ce}")
    public ResponseEntity<ReniecResponse> consultarCe(@PathVariable String ce) {
        return ResponseEntity.ok(reniecService.consultarPorCe(ce));
    }
}