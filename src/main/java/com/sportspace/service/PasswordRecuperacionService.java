package com.sportspace.service;

import com.sportspace.dto.request.RecuperarPasswordRequest;
import com.sportspace.dto.request.ResetPasswordRequest;
import com.sportspace.entity.TokenRecuperacion;
import com.sportspace.exception.BadRequestException;
import com.sportspace.repository.TokenRecuperacionRepository;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecuperacionService {

    private final TokenRecuperacionRepository tokenRepo;
    private final UsuarioRepository           usuarioRepo;
    private final PasswordEncoder             passwordEncoder;
    private final EmailService                emailService;

    private static final int MINUTOS_EXPIRACION = 30;
    private static final int HORAS_BLOQUEO      = 24;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    // ── PASO 1: SOLICITAR ENLACE DE RECUPERACIÓN ──────────────────────────────

    @Transactional
    public void solicitarRecuperacion(RecuperarPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        usuarioRepo.findByEmail(email).ifPresent(usuario -> {

            // FIX — cuenta bloqueada por seguridad no puede recuperar contraseña
            if (Boolean.TRUE.equals(usuario.getBloqueadoPorSeguridad())) {
                throw new BadRequestException(
                        "Tu cuenta está bloqueada por seguridad. " +
                                "Comunícate con soporte para reactivarla.");
            }

            if (!usuario.getActivo()) {
                throw new BadRequestException(
                        "Tu cuenta está inactiva. Comunícate con soporte.");
            }

            tokenRepo.deleteByEmail(email);

            String token = UUID.randomUUID().toString();

            TokenRecuperacion registro = TokenRecuperacion.builder()
                    .token(token)
                    .email(email)
                    .tipo("RECUPERACION")
                    .expiracion(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION))
                    .usado(false)
                    .creadoEn(LocalDateTime.now())
                    .build();

            tokenRepo.save(registro);

            String enlace = frontendUrl + "/reset-password?token=" + token;
            emailService.enviarEnlaceRecuperacion(email, enlace);
            log.info("Enlace de recuperación enviado a: {}", email);
        });

        // Si el email no existe, no hacemos nada — respuesta genérica en el controller
    }

    // ── PASO 2: VALIDAR TOKEN DE RECUPERACIÓN ────────────────────────────────

    public void validarToken(String token) {
        TokenRecuperacion registro = tokenRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException(
                        "El enlace no es válido o ya fue utilizado."));

        if (Boolean.TRUE.equals(registro.getUsado())) {
            throw new BadRequestException(
                    "Este enlace ya fue utilizado. Solicita uno nuevo.");
        }

        if (LocalDateTime.now().isAfter(registro.getExpiracion())) {
            throw new BadRequestException(
                    "El enlace ha expirado. Solicita uno nuevo desde la página de inicio de sesión.");
        }
    }

    // ── PASO 2B: VALIDAR TOKEN DE BLOQUEO (al cargar /bloquear-cuenta) ────────
    // El frontend llama este endpoint al cargar la página para detectar tokens
    // ya usados o expirados ANTES de mostrar el formulario de bloqueo.

    public void validarTokenBloqueo(String token) {
        TokenRecuperacion registro = tokenRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException(
                        "Este enlace no es válido o ya fue utilizado."));

        if (Boolean.TRUE.equals(registro.getUsado())) {
            throw new BadRequestException(
                    "Este enlace ya fue utilizado. Tu cuenta ya fue bloqueada con este enlace.");
        }

        if (LocalDateTime.now().isAfter(registro.getExpiracion())) {
            throw new BadRequestException(
                    "Este enlace ha expirado. Escríbenos a rondomnims9@gmail.com para bloquear tu cuenta.");
        }
    }

    // ── PASO 3: RESTABLECER CONTRASEÑA ────────────────────────────────────────

    @Transactional
    public Map<String, String> resetearPassword(ResetPasswordRequest request) {

        if (!request.getNuevaPassword().equals(request.getConfirmarPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden.");
        }

        validarToken(request.getToken());

        TokenRecuperacion registro = tokenRepo.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Token inválido."));

        var usuario = usuarioRepo.findByEmail(registro.getEmail())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado."));

        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepo.save(usuario);

        registro.setUsado(true);
        registro.setFechaUso(LocalDateTime.now());
        tokenRepo.save(registro);

        log.info("Contraseña restablecida para: {}", registro.getEmail());

        // Generar token de bloqueo (distinto al de recuperación)
        tokenRepo.deleteByEmailAndTipo(registro.getEmail(), "BLOQUEO");

        String tokenBloqueo = UUID.randomUUID().toString();
        tokenRepo.save(TokenRecuperacion.builder()
                .token(tokenBloqueo)
                .email(registro.getEmail())
                .tipo("BLOQUEO")
                .expiracion(LocalDateTime.now().plusHours(HORAS_BLOQUEO))
                .usado(false)
                .creadoEn(LocalDateTime.now())
                .build());

        String primerNombre  = usuario.getNombres().split(" ")[0];
        String enlaceBloqueo = frontendUrl + "/bloquear-cuenta?token=" + tokenBloqueo
                + "&email=" + usuario.getEmail();

        try {
            emailService.enviarCorreoSeguridad(registro.getEmail(), primerNombre, enlaceBloqueo);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de seguridad a {}: {}", registro.getEmail(), e.getMessage());
        }

        return Map.of(
                "mensaje", "Tu contraseña fue restablecida correctamente.",
                "nombre",  usuario.getNombres(),
                "email",   usuario.getEmail()
        );
    }

    // ── BLOQUEAR CUENTA ───────────────────────────────────────────────────────

    @Transactional
    public Map<String, String> bloquearCuenta(String tokenBloqueo) {

        // FIX 1 — si el token no existe en la BD, rechazar
        TokenRecuperacion registro = tokenRepo.findByToken(tokenBloqueo)
                .orElseThrow(() -> new BadRequestException(
                        "Este enlace no es válido o ya fue utilizado."));

        // FIX 2 — si el token ya fue usado, rechazar con mensaje claro
        if (Boolean.TRUE.equals(registro.getUsado())) {
            throw new BadRequestException(
                    "Este enlace ya fue utilizado. Tu cuenta ya estaba bloqueada.");
        }

        // FIX 3 — si el token expiró, rechazar
        if (LocalDateTime.now().isAfter(registro.getExpiracion())) {
            throw new BadRequestException(
                    "Este enlace ha expirado. Escríbenos a rondomnims9@gmail.com para bloquear tu cuenta.");
        }

        var usuario = usuarioRepo.findByEmail(registro.getEmail())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado."));

        // FIX 4 — setear los tres campos de bloqueo correctamente
        usuario.setActivo(false);
        usuario.setBloqueadoPorSeguridad(true);
        usuario.setFechaBloqueSeguridad(LocalDateTime.now());
        usuarioRepo.save(usuario);

        // FIX 5 — marcar el token como USADO para que no funcione de nuevo
        registro.setUsado(true);
        registro.setFechaUso(LocalDateTime.now());
        tokenRepo.save(registro);

        log.warn("Cuenta BLOQUEADA por seguridad: {}", registro.getEmail());

        // FIX 6 — enviar correo de confirmación de bloqueo al usuario
        String primerNombre = usuario.getNombres().split(" ")[0];
        try {
            emailService.enviarCorreoBloqueoConfirmado(registro.getEmail(), primerNombre);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de confirmación de bloqueo a {}: {}",
                    registro.getEmail(), e.getMessage());
        }

        return Map.of(
                "mensaje", "Tu cuenta ha sido bloqueada temporalmente.",
                "nombre",  usuario.getNombres(),
                "email",   usuario.getEmail()
        );
    }
}