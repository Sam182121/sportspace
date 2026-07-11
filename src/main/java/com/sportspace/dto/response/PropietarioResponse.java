package com.sportspace.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO que el admin recibe por cada propietario.
 * Incluye métricas calculadas: canchas, reservas activas e ingresos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropietarioResponse {

    private Long   id;
    private String nombres;
    private String apellidos;
    private String email;
    private String tipoDocumento;    // "DNI" o "CE"
    private String numeroDocumento;  // unificado: DNI o C.E.
    private String telefono;
    private String departamento;
    private String provincia;
    private String distrito;

    /** Estado del propietario: ACTIVO, PENDIENTE o SUSPENDIDO */
    private String estado;

    /** Número de canchas registradas (activas + inactivas) */
    private Long totalCanchas;

    /** Reservas en estado PENDIENTE o CONFIRMADA sobre sus canchas */
    private Long reservasActivas;

    /** Suma de totales de reservas CONFIRMADAS de todas sus canchas */
    private BigDecimal ingresosGenerados;

    private LocalDateTime createdAt;
}