package com.sportspace.controller;

import com.sportspace.entity.MetodoPagoPropietario;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.CanchaRepository;
import com.sportspace.repository.MetodoPagoPropietarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/canchas")
@RequiredArgsConstructor
public class MetodoPagoPublicoController {

    private final CanchaRepository                canchaRepo;
    private final MetodoPagoPropietarioRepository metodoPagoRepo;


    @GetMapping("/{canchaId}/metodos-pago")
    public ResponseEntity<List<Map<String, Object>>> metodosPago(
            @PathVariable Long canchaId) {

        var cancha = canchaRepo.findById(canchaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));

        Long propId = cancha.getPropietario().getId();

        List<Map<String, Object>> result = metodoPagoRepo
                .findByPropietarioIdAndActivoTrue(propId)
                .stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("tipo", m.getTipo());
                    if ("TRANSFERENCIA".equals(m.getTipo())) {
                        map.put("banco",         m.getBanco());
                        map.put("numeroCuenta",  m.getNumeroCuenta());
                        map.put("cci",           m.getCci());
                        map.put("titular",       m.getTitularCuenta());
                    } else {
                        map.put("numeroTelefono", m.getNumeroTelefono());
                        map.put("titular",        m.getNombreTitular());
                    }
                    return map;
                }).toList();

        return ResponseEntity.ok(result);
    }
}