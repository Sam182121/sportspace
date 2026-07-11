package com.sportspace.service;

import com.sportspace.entity.CodigoVerificacion;
import com.sportspace.entity.TipoVerificacion;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import com.sportspace.repository.CodigoVerificacionRepository;
import com.sportspace.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificacionService {

    private final UsuarioRepository            usuarioRepository;
    private final CodigoVerificacionRepository codigoRepository;
    private final EmailService                 emailService;

    private final SecureRandom random = new SecureRandom();
    private static final int MINUTOS_EXPIRACION = 15;

    // enviar code mail

    @Transactional
    public void enviarCodigoEmail(String email) {
        Usuario usuario = buscarPorEmail(email);

        if (Boolean.TRUE.equals(usuario.getEmailVerificado()))
            throw new BadRequestException("El correo ya fue verificado anteriormente.");

        String codigo = generarCodigo();
        guardarCodigo(usuario, codigo, TipoVerificacion.EMAIL);
        emailService.enviarCodigoVerificacion(usuario.getEmail(), codigo);
        log.info("Código EMAIL enviado al usuario id={}", usuario.getId());
    }

    // ── VERIFICAR CÓDIGO DE EMAIL ─────────────────────────────────────────────

    @Transactional
    public void verificarEmail(String email, String codigoIngresado) {
        Usuario usuario = buscarPorEmail(email);

        if (Boolean.TRUE.equals(usuario.getEmailVerificado()))
            throw new BadRequestException("El correo ya fue verificado.");

        validarCodigo(usuario, codigoIngresado, TipoVerificacion.EMAIL);

        usuario.setEmailVerificado(true);
        // Sin SMS: activar cliente directamente al verificar el email
        activarSiCorresponde(usuario);
        usuarioRepository.save(usuario);
        log.info("Email verificado para usuario id={}", usuario.getId());
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private String generarCodigo() {
        return String.valueOf(random.nextInt(900000) + 100000);
    }

    private void guardarCodigo(Usuario usuario, String codigo, TipoVerificacion tipo) {
        codigoRepository.deleteByUsuarioIdAndTipo(usuario.getId(), tipo);
        CodigoVerificacion nuevo = CodigoVerificacion.builder()
                .usuario(usuario)
                .codigo(codigo)
                .tipo(tipo)
                .expiracion(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION))
                .usado(false)
                .build();
        codigoRepository.save(nuevo);
    }

    private void validarCodigo(Usuario usuario, String codigoIngresado, TipoVerificacion tipo) {
        CodigoVerificacion registro = codigoRepository
                .findTopByUsuarioIdAndTipoOrderByExpiracionDesc(usuario.getId(), tipo)
                .orElseThrow(() -> new BadRequestException(
                        "No se encontró un código activo. Solicita uno nuevo."));

        if (Boolean.TRUE.equals(registro.getUsado()))
            throw new BadRequestException("Este código ya fue utilizado. Solicita uno nuevo.");

        if (LocalDateTime.now().isAfter(registro.getExpiracion()))
            throw new BadRequestException("El código ha expirado. Solicita uno nuevo.");

        if (!registro.getCodigo().equals(codigoIngresado.trim()))
            throw new BadRequestException("Código incorrecto. Verifica e intenta de nuevo.");

        registro.setUsado(true);
        codigoRepository.save(registro);
    }

    private void activarSiCorresponde(Usuario usuario) {
        switch (usuario.getRol()) {
            case CLIENTE    -> usuario.setActivo(true);
            case PROPIETARIO -> log.info(
                    "Propietario id={} verificó email. Queda PENDIENTE hasta aprobación del admin.",
                    usuario.getId());
            case ADMIN      -> usuario.setActivo(true);
        }
    }

    private Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con email: " + email));
    }
}