package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "horario_slots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cancha_id","dia_semana","hora"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class HorarioSlot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    /** 0=Lun, 1=Mar, … 6=Dom */
    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    /** 0–23 */
    @Column(nullable = false)
    private Integer hora;

    /** DISPONIBLE | BLOQUEADO | MANTENIMIENTO | SIN ESTADO */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "DISPONIBLE";
}