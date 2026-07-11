package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nacionalidad")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nacionalidad {

    // ISO del país ej: "PER", "COL", "VEN"
    @Id
    @Column(name = "ISO_NAC", length = 3)
    private String iso;

    // Nombre del país ej: "Perú", "Colombia"
    @Column(name = "PAIS_NAC", nullable = false, length = 60)
    private String pais;

    // Gentilicio ej: "PERUANA", "COLOMBIANA"
    @Column(name = "GENTILICIO_NAC", nullable = false, length = 60)
    private String gentilicio;
}