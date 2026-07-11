package com.sportspace.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservaRequest {

    // SE NECESIRA EL ID DE LA RESERVA
    // CON NOMBRE O PRECIO DESDE PANTALLA , BACKEND BUSCA
    @NotNull(message = "La cancha es obligatoria")
    private Long canchaId;

    // @FUTUREORPRSENT VALIDA FECHA DE RESERVA PARA EVITAR HACKS
    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha no puede ser en el pasado")
    private LocalDate fecha;

    // LOCALTIME PARA MANEJAR HORAS EXACTAS
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;
}