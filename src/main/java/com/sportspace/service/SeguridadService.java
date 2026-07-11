package com.sportspace.service;

import com.sportspace.entity.IntentoFallido;
import com.sportspace.entity.Rol;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.IntentoFallidoRepository;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeguridadService {

    private final IntentoFallidoRepository intentoRepo;
    private final UsuarioRepository        usuarioRepo;

    /**
     * Mapa en memoria: token → SesionInfo.
     * Se puebla cuando AuthService genera un token exitoso.
     * Se limpia cuando expira o se revoca desde el panel.
     */
    private final Map<String, SesionInfo> sesionesActivas = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────
    //  INNER RECORD – datos de una sesión activa
    // ─────────────────────────────────────────────────────────────────

    public record SesionInfo(
            String sessionId,
            Long   usuarioId,
            String nombres,
            String apellidos,
            String email,
            String rol,
            String ip,
            LocalDateTime inicioDeSesion
    ) {}

    // ─────────────────────────────────────────────────────────────────
    //  REGISTRAR LOGIN FALLIDO
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public void registrarIntentoFallido(String ip, String correo) {
        try {
            Optional<IntentoFallido> existing =
                    intentoRepo.findByIpAndCorreoIntentado(ip, correo);

            if (existing.isPresent()) {
                IntentoFallido intento = existing.get();
                intento.setCantidad(intento.getCantidad() + 1);
                intento.setUltimoIntento(LocalDateTime.now());
                intentoRepo.save(intento);
            } else {
                IntentoFallido nuevo = IntentoFallido.builder()
                        .ip(ip)
                        .correoIntentado(correo)
                        .cantidad(1)
                        .bloqueada(false)
                        .ultimoIntento(LocalDateTime.now())
                        .build();
                intentoRepo.save(nuevo);
            }
        } catch (Exception e) {
            log.warn("No se pudo registrar intento fallido para IP={}: {}", ip, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  REGISTRAR SESIÓN ACTIVA (llamado desde AuthService al hacer login)
    // ─────────────────────────────────────────────────────────────────

    public void registrarSesion(String token, Long usuarioId, String nombres,
                                String apellidos, String email, String rol, String ip) {
        String sessionId = UUID.randomUUID().toString();
        SesionInfo info = new SesionInfo(
                sessionId, usuarioId, nombres, apellidos,
                email, rol, ip, LocalDateTime.now()
        );
        // Guardamos por sessionId Y por token para poder revocar por cualquiera
        sesionesActivas.put(token, info);
        log.debug("Sesión registrada para {} desde {}", email, ip);
    }

    // ─────────────────────────────────────────────────────────────────
    //  STATS (para las tarjetas del panel)
    // ─────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        long intentosFallidos = intentoRepo.count();
        long ipsBloqueadas    = intentoRepo.countByBloqueadaTrue();
        long sesionesActivas  = this.sesionesActivas.size();
        long usuariosBloqueados = usuarioRepo.countByBloqueadoPorSeguridadTrue();

        LocalDateTime hace24h = LocalDateTime.now().minusHours(24);
        long intentosUltimas24h = intentoRepo.countByUltimoIntentoAfter(hace24h);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("intentosFallidos",   intentosFallidos);
        stats.put("ipsBloqueadas",       ipsBloqueadas);
        stats.put("sesionesActivas",     sesionesActivas);
        stats.put("usuariosBloqueados",  usuariosBloqueados);
        stats.put("intentosUltimas24h",  intentosUltimas24h);
        return stats;
    }

    // ─────────────────────────────────────────────────────────────────
    //  LISTAR INTENTOS FALLIDOS
    // ─────────────────────────────────────────────────────────────────

    public List<Map<String, Object>> listarIntentos() {
        return intentoRepo.findAllByOrderByUltimoIntentoDesc()
                .stream()
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",               i.getId());
                    m.put("ip",               i.getIp());
                    m.put("correoIntentado",  i.getCorreoIntentado());
                    m.put("cantidad",         i.getCantidad());
                    m.put("ultimoIntento",    i.getUltimoIntento());
                    m.put("bloqueada",        i.getBloqueada());
                    return m;
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    //  LISTAR SESIONES ACTIVAS
    // ─────────────────────────────────────────────────────────────────

    public List<Map<String, Object>> listarSesiones(String tokenActual) {
        return sesionesActivas.entrySet().stream()
                .map(entry -> {
                    SesionInfo s = entry.getValue();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sessionId",      s.sessionId());
                    m.put("usuarioId",      s.usuarioId());
                    m.put("nombres",        s.nombres());
                    m.put("apellidos",      s.apellidos());
                    m.put("email",          s.email());
                    m.put("rol",            s.rol());
                    m.put("ip",             s.ip());
                    m.put("inicioDeSesion", s.inicioDeSesion());
                    // Marca la sesión del admin que consulta
                    m.put("esSesionActual", entry.getKey().equals(tokenActual));
                    return m;
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    //  BLOQUEAR IP
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public void bloquearIP(String ip) {
        // Marcamos todas las entradas de esa IP como bloqueadas
        List<IntentoFallido> lista = intentoRepo.findAll()
                .stream()
                .filter(i -> ip.equals(i.getIp()))
                .toList();

        if (lista.isEmpty()) {
            // La IP no tiene intentos previos: la creamos bloqueada
            IntentoFallido nuevo = IntentoFallido.builder()
                    .ip(ip)
                    .correoIntentado("")
                    .cantidad(0)
                    .bloqueada(true)
                    .ultimoIntento(LocalDateTime.now())
                    .build();
            intentoRepo.save(nuevo);
        } else {
            lista.forEach(i -> i.setBloqueada(true));
            intentoRepo.saveAll(lista);
        }

        // También cerramos todas las sesiones activas de esa IP
        sesionesActivas.entrySet().removeIf(e -> ip.equals(e.getValue().ip()));
    }

    // ─────────────────────────────────────────────────────────────────
    //  DESBLOQUEAR IP
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public void desbloquearIP(String ip) {
        intentoRepo.findAll().stream()
                .filter(i -> ip.equals(i.getIp()))
                .forEach(i -> i.setBloqueada(false));
        intentoRepo.saveAll(
                intentoRepo.findAll().stream()
                        .filter(i -> ip.equals(i.getIp()))
                        .toList()
        );
    }

    // ─────────────────────────────────────────────────────────────────
    //  ELIMINAR INTENTO (ignorar / limpiar)
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public void eliminarIntento(Long id) {
        if (!intentoRepo.existsById(id)) {
            throw new ResourceNotFoundException("Intento no encontrado con id: " + id);
        }
        intentoRepo.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────────
    //  CERRAR SESIÓN INDIVIDUAL
    // ─────────────────────────────────────────────────────────────────

    public void cerrarSesion(String sessionId) {
        boolean removed = sesionesActivas.values().removeIf(
                s -> sessionId.equals(s.sessionId())
        );
        // También intentar por sessionId == token directamente
        sesionesActivas.entrySet().removeIf(e -> sessionId.equals(e.getKey()));
        if (!removed) {
            log.warn("Sesión {} no encontrada al intentar cerrarla", sessionId);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CERRAR TODAS LAS SESIONES (excepto la del admin que ejecuta)
    // ─────────────────────────────────────────────────────────────────

    public void cerrarTodasLasSesiones(String tokenActual) {
        sesionesActivas.entrySet()
                .removeIf(e -> !e.getKey().equals(tokenActual));
    }

    // ─────────────────────────────────────────────────────────────────
    //  VERIFICAR SI UNA IP ESTÁ BLOQUEADA (usado en JwtAuthFilter/AuthService)
    // ─────────────────────────────────────────────────────────────────

    public boolean isIpBloqueada(String ip) {
        return intentoRepo.findAll().stream()
                .anyMatch(i -> ip.equals(i.getIp()) && Boolean.TRUE.equals(i.getBloqueada()));
    }

    // ─────────────────────────────────────────────────────────────────
    //  VERIFICAR SI UN TOKEN YA ESTÁ REGISTRADO COMO SESIÓN
    // ─────────────────────────────────────────────────────────────────

    public boolean isSesionRegistrada(String token) {
        return sesionesActivas.containsKey(token);
    }

    // ─────────────────────────────────────────────────────────────────
    //  REMOVER SESIÓN POR TOKEN (llamado al expirar)
    // ─────────────────────────────────────────────────────────────────

    public void removerSesionPorToken(String token) {
        sesionesActivas.remove(token);
    }
}