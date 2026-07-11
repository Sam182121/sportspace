package com.sportspace.controller;

import com.sportspace.service.PreRegistroService;
import com.sportspace.service.VerificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class VerificacionController {

    private final VerificacionService verificacionService;
    private final PreRegistroService  preRegistroService;



    @PostMapping("/pre-registro/enviar-codigo-email")
    public ResponseEntity<Map<String, String>> enviarCodigoEmailPreRegistro(
            @RequestBody Map<String, String> body) {
        preRegistroService.enviarCodigoEmail(body.get("email"));
        return ResponseEntity.ok(Map.of(
                "mensaje", "Código enviado a tu correo electrónico."
        ));
    }


    @PostMapping("/pre-registro/verificar-email")
    public ResponseEntity<Map<String, String>> verificarEmailPreRegistro(
            @RequestBody Map<String, String> body) {
        preRegistroService.verificarEmail(body.get("email"), body.get("codigo"));
        return ResponseEntity.ok(Map.of(
                "mensaje", "Correo verificado correctamente."
        ));
    }


    @PostMapping("/verificar-email")
    public ResponseEntity<Map<String, String>> verificarEmail(
            @RequestBody Map<String, String> body) {
        verificacionService.verificarEmail(body.get("email"), body.get("codigo"));
        return ResponseEntity.ok(Map.of("mensaje", "Correo verificado correctamente."));
    }

    @PostMapping("/reenviar-codigo-email")
    public ResponseEntity<Map<String, String>> reenviarCodigoEmail(
            @RequestBody Map<String, String> body) {
        verificacionService.enviarCodigoEmail(body.get("email"));
        return ResponseEntity.ok(Map.of("mensaje", "Código reenviado a tu correo electrónico."));
    }
}