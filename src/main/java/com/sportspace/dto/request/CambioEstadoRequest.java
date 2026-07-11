package com.sportspace.dto.request;

import com.sportspace.entity.EstadoReserva;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data // LOMBOK GENERA AUTOMATICO GETTER SETTERS  toString equals y hashCode.
public class CambioEstadoRequest {

    // EL NUEVO ESTADO QUE SE LE QUIERE ASIGNAR A LA RESERVA

    @NotNull(message = "El estado es obligatorio")
    private EstadoReserva estado;

    // CAMPO OPCIONAL DEL PORQUE SE HIZO EL CAMBIO
    // AL NO ENCONTRAR SE ENVIA COMO VACIO O NO ENVIA EN EL JSON NO BOTA ERROR
    // PUEDE INGRESAR EL MOTIVO MEDIANTE CAMPO ABIERTO EJMPLO "LO CANCELO POR EL MAL CLIMA"

    private String motivo;
}