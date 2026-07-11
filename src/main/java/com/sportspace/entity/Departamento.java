package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ubigeo_peru_departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Departamento {

    // id es varchar(2) ej: "15" para Lima
    @Id
    @Column(length = 2)
    private String id;

    @Column(nullable = false, length = 45)
    private String name;
}