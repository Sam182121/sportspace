package com.sportspace.dto.response.dashboard;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardPropietarioResponse {

    // RESUMEN CANCHAS
    private Long   totalCanchas;
    private Long   canchasActivas;
    private Long   canchasInactivas;

    // RESUMEN RESERVAS
    private Long   totalReservas;
    private Long   reservasPendientes;
    private Long   reservasConfirmadas;
    private Long   reservasCanceladas;

    // RESUMEN INGRESOS
    private BigDecimal ingresosTotales;
    private BigDecimal ingresosEsteMes;

    // LISTA DETALLES
    private List<ResumenCanchaDetalle> canchas;
    private List<UltimaReserva>        ultimasReservas;

    // DETALLE DE CADA CANCHA
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ResumenCanchaDetalle {
        private Long       id;
        private String     nombre;
        private String     deporte;
        private String     distrito;
        private BigDecimal precioHora;
        private Boolean    activa;
        private Long       totalReservas;
        private Long       reservasPendientes;
        private Long       reservasConfirmadas;
        private BigDecimal ingresos;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class UltimaReserva {
        private Long       id;
        private String     clienteNombre;
        private String     canchaNombre;
        private String     fecha;
        private String     horaInicio;
        private String     horaFin;
        private String     estado;
        private BigDecimal total;
    }
}