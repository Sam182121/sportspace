package com.sportspace.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CanchaResponse {

    private Long        id;
    private String      nombre;
    private String      descripcion;
    private String      deporte;
    private BigDecimal  precioPorHora;
    private String      direccion;
    private String      departamento;
    private String      provincia;
    private String      distrito;
    private Integer     capacidad;
    private String      estado;
    private Boolean     activa;
    private Integer     totalReservas;
    private LocalDateTime createdAt;

    // FOTOS
    private List<String> fotos;

    // Datos del propietario
    private Long        propietarioId;
    private String      propietarioNombre;
    private String      propietarioEmail;
    private String      propietarioTelefono;
}