package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "canchas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Usuario propietario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 50)
    private String deporte;

    @Column(name = "precio_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioHora;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(length = 100)
    private String departamento;

    @Column(length = 100)
    private String provincia;

    @Column(length = 100)
    private String distrito;

    private Integer capacidad;

    /**
     * Estado del flujo de aprobación:
     *   PENDIENTE  → recién creada, espera aprobación del admin
     *   ACTIVA     → aprobada y visible públicamente
     *   DESTACADA  → activa + aparece primero en búsquedas
     *   INACTIVA   → oculta del listado público
     */
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PENDIENTE'")
    @Builder.Default
    private String estado = "PENDIENTE";

    /**
     * true  = cancha visible/activa
     * false = cancha inactiva/desactivada
     * Se sincroniza con 'estado': ACTIVA o DESTACADA → activa=true
     */
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean activa = true;

    /** Contador desnormalizado de reservas totales. */
    @Column(name = "total_reservas", nullable = false,
            columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer totalReservas = 0;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha/hora de la última edición hecha por el propietario (PUT).
     * Se usa para limitar a 1 edición por mes por cancha.
     */
    @Column(name = "ultima_edicion")
    private LocalDateTime ultimaEdicion;

    @Column(name = "tipo_superficie", length = 50)
    private String tipoSuperficie;

    /**
     * Fotos de la cancha (max 3).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "cancha_fotos",
            joinColumns = @JoinColumn(name = "cancha_id")
    )
    @Column(name = "foto_url", columnDefinition = "LONGTEXT")
    @Builder.Default
    private List<String> fotos = new ArrayList<>();
}