package com.sportspace.dto.response;

import com.sportspace.entity.EstadoReserva;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservaResponse {

    private Long          id;
    private EstadoReserva estado;
    private LocalDate     fecha;
    private LocalTime     horaInicio;
    private LocalTime     horaFin;
    private BigDecimal    total;
    private LocalDateTime createdAt;

    private Boolean       reembolsoProcesado;

    private String        canceladoPor;

    // ── Cancha ────────────────────────────────────────────────────────────────
    private Long       canchaId;
    private String     canchaNombre;
    private String     canchaDeporte;

    /** FIX — el JS usa r.deporte (sin prefijo cancha) */
    private String     deporte;

    private String     canchaDistrito;

    /**
     * FIX — el JS usa r.canhaDistrito (typo con una h) y también r.canchaDireccion.
     * Exponemos ambos para que funcione sin importar cuál use el JS.
     */
    private String     canhaDistrito;      // typo intencional para compatibilidad con el JS
    private String     canchaDireccion;

    private BigDecimal canchaPrecioHora;

    // ── Cliente ───────────────────────────────────────────────────────────────
    private Long   clienteId;
    private String clienteNombre;
    private String clienteEmail;
    private String clienteTelefono;

    private String usuarioNombre;
    private String usuarioEmail;

    // ── Propietario ───────────────────────────────────────────────────────────
    private Long   propietarioId;
    private String propietarioNombre;
    private String propietarioEmail;

    private Double duracionHoras;
}