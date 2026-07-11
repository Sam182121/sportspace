package com.sportspace.dto.response.dashboard;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardAdminResponse {

    // TARJETAS METRICAS DE USUARIOS
    private Long totalUsuarios;
    private Long totalClientes;
    private Long totalPropietarios;
    private Long usuariosActivos;
    private Long usuariosInactivos;

    // TARJETAS METRICAS DE CANCHAS
    private Long totalCanchas;
    private Long canchasActivas;
    private Long canchasInactivas;

    // TARJETAS METRICAS DE RESERVAS
    private Long totalReservas;
    private Long reservasPendientes;
    private Long reservasConfirmadas;
    private Long reservasCanceladas;

    // TARJETAS METRICAS DE INGRESOS
    // SE USA BIGDECIAML PARA EVITAR PERDIDA 0.000 AL SUMAR INGRESOS
    private BigDecimal ingresosTotalesPlataforma;

    // TABLA TOP 5
    // Listas para mostrar tablas de ranking en la pantalla del ADMIN
    private List<ResumenPropietario> topPropietarios;
    private List<ResumenCancha>      canchasMasReservadas;
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ResumenPropietario {
        private Long   id;
        private String nombreCompleto;
        private String email;
        private Long   totalCanchas;
        private Long   totalReservas;
        private BigDecimal ingresos; // DINERO QUE GENERA EL PROPEITARIO
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ResumenCancha {
        private Long   id;
        private String nombre;
        private String deporte;
        private String distrito;
        private Long   totalReservas; // Cuantas veces se alquilo esta cancha especifica
        private BigDecimal ingresos; // Cuanto dinero hizo
    }
}