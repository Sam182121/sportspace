package com.sportspace.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReniecResponse {

    // Número de documento (DNI o C.E.)
    private String numeroDocumento;

    // Tipo de documento: "DNI" o "CE"
    private String tipoDocumento;

    private String nombres;
    private String apellidos;
    private String nombreCompleto;

    // Estos campos solo vienen con DNI — con C.E. vienen null
    private String fechaNacimiento;
    private String departamento;
    private String provincia;
    private String distrito;
    private String direccion;
    private String ubigeo;

    // Nacionalidad: para DNI siempre "PERUANA",
    // para C.E. el usuario la escribe manualmente
    private String nacionalidad;
}