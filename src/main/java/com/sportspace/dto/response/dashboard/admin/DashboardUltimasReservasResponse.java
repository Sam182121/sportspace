package com.sportspace.dto.response.dashboard.admin;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class DashboardUltimasReservasResponse {
    private List<ReservaRow> reservas;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ReservaRow {
        private Long id;
        private String usuarioNombre;
        private String canchaNombre;
        private String fecha;
        private BigDecimal total;
        private String estado;
    }
}