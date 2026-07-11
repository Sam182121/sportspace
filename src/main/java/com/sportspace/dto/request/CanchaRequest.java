package com.sportspace.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CanchaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotBlank(message = "El deporte es obligatorio")
    private String deporte;

    @NotNull(message = "El precio por hora es obligatorio")
    @DecimalMin(value = "0.1", message = "El precio debe ser mayor a 0")
    private BigDecimal precioHora;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    // Ubicación completa
    private String departamento;
    private String provincia;
    private String distrito;

    @Min(value = 1, message = "La capacidad debe ser al menos 1")
    private Integer capacidad;
}