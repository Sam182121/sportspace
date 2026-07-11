package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Destinatario de la notificación: puede ser un PROPIETARIO o un CLIENTE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 400)
    private String mensaje;

    /** Reserva relacionada, para poder llevar al usuario directo a ella. */
    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leida = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum TipoNotificacion {
        // Propietario
        NUEVA_RESERVA,
        RESERVA_CANCELADA_CLIENTE,
        // Cliente
        RESERVA_APROBADA,
        RESERVA_RECHAZADA,
        RESERVA_CANCELADA_PROPIETARIO
    }
}