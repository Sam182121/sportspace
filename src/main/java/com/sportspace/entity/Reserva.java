package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservas")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "reembolso_procesado", nullable = false)
    @Builder.Default
    private Boolean reembolsoProcesado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelado_por", length = 20)
    private CanceladoPor canceladoPor;

    /**
     * Fecha y hora exacta en que el PROPIETARIO aprobó o rechazó la reserva.
     * Null mientras la reserva siga PENDIENTE.
     */
    @Column(name = "fecha_respuesta_propietario")
    private LocalDateTime fechaRespuestaPropietario;

    /**
     * Motivo/comentario que dejó el propietario al RECHAZAR la reserva.
     * Null si la reserva fue aprobada o sigue pendiente.
     */
    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum CanceladoPor {
        CLIENTE,
        PROPIETARIO
    }
}