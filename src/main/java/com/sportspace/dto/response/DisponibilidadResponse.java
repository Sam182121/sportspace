package com.sportspace.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DisponibilidadResponse {
    private Long canchaId;
    private String canchaNombre;
    private LocalDate fecha;

    /** true si el propietario bloqueó esta fecha completa (mantenimiento, feriado, etc). */
    private boolean bloqueada;
    /** Motivo del bloqueo, solo presente si bloqueada=true. */
    private String motivoBloqueo;

    /**
     * Horarios del día, uno por cada hora configurada como DISPONIBLE por el
     * propietario en su horario semanal. Cada slot dura 1 hora.
     */
    private List<SlotDisponibilidad> slots;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SlotDisponibilidad {
        private LocalTime inicio;
        private LocalTime fin;
        /** true = libre y reservable; false = ya ocupado por otra reserva o ya pasó la hora. */
        private boolean disponible;
    }
}