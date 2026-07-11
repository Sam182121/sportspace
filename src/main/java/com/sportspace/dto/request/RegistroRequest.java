package com.sportspace.dto.request;

import com.sportspace.entity.Rol;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistroRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombres;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellidos;

    @NotBlank
    @Email(message = "Email invalido")
    private String email;

    @NotBlank
    @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres")
    private String password;

    // "DNI" o "CE"
    private String tipoDocumento;

    // Número de documento unificado: 8 dígitos para DNI, hasta 12 para C.E.
    private String numeroDocumento;

    // Nacionalidad: "PERUANA" para DNI; gentilicio elegido por el usuario para C.E.
    private String nacionalidad;

    @NotBlank(message = "El telefono es obligatorio")
    @Pattern(regexp = "\\d{9}", message = "El telefono debe tener 9 digitos")
    private String telefono;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    // Campos de ubicación — se auto-completan desde la API para DNI;
    // el usuario los ingresa manualmente para C.E.
    private String fechaNacimiento;
    private String departamento;
    private String provincia;
    private String distrito;
    private String direccion;
    private String ubigeo;
}