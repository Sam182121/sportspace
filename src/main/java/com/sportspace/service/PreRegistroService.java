package com.sportspace.service;

import com.sportspace.entity.CodigoPreRegistro;
import com.sportspace.entity.TipoVerificacion;
import com.sportspace.exception.BadRequestException;
import com.sportspace.repository.CodigoPreRegistroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreRegistroService {

    private final CodigoPreRegistroRepository codigoPreRegistroRepository;
    private final EmailService                emailService;

    private static final int MINUTOS_EXPIRACION = 15;
    private final SecureRandom random = new SecureRandom();

    // ENVIA CODE MAIL

    @Transactional
    public void enviarCodigoEmail(String email) {
        // Eliminar códigos anteriores para ese email
        codigoPreRegistroRepository.deleteByEmailAndTipo(email, TipoVerificacion.EMAIL);

        String codigo = generarCodigo();
        CodigoPreRegistro registro = CodigoPreRegistro.builder()
                .email(email)
                .codigo(codigo)
                .tipo(TipoVerificacion.EMAIL)
                .expiracion(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION))
                .usado(false)
                .build();
        codigoPreRegistroRepository.save(registro);
        emailService.enviarCodigoVerificacion(email, codigo);
        log.info("Código EMAIL pre-registro enviado a: {}", email);
    }

    // VERIFICA CODE MAIL

    @Transactional
    public void verificarEmail(String email, String codigoIngresado) {
        CodigoPreRegistro registro = codigoPreRegistroRepository
                .findTopByEmailAndTipoOrderByExpiracionDesc(email, TipoVerificacion.EMAIL)
                .orElseThrow(() -> new BadRequestException(
                        "No hay un código activo para este correo. Pide uno nuevo."));
        validar(registro, codigoIngresado);
        registro.setUsado(true);
        codigoPreRegistroRepository.save(registro);
        log.info("Email pre-registro verificado: {}", email);
    }


    // VERIFICA MAIL
    public void verificarSoloEmailCompleto(String email) {
        boolean emailOk = codigoPreRegistroRepository
                .findTopByEmailAndTipoOrderByExpiracionDesc(email, TipoVerificacion.EMAIL)
                .map(c -> Boolean.TRUE.equals(c.getUsado()))
                .orElse(false);

        if (!emailOk) {
            throw new BadRequestException(
                    "Debes verificar tu correo electrónico antes de registrarte.");
        }
    }

    // LIMPIA DESPUES DE REGISTRO

    @Transactional
    public void limpiarPorEmail(String email) {
        codigoPreRegistroRepository.deleteAllByEmail(email);
        log.info("Códigos pre-registro limpiados para: {}", email);
    }


    private String generarCodigo() {
        return String.valueOf(random.nextInt(900000) + 100000);
    }

    private void validar(CodigoPreRegistro registro, String codigoIngresado) {
        if (Boolean.TRUE.equals(registro.getUsado()))
            throw new BadRequestException("Este código ya fue utilizado. Pide uno nuevo.");
        if (LocalDateTime.now().isAfter(registro.getExpiracion()))
            throw new BadRequestException("El código ha expirado. Pide uno nuevo.");
        if (!registro.getCodigo().equals(codigoIngresado.trim()))
            throw new BadRequestException("Código incorrecto. Verifica e intenta de nuevo.");
    }
}