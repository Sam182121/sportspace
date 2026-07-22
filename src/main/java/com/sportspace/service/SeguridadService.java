package com.sportspace.service;

import com.sportspace.entity.IntentoFallido;
import com.sportspace.entity.SesionActiva;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.IntentoFallidoRepository;
import com.sportspace.repository.SesionActivaRepository;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeguridadService {

    private final IntentoFallidoRepository intentoRepo;
    private final UsuarioRepository        usuarioRepo;
    private final SesionActivaRepository   sesionRepo;

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

    @Transactional
    public void registrarSesion(String token, Long usuarioId, String nombres,
                                String apellidos, String email, String rol, String ip) {
        if (sesionRepo.findByToken(token).isPresent()) {
            return;
        }
        SesionActiva sesion = SesionActiva.builder()
                .sessionId(UUID.randomUUID().toString())
                .token(token)
                .usuarioId(usuarioId)
                .nombres(nombres)
                .apellidos(apellidos)
                .email(email)
                .rol(rol)
                .ip(ip)
                .inicioDeSesion(LocalDateTime.now())
                .build();
        sesionRepo.save(sesion);
        log.debug("Sesión registrada para {} desde {}", email, ip);
    }

    public Map<String, Object> getStats() {
        long intentosFallidos   = intentoRepo.count();
        long ipsBloqueadas      = intentoRepo.countByBloqueadaTrue();
        long sesionesActivas    = sesionRepo.count();
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

    public long contarSesionesActivas() {
        return sesionRepo.count();
    }

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

    public List<Map<String, Object>> listarSesiones(String tokenActual) {
        return sesionRepo.findAll().stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sessionId",      s.getSessionId());
                    m.put("usuarioId",      s.getUsuarioId());
                    m.put("nombres",        s.getNombres());
                    m.put("apellidos",      s.getApellidos());
                    m.put("email",          s.getEmail());
                    m.put("rol",            s.getRol());
                    m.put("ip",             s.getIp());
                    m.put("inicioDeSesion", s.getInicioDeSesion());
                    m.put("esSesionActual", s.getToken().equals(tokenActual));
                    return m;
                })
                .toList();
    }

    @Transactional
    public void bloquearIP(String ip) {
        List<IntentoFallido> lista = intentoRepo.findAll()
                .stream()
                .filter(i -> ip.equals(i.getIp()))
                .toList();

        if (lista.isEmpty()) {
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

        List<SesionActiva> sesionesDeEsaIp = sesionRepo.findAll().stream()
                .filter(s -> ip.equals(s.getIp()))
                .toList();
        sesionRepo.deleteAll(sesionesDeEsaIp);
    }

    @Transactional
    public void desbloquearIP(String ip) {
        List<IntentoFallido> lista = intentoRepo.findAll().stream()
                .filter(i -> ip.equals(i.getIp()))
                .toList();
        lista.forEach(i -> i.setBloqueada(false));
        intentoRepo.saveAll(lista);
    }

    @Transactional
    public void eliminarIntento(Long id) {
        if (!intentoRepo.existsById(id)) {
            throw new ResourceNotFoundException("Intento no encontrado con id: " + id);
        }
        intentoRepo.deleteById(id);
    }

    @Transactional
    public void cerrarSesion(String sessionId) {
        List<SesionActiva> coincidencias = sesionRepo.findAll().stream()
                .filter(s -> sessionId.equals(s.getSessionId()) || sessionId.equals(s.getToken()))
                .toList();

        if (coincidencias.isEmpty()) {
            log.warn("Sesión {} no encontrada al intentar cerrarla", sessionId);
            return;
        }
        sesionRepo.deleteAll(coincidencias);
    }

    @Transactional
    public void cerrarTodasLasSesiones(String tokenActual) {
        List<SesionActiva> otras = sesionRepo.findAll().stream()
                .filter(s -> !tokenActual.equals(s.getToken()))
                .toList();
        sesionRepo.deleteAll(otras);
    }

    public boolean isIpBloqueada(String ip) {
        return intentoRepo.findAll().stream()
                .anyMatch(i -> ip.equals(i.getIp()) && Boolean.TRUE.equals(i.getBloqueada()));
    }

    public boolean isSesionRegistrada(String token) {
        return sesionRepo.findByToken(token).isPresent();
    }

    @Transactional
    public void removerSesionPorToken(String token) {
        sesionRepo.deleteByToken(token);
    }
}