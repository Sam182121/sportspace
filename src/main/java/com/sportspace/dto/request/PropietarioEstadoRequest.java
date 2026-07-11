package com.sportspace.dto.request;

import lombok.*;

@Data
@NoArgsConstructor // Crea un constructor vacío por detrás public PropietarioEstadoRequest
@AllArgsConstructor // Crea un constructor con todos los campos public PropietarioEstadoRequest
public class PropietarioEstadoRequest {

    // EL NUEVO ESTADO QUE SE LE ASGINA AL PROPIETARIO
    // VALIDO PARA CAMBIAR EL ESTADO DE UN PROPIETARIO
    private String estado;
}