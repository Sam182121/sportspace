package com.sportspace.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioStatsResponse {
    private Long total;
    private Long activos;
    private Long inactivos;
    private Long propietarios;
    private Long clientes;
    private Long admins;
}