package com.sportspace.controller;

import com.sportspace.entity.Departamento;
import com.sportspace.entity.Distrito;
import com.sportspace.entity.Provincia;
import com.sportspace.repository.DepartamentoRepository;
import com.sportspace.repository.DistritoRepository;
import com.sportspace.repository.ProvinciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ubigeo")
@RequiredArgsConstructor
public class UbigeoController {

    private final DepartamentoRepository departamentoRepository;
    private final ProvinciaRepository    provinciaRepository;
    private final DistritoRepository     distritoRepository;


    @GetMapping("/departamentos")
    public ResponseEntity<List<Departamento>> departamentos() {
        return ResponseEntity.ok(departamentoRepository.findAllByOrderByNameAsc());
    }

    @GetMapping("/provincias/{departamentoId}")
    public ResponseEntity<List<Provincia>> provincias(@PathVariable String departamentoId) {
        return ResponseEntity.ok(
                provinciaRepository.findByDepartmentIdOrderByNameAsc(departamentoId));
    }


    @GetMapping("/distritos/{provinciaId}")
    public ResponseEntity<List<Distrito>> distritos(@PathVariable String provinciaId) {
        return ResponseEntity.ok(
                distritoRepository.findByProvinceIdOrderByNameAsc(provinciaId));
    }
}