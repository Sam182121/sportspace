package com.sportspace.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String tipo;
    private Long id;
    private String nombres;
    private String apellidos;
    private String email;
    private String rol;

    /** true cuando el usuario tiene doble rol y debe elegir con cuál ingresar. */
    @Builder.Default
    private boolean requiereSeleccion = false;

    /** Token temporal (5 min) para completar el login en /auth/seleccionar-rol. */
    private String preToken;

    /** "Nombre ApellidoPaterno" para el saludo de bienvenida. */
    private String nombreMostrar;

    /** Roles disponibles para elegir, ej: ["CLIENTE", "PROPIETARIO"]. */
    private java.util.List<String> rolesDisponibles;
}