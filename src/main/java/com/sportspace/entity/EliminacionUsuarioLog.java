package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Auditoría de cuando un ADMIN elimina o anonimiza una cuenta de usuario.
 * Guarda un snapshot de los datos porque el usuario puede haber sido
 * borrado o anonimizado (ya no se puede confiar en leer Usuario por id).
 */
@Entity
@Table(name = "eliminaciones_usuario_log")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EliminacionUsuarioLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "email_snapshot", length = 150)
    private String emailSnapshot;

    @Column(name = "nombre_snapshot", length = 200)
    private String nombreSnapshot;

    @Column(length = 20)
    private String rol;

    /** SOLICITADO_POR_USUARIO | MAL_USO_PLATAFORMA | CUENTA_DUPLICADA_PRUEBA | OTRO */
    @Column(nullable = false, length = 40)
    private String motivo;

    @Column(length = 500)
    private String comentario;

    /** ELIMINADO TOTAL | ELIMINADO TOTAL FORZADO | ANONIMIZADO | DESACTIVADO */
    @Column(name = "tipo_accion", nullable = false, length = 40)
    private String tipoAccion;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_nombre", length = 200)
    private String adminNombre;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}