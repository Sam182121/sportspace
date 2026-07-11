package com.sportspace.dto.response.dashboard.admin;

import lombok.*;
import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class DashboardReservasDeporteResponse {
    private List<DeporteStats> deportes;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class DeporteStats {
        private String deporte;
        private Long total;
        private Integer porcentaje;
    }
}