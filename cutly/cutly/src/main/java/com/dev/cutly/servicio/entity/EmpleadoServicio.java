package com.dev.cutly.servicio.entity;

import com.dev.cutly.empleado.entity.Empleado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

 /* Un empleado puede realizar varios servicios y un servicio puede ser realizado por varios empleados.
    Por eso necesitamos una relación:
    Empleado N ───── N Servicio */

@Entity
@Table(
        name = "empleados_servicios",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_empleado_servicio",
                        columnNames = {"empleado_id", "servicio_id"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpleadoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empleado_servicio_id")
    private Long empleadoServicioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
}