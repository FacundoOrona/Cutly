package com.dev.cutly.negocio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "direcciones_negocio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DireccionNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "direccion_id")
    private Long direccionId;

    @Column(nullable = false, length = 100)
    private String calle;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(length = 100)
    private String ciudad;

    @Column(length = 100)
    private String provincia;

    @Column(length = 10)
    private String codigoPostal;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false, unique = true)
    private Negocio negocio;
}