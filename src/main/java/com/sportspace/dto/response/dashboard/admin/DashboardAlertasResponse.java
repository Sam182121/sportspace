package com.sportspace.dto.response.dashboard.admin;

import lombok.*;
import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class DashboardAlertasResponse {
    private List<AlertaItem> alertas;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class AlertaItem {
        private String tipo;
        private String mensaje;
    }
}