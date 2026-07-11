package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ubigeo_peru_provinces")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provincia {

    // id es varchar(4)
    @Id
    @Column(length = 4)
    private String id;

    @Column(nullable = false, length = 45)
    private String name;

    // FK al departamento al que pertenece
    @Column(name = "department_id", nullable = false, length = 2)
    private String departmentId;
}