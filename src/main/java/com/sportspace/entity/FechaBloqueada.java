package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "fechas_bloqueadas")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FechaBloqueada {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 100)
    private String motivo;

    @Column(length = 300)
    private String nota;
}