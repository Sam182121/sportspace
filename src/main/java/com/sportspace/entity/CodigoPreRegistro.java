package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_pre_registro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoPreRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Email al que se envió el código (puede ser null si es solo SMS)
    @Column(length = 150)
    private String email;

    // Teléfono al que se envió el código (puede ser null si es solo email)
    @Column(length = 20)
    private String telefono;

    // Código de 6 dígitos enviado
    @Column(nullable = false, length = 6)
    private String codigo;

    // EMAIL o TELEFONO
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoVerificacion tipo;

    // Expira en 15 minutos
    @Column(nullable = false)
    private LocalDateTime expiracion;

    // true cuando ya fue validado correctamente
    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;
}