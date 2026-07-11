package com.sportspace.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApiPeruReniecResponse {

    private boolean success;
    private String  message;
    private DataReniec data;

    @Data
    public static class DataReniec {

        @JsonProperty("number")
        private String dni;

        @JsonProperty("full_name")
        private String nombreCompleto;

        @JsonProperty("name")
        private String nombres;

        @JsonProperty("surname")
        private String apellidos;

        @JsonProperty("date_of_birth")
        private String fechaNacimiento;

        @JsonProperty("department")
        private String departamento;

        @JsonProperty("province")
        private String provincia;

        @JsonProperty("district")
        private String distrito;

        @JsonProperty("address")
        private String direccion;

        @JsonProperty("ubigeo")
        private String ubigeo;
    }
}
