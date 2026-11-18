package com.dev.cutly.suscripcion.entity;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "suscripciones")
@Data
@NoArgsConstructor
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suscripcion_id")
    private Long suscripcionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuscripcionStatus status;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Setter(AccessLevel.NONE)
    @Column(name = "precio_contratado", precision = 10, scale = 2, nullable = true)
    private BigDecimal precioContratado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanSuscripcion plan;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false, unique = true)
    private Negocio negocio;

    public void asignarPrecioContratado(BigDecimal precio) {
        if (precioContratado != null) {
            throw new IllegalStateException("El precio contratado ya fue asignado");
        }
        BigDecimal precioValidado = Objects.requireNonNull(precio, "El precio contratado es obligatorio");
        if (precioValidado.signum() < 0) {
            throw new IllegalArgumentException("El precio contratado no puede ser negativo");
        }
        if (status == SuscripcionStatus.TRIAL && precioValidado.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("El precio contratado de una suscripción TRIAL debe ser cero");
        }
        this.precioContratado = precioValidado;
    }

    @PrePersist
    private void validarAntesDeCrear() {
        if (precioContratado == null) {
            throw new IllegalStateException("Las nuevas suscripciones requieren un precio contratado");
        }
        if (precioContratado.signum() < 0) {
            throw new IllegalStateException("El precio contratado no puede ser negativo");
        }
        if (status == SuscripcionStatus.TRIAL && precioContratado.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("El precio contratado de una suscripción TRIAL debe ser cero");
        }
    }
}
