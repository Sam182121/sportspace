package com.sportspace.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// DTO que mapea la respuesta de la API de Factiliza

@Data
public class FactilizaResponse {

    private boolean success;
    private String  message;
    private DataFactiliza data;

    @Data
    public static class DataFactiliza {

        // Campos comunes a DNI y C.E.
        @JsonProperty("numero")
        private String numero;

        @JsonProperty("nombres")
        private String nombres;

        @JsonProperty("apellido_paterno")
        private String apellidoPaterno;

        @JsonProperty("apellido_materno")
        private String apellidoMaterno;

        @JsonProperty("nombre_completo")
        private String nombreCompleto;

        // Solo vienen con DNI (C.E. devuelve estos campos vacíos o ausentes)

        @JsonProperty("departamento")
        private String departamento;

        @JsonProperty("provincia")
        private String provincia;

        @JsonProperty("distrito")
        private String distrito;

        @JsonProperty("direccion")
        private String direccion;

        @JsonProperty("ubigeo_reniec")
        private String ubigeoReniec;

        @JsonProperty("fecha_nacimiento")
        private String fechaNacimiento;
    }
}