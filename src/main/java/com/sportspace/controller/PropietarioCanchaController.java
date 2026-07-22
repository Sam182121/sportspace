package com.sportspace.controller;

import com.sportspace.entity.Cancha;
import com.sportspace.entity.EstadoReserva;
import com.sportspace.entity.Pago;
import com.sportspace.entity.Reserva;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/propietario/canchas")
@PreAuthorize("hasRole('PROPIETARIO')")
@RequiredArgsConstructor
public class PropietarioCanchaController {

    private final CanchaRepository         canchaRepo;
    private final ReservaRepository        reservaRepo;
    private final PagoRepository           pagoRepo;
    private final HorarioSlotRepository    slotRepo;
    private final FechaBloqueadaRepository fechaRepo;
    private final UsuarioRepository        usuarioRepo;

    // GET /api/propietario/canchas
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(
            @AuthenticationPrincipal UserDetails ud) {
        Usuario prop = getPropietario(ud);
        return ResponseEntity.ok(
                canchaRepo.findByPropietarioId(prop.getId())
                        .stream().map(this::toMap).toList());
    }

    // ── GET /api/propietario/canchas/{id} ────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detalle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(toMap(validarPropietario(id, ud)));
    }

    // ── POST /api/propietario/canchas ────────────────────────────
    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails ud) {
        Usuario prop = getPropietario(ud);
        Cancha c = new Cancha();
        c.setPropietario(prop);
        poblar(c, body);
        c.setEstado("PENDIENTE");
        c.setActiva(false);
        c.setTotalReservas(0);
        c.setFotos(new ArrayList<>());
        if (c.getNombre() == null || c.getDeporte() == null || c.getPrecioHora() == null)
            throw new BadRequestException("nombre, deporte y precioPorHora son obligatorios");
        agregarFotos(c, body);
        canchaRepo.save(c);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Cancha registrada. Espera la aprobación del administrador.",
                "id", c.getId(), "estado", "PENDIENTE"));
    }

    // PUT /api/propietario/canchas/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizar(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails ud) {
        Cancha c = validarPropietario(id, ud);
        if ("INACTIVA".equals(c.getEstado()))
            throw new BadRequestException(
                    "Tu cancha fue desactivada por el administrador. No puedes editarla.");

        if ("PENDIENTE".equals(c.getEstado()))
            throw new BadRequestException(
                    "Esta cancha aún no ha sido aprobada por el administrador. " +
                            "No puedes editarla hasta que sea aprobada.");

        if (c.getUltimaEdicion() != null) {
            java.time.LocalDateTime disponibleDesde = c.getUltimaEdicion().plusDays(30);
            if (disponibleDesde.isAfter(java.time.LocalDateTime.now())) {
                throw new BadRequestException(
                        "Ya usaste tu edición de este mes para esta cancha. " +
                                "Podrás editarla nuevamente a partir del " +
                                disponibleDesde.toLocalDate() +
                                ". Esto evita cambios frecuentes que puedan confundir a tus clientes.");
            }
        }

        poblar(c, body);
        agregarFotos(c, body);
        c.setUltimaEdicion(java.time.LocalDateTime.now());
        canchaRepo.save(c);
        return ResponseEntity.ok(Map.of("mensaje", "Cancha actualizada correctamente"));
    }

    // PATCH /{id}/publicar
    @PatchMapping("/{id}/publicar")
    public ResponseEntity<Map<String, Object>> publicar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        Cancha c = validarPropietario(id, ud);
        if ("PENDIENTE".equals(c.getEstado()))
            throw new BadRequestException("Espera la aprobación del administrador.");
        if ("INACTIVA".equals(c.getEstado()))
            throw new BadRequestException("Tu cancha fue desactivada por el administrador.");
        c.setActiva(true);
        canchaRepo.save(c);
        return ResponseEntity.ok(Map.of("mensaje", "Cancha publicada. Los clientes ya pueden encontrarla."));
    }

    //  PATCH /{id}/despublicar
    @PatchMapping("/{id}/despublicar")
    public ResponseEntity<Map<String, Object>> despublicar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        Cancha c = validarPropietario(id, ud);
        if ("INACTIVA".equals(c.getEstado()))
            throw new BadRequestException("Tu cancha está desactivada por el administrador.");
        c.setActiva(false);
        canchaRepo.save(c);
        return ResponseEntity.ok(Map.of("mensaje", "Cancha ocultada de la búsqueda de clientes."));
    }

    // ── DELETE /api/propietario/canchas/{id} ─────────────────────
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        Cancha c = validarPropietario(id, ud);

        // No eliminar si tiene reservas activas
        List<Reserva> reservas = reservaRepo.findByCanchaIdOrderByFechaDescHoraInicioDesc(c.getId());
        boolean tieneActivas = reservas.stream()
                .anyMatch(r -> r.getEstado() == EstadoReserva.PENDIENTE
                        || r.getEstado() == EstadoReserva.CONFIRMADA);
        if (tieneActivas)
            throw new BadRequestException(
                    "No puedes eliminar esta cancha porque tiene reservas pendientes o confirmadas.");

        // 1. Eliminar pagos de cada reserva de esta cancha
        for (Reserva r : reservas) {
            pagoRepo.findByReservaId(r.getId()).ifPresent(pagoRepo::delete);
        }

        // 2. Eliminar reservas
        reservaRepo.deleteAll(reservas);

        // 3. Eliminar horarios
        slotRepo.deleteByCanchaId(c.getId());

        // 4. Eliminar fechas bloqueadas
        fechaRepo.deleteAll(fechaRepo.findByCanchaIdOrderByFechaAsc(c.getId()));

        // 5. Limpiar fotos (ElementCollection)
        c.getFotos().clear();
        canchaRepo.save(c);

        // 6. Eliminar la cancha
        canchaRepo.delete(c);

        return ResponseEntity.ok(Map.of("mensaje", "Cancha eliminada correctamente."));
    }

    // DELETE /{id}/fotos/{index}
    @DeleteMapping("/{id}/fotos/{index}")
    public ResponseEntity<Map<String, Object>> eliminarFoto(
            @PathVariable Long id,
            @PathVariable int index,
            @AuthenticationPrincipal UserDetails ud) {
        Cancha c = validarPropietario(id, ud);
        if ("INACTIVA".equals(c.getEstado()))
            throw new BadRequestException("No puedes modificar una cancha desactivada.");
        if (index >= 0 && index < c.getFotos().size()) {
            c.getFotos().remove(index);
            canchaRepo.save(c);
        }
        return ResponseEntity.ok(Map.of("mensaje", "Foto eliminada"));
    }

    // Helpers
    private void poblar(Cancha c, Map<String, Object> body) {
        if (body.containsKey("nombre"))         c.setNombre(str(body, "nombre"));
        if (body.containsKey("deporte"))        c.setDeporte(str(body, "deporte"));
        if (body.containsKey("tipoSuperficie")) c.setTipoSuperficie(str(body, "tipoSuperficie"));
        if (body.containsKey("descripcion"))    c.setDescripcion(str(body, "descripcion"));
        if (body.containsKey("direccion"))      c.setDireccion(str(body, "direccion"));
        if (body.containsKey("departamento"))   c.setDepartamento(str(body, "departamento"));
        if (body.containsKey("provincia"))      c.setProvincia(str(body, "provincia"));
        if (body.containsKey("distrito"))       c.setDistrito(str(body, "distrito"));
        if (body.containsKey("precioPorHora"))  c.setPrecioHora(bigDec(body, "precioPorHora"));
        if (body.containsKey("capacidad"))      c.setCapacidad(intVal(body, "capacidad"));
    }

    private void agregarFotos(Cancha c, Map<String, Object> body) {
        if (!body.containsKey("fotos")) return;
        Object fotosObj = body.get("fotos");
        if (fotosObj instanceof List<?> fotosList) {
            if (c.getFotos() == null) c.setFotos(new ArrayList<>());
            c.getFotos().clear();
            fotosList.stream().limit(3).map(Object::toString).forEach(url -> c.getFotos().add(url));
        }
    }

    private Map<String, Object> toMap(Cancha c) {
        long reservasMes = reservaRepo
                .findByCanchaIdOrderByFechaDescHoraInicioDesc(c.getId())
                .stream()
                .filter(r -> r.getFecha() != null
                        && r.getFecha().getYear()        == LocalDate.now().getYear()
                        && r.getFecha().getMonthValue()  == LocalDate.now().getMonthValue()
                        && r.getEstado() != EstadoReserva.CANCELADA)
                .count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              c.getId());
        m.put("nombre",          c.getNombre());
        m.put("deporte",         c.getDeporte());
        m.put("tipoSuperficie",  c.getTipoSuperficie());
        m.put("descripcion",     c.getDescripcion());
        m.put("precioPorHora",   c.getPrecioHora());
        m.put("capacidad",       c.getCapacidad());
        m.put("departamento",    c.getDepartamento());
        m.put("provincia",       c.getProvincia());
        m.put("distrito",        c.getDistrito());
        m.put("direccion",       c.getDireccion());
        m.put("estado",          c.getEstado());
        m.put("activa",          c.getActiva());
        m.put("publicada",       Boolean.TRUE.equals(c.getActiva())
                && ("ACTIVA".equals(c.getEstado()) || "DESTACADA".equals(c.getEstado())));
        boolean aprobada = "ACTIVA".equals(c.getEstado()) || "DESTACADA".equals(c.getEstado());
        java.time.LocalDateTime disponibleDesde = c.getUltimaEdicion() != null
                ? c.getUltimaEdicion().plusDays(30) : null;
        boolean enEsperaPorEdicionMensual = disponibleDesde != null
                && disponibleDesde.isAfter(java.time.LocalDateTime.now());

        m.put("editable",        !"INACTIVA".equals(c.getEstado()) && aprobada && !enEsperaPorEdicionMensual);
        m.put("pendienteAprobacion", "PENDIENTE".equals(c.getEstado()));
        m.put("edicionDisponibleDesde", enEsperaPorEdicionMensual ? disponibleDesde.toLocalDate() : null);
        m.put("fechaCreacion",   c.getCreatedAt());
        m.put("publicable",      ("ACTIVA".equals(c.getEstado()) || "DESTACADA".equals(c.getEstado()))
                && !Boolean.TRUE.equals(c.getActiva()));
        m.put("fotos",           c.getFotos() != null ? c.getFotos() : List.of());
        m.put("totalReservasMes", reservasMes);
        m.put("ocupacion",       calcOcupacion(c.getId()));
        return m;
    }

    private int calcOcupacion(Long canchaId) {
        long total = reservaRepo.findByCanchaIdOrderByFechaDescHoraInicioDesc(canchaId)
                .stream()
                .filter(r -> r.getFecha() != null
                        && r.getEstado() == EstadoReserva.CONFIRMADA
                        && r.getFecha().getMonthValue() == LocalDate.now().getMonthValue()
                        && r.getFecha().getYear()       == LocalDate.now().getYear())
                .count();
        return (int) Math.min(total == 0 ? 0 : (total * 100) / 30, 100);
    }

    private Cancha validarPropietario(Long canchaId, UserDetails ud) {
        Cancha c = canchaRepo.findById(canchaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        if (!c.getPropietario().getEmail().equals(ud.getUsername()))
            throw new BadRequestException("No tienes permisos sobre esta cancha");
        return c;
    }

    private Usuario getPropietario(UserDetails ud) {
        return usuarioRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private String     str(Map<String,Object> m, String k) { Object v = m.get(k); return v != null ? v.toString().trim() : null; }
    private BigDecimal bigDec(Map<String,Object> m, String k) { Object v = m.get(k); return v != null && !v.toString().isBlank() ? new BigDecimal(v.toString()) : null; }
    private Integer    intVal(Map<String,Object> m, String k) { Object v = m.get(k); return v != null && !v.toString().isBlank() ? Integer.parseInt(v.toString()) : null; }
}