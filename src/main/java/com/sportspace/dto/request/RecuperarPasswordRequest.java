package com.sportspace.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ─────────────────────────────────────────────────────────────────────────────
// Paso 1: el usuario envía su email para recibir el enlace
// ─────────────────────────────────────────────────────────────────────────────
// Uso: POST /api/auth/recuperar-password
//
// Nota: la respuesta SIEMPRE devuelve el mismo mensaje genérico ("Si el correo
// existe en nuestro sistema...") sin importar si el email existe o no.
// Esto previene la enumeración de cuentas registradas.
// ─────────────────────────────────────────────────────────────────────────────

@Data
public class RecuperarPasswordRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresa un correo electrónico válido")
    private String email;
}