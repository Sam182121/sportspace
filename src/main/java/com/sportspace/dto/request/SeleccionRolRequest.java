package com.sportspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SeleccionRolRequest {

    @NotBlank(message = "El preToken es obligatorio")
    private String preToken;

    /** "CLIENTE" o "PROPIETARIO" */
    @NotBlank(message = "Debes elegir un rol")
    private String rol;
}