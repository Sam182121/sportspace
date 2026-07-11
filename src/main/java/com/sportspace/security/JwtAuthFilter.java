package com.sportspace.security;

import com.sportspace.entity.Usuario;
import com.sportspace.repository.UsuarioRepository;
import com.sportspace.service.SeguridadService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil                jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final SeguridadService       seguridadService;
    private final UsuarioRepository      usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            // Token expirado → limpiar del mapa de sesiones
            seguridadService.removerSesionPorToken(token);
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.extractEmail(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // ── Re-registrar sesión si no está en el mapa (ej. reinicio del servidor) ──
            if (!seguridadService.isSesionRegistrada(token)) {
                try {
                    usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                        String ip = obtenerIp(request);
                        seguridadService.registrarSesion(
                                token,
                                usuario.getId(),
                                usuario.getNombres(),
                                usuario.getApellidos(),
                                usuario.getEmail(),
                                usuario.getRol().name(),
                                ip
                        );
                    });
                } catch (Exception e) {
                    log.warn("No se pudo re-registrar sesión para {}: {}", email, e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        // IPv6 localhost → normalizar a 127.0.0.1
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}