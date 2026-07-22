package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_activas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionActiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 36)
    private String sessionId;

    @Column(unique = true, length = 1000)
    private String token;

    private Long usuarioId;
    private String nombres;
    private String apellidos;
    private String email;
    private String rol;
    private String ip;
    private LocalDateTime inicioDeSesion;
}