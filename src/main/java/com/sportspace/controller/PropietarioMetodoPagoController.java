package com.sportspace.controller;

import com.sportspace.entity.MetodoPagoPropietario;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.MetodoPagoPropietarioRepository;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/propietario/metodos-pago")
@PreAuthorize("hasRole('PROPIETARIO')")
@RequiredArgsConstructor
public class PropietarioMetodoPagoController {

    private final MetodoPagoPropietarioRepository metodoPagoRepo;
    private final UsuarioRepository               usuarioRepo;

    private static final List<String> TIPOS_VALIDOS = List.of("TRANSFERENCIA", "YAPE", "PLIN");

    // GET /api/propietario/metodos-pago
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(
            @AuthenticationPrincipal UserDetails ud) {
        Long propId = getPropietario(ud).getId();
        List<Map<String, Object>> result = metodoPagoRepo
                .findByPropietarioId(propId)
                .stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(result);
    }

    // POST /api/propietario/metodos-pago
    // Crea o actualiza el método de pago de un tipo (upsert)
    @PostMapping
    public ResponseEntity<Map<String, Object>> guardar(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails ud) {
        Usuario prop = getPropietario(ud);
        String tipo  = str(body, "tipo");

        if (tipo == null || !TIPOS_VALIDOS.contains(tipo.toUpperCase()))
            throw new BadRequestException("Tipo inválido. Use: TRANSFERENCIA, YAPE o PLIN");

        tipo = tipo.toUpperCase();
        validarCampos(tipo, body);

        // Upsert: si ya existe el tipo, actualizar; si no, crear
        MetodoPagoPropietario metodo = metodoPagoRepo
                .findByPropietarioIdAndTipo(prop.getId(), tipo)
                .orElse(new MetodoPagoPropietario());

        metodo.setPropietario(prop);
        metodo.setTipo(tipo);
        metodo.setActivo(true); // al crear/guardar se activa automáticamente

        // Campos TRANSFERENCIA
        if ("TRANSFERENCIA".equals(tipo)) {
            metodo.setBanco(str(body, "banco"));
            metodo.setNumeroCuenta(str(body, "numeroCuenta"));
            metodo.setCci(str(body, "cci"));
            metodo.setTitularCuenta(str(body, "titularCuenta"));
            metodo.setNumeroTelefono(null);
            metodo.setNombreTitular(null);
        }
        // Campos YAPE / PLIN
        else {
            metodo.setNumeroTelefono(str(body, "numeroTelefono"));
            metodo.setNombreTitular(str(body, "nombreTitular"));
            metodo.setBanco(null);
            metodo.setNumeroCuenta(null);
            metodo.setCci(null);
            metodo.setTitularCuenta(null);
        }

        metodoPagoRepo.save(metodo);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Método de pago guardado correctamente",
                "id",      metodo.getId()));
    }

    // PATCH /api/propietario/metodos-pago/{id}/toggle
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        MetodoPagoPropietario m = validarPropietario(id, ud);
        m.setActivo(!Boolean.TRUE.equals(m.getActivo()));
        metodoPagoRepo.save(m);
        String estado = Boolean.TRUE.equals(m.getActivo()) ? "activado" : "desactivado";
        return ResponseEntity.ok(Map.of(
                "mensaje", "Método de pago " + estado,
                "activo",  m.getActivo()));
    }

    // DELETE /api/propietario/metodos-pago/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        MetodoPagoPropietario m = validarPropietario(id, ud);
        metodoPagoRepo.delete(m);
        return ResponseEntity.ok(Map.of("mensaje", "Método de pago eliminado"));
    }

    // ── GET PÚBLICO: métodos activos de propietario de una cancha
    // (Para mostrar al cliente al reservar — no necesita auth de propietario)

    // Helpers
    private Map<String, Object> toMap(MetodoPagoPropietario m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",             m.getId());
        map.put("tipo",           m.getTipo());
        map.put("activo",         m.getActivo());
        map.put("banco",          m.getBanco());
        map.put("numeroCuenta",   m.getNumeroCuenta());
        map.put("cci",            m.getCci());
        map.put("titularCuenta",  m.getTitularCuenta());
        map.put("numeroTelefono", m.getNumeroTelefono());
        map.put("nombreTitular",  m.getNombreTitular());
        return map;
    }

    private void validarCampos(String tipo, Map<String, Object> body) {
        if ("TRANSFERENCIA".equals(tipo)) {
            if (blank(body, "banco"))        throw new BadRequestException("banco es obligatorio");
            if (blank(body, "numeroCuenta")) throw new BadRequestException("numeroCuenta es obligatorio");
            if (blank(body, "cci"))          throw new BadRequestException("cci es obligatorio");
            if (blank(body, "titularCuenta"))throw new BadRequestException("titularCuenta es obligatorio");

            String numeroCuenta = str(body, "numeroCuenta");
            if (!numeroCuenta.matches("\\d{1,20}"))
                throw new BadRequestException("numeroCuenta debe contener solo dígitos (máx. 20)");

            String cci = str(body, "cci");
            if (!cci.matches("\\d{20}"))
                throw new BadRequestException("cci debe tener exactamente 20 dígitos");
        } else {
            if (blank(body, "numeroTelefono")) throw new BadRequestException("numeroTelefono es obligatorio");
            if (blank(body, "nombreTitular"))  throw new BadRequestException("nombreTitular es obligatorio");
        }
    }

    private MetodoPagoPropietario validarPropietario(Long id, UserDetails ud) {
        MetodoPagoPropietario m = metodoPagoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado"));
        if (!m.getPropietario().getEmail().equals(ud.getUsername()))
            throw new BadRequestException("No tienes permisos sobre este método de pago");
        return m;
    }

    private Usuario getPropietario(UserDetails ud) {
        return usuarioRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private String  str(Map<String,Object> m, String k) { Object v = m.get(k); return v != null && !v.toString().isBlank() ? v.toString().trim() : null; }
    private boolean blank(Map<String,Object> m, String k) { return str(m, k) == null; }
}