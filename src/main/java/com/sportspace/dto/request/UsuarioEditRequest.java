package com.sportspace.dto.request;

import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEditRequest {

    /** Nuevo correo electronico obligatorio, debe ser único). */
    private String email;

    /** Nuevo telefono opcional, 9 dígitos.. */
    private String telefono;

    /**
     * Nuevo rol del usuario.
     * Valores permitidos CLIENTE o PROPIETARIO.*/
    private String rol;
}