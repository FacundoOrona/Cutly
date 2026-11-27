package com.dev.cutly.empleado.entity;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "empleados")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empleado_id")
    private Long empleadoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "porcentaje_remuneracion", nullable = false, precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) NOT NULL DEFAULT 0.00")
    private BigDecimal porcentajeRemuneracion = BigDecimal.ZERO;
}
