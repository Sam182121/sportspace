package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_verificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A qué usuario pertenece este código
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // El código de 6 dígitos que se envia
    @Column(nullable = false, length = 6)
    private String codigo;

    // Indica si es para verificar EMAIL o TELEFONO
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoVerificacion tipo;

    // Fecha y hora en que expira el codigo 15 min
    @Column(nullable = false)
    private LocalDateTime expiracion;

    // Un código solo puede usarse una vez aunque no haya expirado.
    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;
}