package com.sportspace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ubigeo_peru_districts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Distrito {

    // id es varchar(6)
    @Id
    @Column(length = 6)
    private String id;

    @Column(length = 45)
    private String name;

    // FK a la provincia a la que pertenece
    @Column(name = "province_id", length = 4)
    private String provinceId;

    // FK al departamento
    @Column(name = "department_id", length = 2)
    private String departmentId;
}