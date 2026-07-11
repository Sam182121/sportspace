package com.sportspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ─────────────────────────────────────────────────────────────────────────────
// Paso 2: el usuario abre el enlace, ingresa su nueva contraseña y la confirma
// ─────────────────────────────────────────────────────────────────────────────
// Uso: POST /api/auth/reset-password
// ─────────────────────────────────────────────────────────────────────────────

@Data
public class ResetPasswordRequest {

    // El token UUID que llegó en el enlace del correo
    @NotBlank(message = "El token es obligatorio")
    private String token;

    // Nueva contraseña elegida por el usuario
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String nuevaPassword;

    // Confirmación (la igualdad se valida en el service, no aquí)
    @NotBlank(message = "Debes confirmar la contraseña")
    private String confirmarPassword;
}