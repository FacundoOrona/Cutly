package com.dev.cutly.turno.entity;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turnos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "turno_id")
    private Long turnoId;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurnoStatus status;

    @Column(precision = 10, scale = 2)
    private BigDecimal precio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_local_id")
    private ClienteLocal clienteLocal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_responsable_id")
    private Usuario ownerResponsable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(length = 500)
    private String observaciones;

    @PrePersist
    @PreUpdate
    void validarRelacionesClienteYResponsable() {
        if ((cliente != null) == (clienteLocal != null)) {
            throw new IllegalStateException("El turno debe tener exactamente un tipo de cliente");
        }
        if ((empleado != null) == (ownerResponsable != null)) {
            throw new IllegalStateException("El turno debe tener exactamente un responsable");
        }
    }
}
