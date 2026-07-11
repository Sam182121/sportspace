package com.sportspace.controller;

import com.sportspace.dto.request.UsuarioEditRequest;
import com.sportspace.dto.response.UsuarioResponse;
import com.sportspace.dto.response.UsuarioStatsResponse;
import com.sportspace.entity.Usuario;
import com.sportspace.repository.UsuarioRepository;
import com.sportspace.service.AdminUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    private final AdminUsuarioService service;
    private final UsuarioRepository   usuarioRepository;

    // LLAMA A TODOS LOS USUARIOS
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar() {
        List<UsuarioResponse> lista = service.listarTodos();
        return ResponseEntity.ok(Map.of("usuarios", lista));
    }

    // DEVULVE METRICAS GENERALES, TOTAL USUARIOS,USAUIROS NUEVO ESTE MES
    @GetMapping("/stats")
    public ResponseEntity<UsuarioStatsResponse> stats() {
        return ResponseEntity.ok(service.getStats());
    }

    // OBTENER USUARIOS POR ID
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    // EDITAR USUARIO
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> editar(
            @PathVariable Long id,
            @RequestBody UsuarioEditRequest req) {
        return ResponseEntity.ok(service.editar(id, req));
    }

    // CAMBIA ESTADO DEL USUARIO ACTIVO / INACTIVO (bloquear pide motivo + comentario)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails ud) {
        Boolean activo     = (Boolean) body.get("activo");
        String  motivo     = (String)  body.get("motivo");
        String  comentario = (String)  body.get("comentario");
        Usuario admin = usuarioRepository.findByEmail(ud.getUsername()).orElse(null);
        return ResponseEntity.ok(service.cambiarEstado(id, activo, motivo, comentario, admin));
    }

    // ACTIVA/DESACTIVA LOS ROLES CLIENTE Y/O PROPIETARIO (doble rol)
    @PatchMapping("/{id}/roles")
    public ResponseEntity<UsuarioResponse> cambiarRoles(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(service.cambiarRoles(
                id, body.get("esCliente"), body.get("esPropietario")));
    }

    // ELIMINA USUARIO
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        Usuario admin = usuarioRepository.findByEmail(ud.getUsername()).orElse(null);
        boolean forzar = "true".equalsIgnoreCase(body.get("forzar"));
        String tipoAccion = service.eliminar(id, body.get("motivo"), body.get("comentario"), admin, forzar);
        String mensaje = switch (tipoAccion) {
            case "ANONIMIZADO" -> "Cuenta anonimizada correctamente (tenía historial, se conservó por integridad de datos).";
            case "ELIMINADO_TOTAL_FORZADO" -> "Usuario y todo su historial relacionado fueron eliminados por completo.";
            default -> "Usuario eliminado correctamente.";
        };
        return ResponseEntity.ok(Map.of("mensaje", mensaje, "tipoAccion", tipoAccion));
    }
}