package com.sportspace.dto.response.dashboard;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardClienteResponse {

    private String     nombreCompleto;
    private String     email;
    private String     tipoDocumento;    // "DNI" o "CE"
    private String     numeroDocumento;  // unificado
    private Long       totalReservas;
    private Long       reservasPendientes;
    private Long       reservasConfirmadas;
    private Long       reservasCanceladas;
    private BigDecimal totalGastado;

    private List<ProximaReserva>   proximasReservas;
    private List<HistorialReserva> historialReservas;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ProximaReserva {
        private Long       id;
        private String     canchaNombre;
        private String     canchaDeporte;
        private String     canchaDistrito;
        private String     fecha;
        private String     horaInicio;
        private String     horaFin;
        private String     estado;
        private BigDecimal total;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class HistorialReserva {
        private Long       id;
        private String     canchaNombre;
        private String     canchaDeporte;
        private String     fecha;
        private String     horaInicio;
        private String     horaFin;
        private String     estado;
        private BigDecimal total;
    }
}