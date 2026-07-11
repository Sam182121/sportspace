package com.sportspace.controller;

import com.sportspace.dto.request.PropietarioEstadoRequest;
import com.sportspace.dto.response.PropietarioEstadisticasResponse;
import com.sportspace.dto.response.PropietarioResponse;
import com.sportspace.entity.Usuario;
import com.sportspace.repository.UsuarioRepository;
import com.sportspace.service.AdminPropietarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/propietarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPropietarioController {

    private final AdminPropietarioService propietarioService;
    private final UsuarioRepository       usuarioRepository;

    // DEVUELVE LISTA DE TODOS LOS PROPIETARIOS REGISTRADOS
    @GetMapping
    public ResponseEntity<List<PropietarioResponse>> listar() {
        return ResponseEntity.ok(propietarioService.listarTodos());
    }

    /* BUSCA Y DEVUELE LOS DATOS DE UN UNICO PROPIETARIO */
    @GetMapping("/{id}")
    public ResponseEntity<PropietarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(propietarioService.obtenerPorId(id));
    }

    /* DEVUELVE INFORMACION ESTADISTICA DE UN PROPIETARIO */
    @GetMapping("/{id}/estadisticas")
    public ResponseEntity<PropietarioEstadisticasResponse> estadisticas(@PathVariable Long id) {
        return ResponseEntity.ok(propietarioService.getEstadisticas(id));
    }

    /* USA PATCH PORQUE SOLO ACTUALIZA UNA PARTE DE LA ENTIDAD (ESTADO), NO TODO EL REGISTRO*/
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PropietarioResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody PropietarioEstadoRequest req) {
        // AGARRA EL JSON DE LA PETICION LO CONVIERTE EN OBJETOV JAVA (PROPIETARIOESTADOREQUEST)
        return ResponseEntity.ok(propietarioService.cambiarEstado(id, req));
    }

    /* BORRA PROPIETARIO DE LA BD, NO PUEDE BORRAR CUANDO ESTA PENDIENTE */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        Usuario admin = usuarioRepository.findByEmail(ud.getUsername()).orElse(null);
        boolean forzar = "true".equalsIgnoreCase(body.get("forzar"));
        String tipoAccion = propietarioService.eliminar(id, body.get("motivo"), body.get("comentario"), admin, forzar);
        String mensaje = switch (tipoAccion) {
            case "ANONIMIZADO" -> "Cuenta anonimizada correctamente (tenía historial, se conservó por integridad de datos).";
            case "ELIMINADO_TOTAL_FORZADO" -> "Propietario y todo su historial relacionado (canchas, reservas, pagos) fueron eliminados por completo.";
            default -> "Propietario eliminado correctamente";
        };
        return ResponseEntity.ok(Map.of("mensaje", mensaje, "tipoAccion", tipoAccion));
    }
}