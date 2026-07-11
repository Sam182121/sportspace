package com.sportspace.dto.response.dashboard.admin;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class DashboardUltimosPagosResponse {
    private List<PagoRow> pagos;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class PagoRow {
        private Long       id;
        private String     usuarioNombre;
        private String     usuarioEmail;      // el JS usa p.usuarioEmail
        private String     metodo;            // el JS usa p.metodo
        private BigDecimal monto;
        private String     estado;            // COMPLETADO (el JS filtra por APROBADO → lo corregimos abajo)
        private String     fecha;             // el JS usa p.fecha para filtrar por mes
        private Long       reservaId;         // el JS muestra #reservaId
    }
}