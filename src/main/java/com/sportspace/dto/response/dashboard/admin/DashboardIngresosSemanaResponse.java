package com.sportspace.dto.response.dashboard.admin;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class DashboardIngresosSemanaResponse {
    private String rango;
    private List<DiaMonto> dias;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class DiaMonto {
        private String label;
        private BigDecimal monto;
    }
}