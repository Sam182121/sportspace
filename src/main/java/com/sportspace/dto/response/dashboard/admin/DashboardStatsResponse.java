package com.sportspace.dto.response.dashboard.admin;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class DashboardStatsResponse {
    private Long totalUsuarios;
    private Long totalPropietarios;
    private Long totalCanchas;
    private Long canchasActivas;
    private Long reservasHoy;
    private Long reservasPendientes;
    private BigDecimal ingresosDia;
    private BigDecimal ingresosMes;
    private Long partidosActivos;
    private Long usuariosConectados;
    private Long nuevosEsteMes;
    private Long propietariosPendientes;
    private Boolean tieneAlertas;
}