package com.sportspace.dto.response;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO para el modal de estadísticas de un propietario específico.
 * Devuelve métricas detalladas sobre sus canchas y reservas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropietarioEstadisticasResponse {

    private Long       totalCanchas;
    private Long       canchasActivas;
    private Long       canchasInactivas;

    private Long       totalReservas;
    private Long       reservasConfirmadas;
    private Long       reservasCanceladas;
    private Long       reservasPendientes;

    /** Ingresos del mes actual (reservas CONFIRMADAS) */
    private BigDecimal ingresosMes;

    /** Ingresos históricos totales (reservas CONFIRMADAS) */
    private BigDecimal ingresosTotal;
}