package com.sportspace.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioResponse {

    private Long          id;
    private String        nombres;
    private String        apellidos;
    private String        email;
    private String        tipoDocumento;    // "DNI" o "CE"
    private String        numeroDocumento;  // unificado: sirve para DNI y C.E.
    private String        nacionalidad;
    private String        telefono;
    private String        rol;
    private Boolean       esCliente;
    private Boolean       esPropietario;
    private Boolean       activo;
    private String        estado;           // "ACTIVO" o "INACTIVO"
    private LocalDateTime createdAt;

    // Campos de ubicación
    private String fechaNacimiento;
    private String departamento;
    private String provincia;
    private String distrito;
    private String direccion;
    private String ubigeo;
}