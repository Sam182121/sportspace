package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    // "DNI" o "CE"
    @Column(name = "tipo_documento", length = 3)
    private String tipoDocumento;

    // Número de documento unificado: 8 dígitos para DNI, hasta 12 para C.E.
    @Column(name = "numero_documento", unique = true, length = 12)
    private String numeroDocumento;

    @Column(length = 60)
    private String nacionalidad;

    @Column(length = 15)
    private String telefono;

    @Column(name = "fecha_nacimiento", length = 20)
    private String fechaNacimiento;

    @Column(length = 100)
    private String departamento;

    @Column(length = 100)
    private String provincia;

    @Column(length = 100)
    private String distrito;

    @Column(length = 255)
    private String direccion;

    @Column(length = 10)
    private String ubigeo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    /**
     * Indican qué roles TIENE la cuenta (independiente de "rol", que es el
     * rol ACTIVO con el que quedó autenticado en la última sesión).
     * Si ambos son true, el login debe preguntar con cuál desea ingresar.
     * Si están en null (cuentas antiguas), se asume según el campo "rol".
     */
    @Column(name = "es_cliente")
    private Boolean esCliente;

    @Column(name = "es_propietario")
    private Boolean esPropietario;

    @Transient
    public boolean tieneRolCliente() {
        return esCliente != null ? esCliente : rol == Rol.CLIENTE;
    }

    @Transient
    public boolean tieneRolPropietario() {
        return esPropietario != null ? esPropietario : rol == Rol.PROPIETARIO;
    }

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_propietario", length = 20)
    private EstadoPropietario estadoPropietario;

    @Column(name = "email_verificado", nullable = false)
    @Builder.Default
    private Boolean emailVerificado = false;

    @Column(name = "telefono_verificado", nullable = false)
    @Builder.Default
    private Boolean telefonoVerificado = false;

    /**
     * CAMPO NUEVO — true cuando el usuario bloqueó su cuenta desde el correo de seguridad.
     * Se diferencia de activo=false por admin: este flag indica que fue el propio usuario.
     * Se limpia cuando el admin reactiva la cuenta manualmente.
     */
    @Column(name = "bloqueado_por_seguridad", nullable = false)
    @Builder.Default
    private Boolean bloqueadoPorSeguridad = false;

    /**
     * fecha y hora exacta en que el usuario bloqueó su cuenta.
     * Null si nunca fue bloqueada por seguridad.
     */
    @Column(name = "fecha_bloqueo_seguridad")
    private LocalDateTime fechaBloqueSeguridad;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}