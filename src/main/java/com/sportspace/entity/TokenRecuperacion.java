package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens_recuperacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Token UUID aleatorio — único e irrepetible */
    @Column(nullable = false, unique = true, length = 100)
    private String token;

    /** Email del usuario asociado */
    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tipo = "RECUPERACION";

    /** Cuándo expira el enlace */
    @Column(nullable = false)
    private LocalDateTime expiracion;

    /** true una vez que el token fue consumido — impide reutilización */
    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    /** Fecha y hora en que fue creado */
    @Column(nullable = false)
    private LocalDateTime creadoEn;

    /** Fecha y hora exacta en que fue utilizado (auditoría) */
    @Column
    private LocalDateTime fechaUso;

    /** IP desde donde se usó el enlace (auditoría) */
    @Column(length = 45)
    private String ipUso;

    /** User-Agent del navegador que usó el enlace (auditoría) */
    @Column(length = 500)
    private String userAgentUso;
}