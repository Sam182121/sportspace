package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Cada propietario puede tener hasta 1 registro por tipo:
 *   TRANSFERENCIA, YAPE, PLIN
 *
 * Cuando el cliente va a pagar una reserva, el sistema consulta
 * los métodos ACTIVOS del propietario de la cancha para mostrarlos.
 */
@Entity
@Table(name = "metodos_pago_propietario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"propietario_id", "tipo"}))
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetodoPagoPropietario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Usuario propietario;

    /** TRANSFERENCIA | YAPE | PLIN */
    @Column(nullable = false, length = 20)
    private String tipo;

    /** true = visible para clientes, false = oculto */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /* Campos TRANSFERENCIA */
    @Column(length = 80)
    private String banco;

    @Column(name = "numero_cuenta", length = 30)
    private String numeroCuenta;

    @Column(length = 30)
    private String cci;

    @Column(name = "titular_cuenta", length = 100)
    private String titularCuenta;

    /* Campos YAPE / PLIN  */
    @Column(name = "numero_telefono", length = 15)
    private String numeroTelefono;

    @Column(name = "nombre_titular", length = 100)
    private String nombreTitular;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}