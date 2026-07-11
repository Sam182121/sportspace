package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Registra cada intento fallido de login agrupado por IP + correo.
 * Si la misma IP sigue intentando, se incrementa el contador.
 */
@Entity
@Table(name = "intentos_fallidos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ip", "correo_intentado"}))
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentoFallido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dirección IP desde donde se intentó el acceso. */
    @Column(nullable = false, length = 45)
    private String ip;

    /** Email que se usó en el intento (puede no existir). */
    @Column(name = "correo_intentado", length = 100)
    private String correoIntentado;

    /** Número acumulado de intentos desde esta IP+correo. */
    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 1;

    /** Indica si el admin bloqueó esta IP manualmente. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean bloqueada = false;

    /** Fecha/hora del último intento fallido. */
    @Column(name = "ultimo_intento")
    private LocalDateTime ultimoIntento;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}