package com.sportspace.dto.response.dashboard.admin;

import lombok.*;
import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class DashboardActividadResponse {
    private List<ActividadItem> actividad;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ActividadItem {
        private String tipo;
        private String titulo;
        private String subtitulo;
        private String tiempo;
        private String color;
    }
}