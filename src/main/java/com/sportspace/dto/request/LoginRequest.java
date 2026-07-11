package com.sportspace.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    // NOTBLANK ASEGURA QUE EL USUARIO NO PONGA CORREO VACIO O LLENO DE ESPACIOS
    // MAIL VALIDA EL CORREO
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    // NOTBLANK ASEGURA QUE LA CONTRASEÑA NO PONGA VACIA
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}