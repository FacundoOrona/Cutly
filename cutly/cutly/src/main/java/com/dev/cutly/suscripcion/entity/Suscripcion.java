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

    /** Corrección explícita reservada al flujo administrativo de regularización. */
    public void corregirPrecioContratadoAdministrativamente(BigDecimal precio) {
        BigDecimal precioValidado = Objects.requireNonNull(precio, "El precio contratado es obligatorio");
        if (precioValidado.signum() < 0) {
            throw new IllegalArgumentException("El precio contratado no puede ser negativo");
        }
        if (precioValidado.precision() - precioValidado.scale() > 8 || precioValidado.scale() > 2) {
            throw new IllegalArgumentException("El precio admite hasta 8 enteros y 2 decimales");
        }
        if (status == SuscripcionStatus.TRIAL && precioValidado.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("El precio contratado de una suscripción TRIAL debe ser cero");
        }
        this.precioContratado = precioValidado;
    }

    @PrePersist
    private void validarAntesDeCrear() {
        validarPrecioContratado(true);
    }

    @PreUpdate
    private void validarAntesDeActualizar() {
        validarPrecioContratado(false);
    }

    private void validarPrecioContratado(boolean obligatorio) {
        if (precioContratado == null && obligatorio) {
            throw new IllegalStateException("Las nuevas suscripciones requieren un precio contratado");
        }
        if (precioContratado == null) {
            return;
        }
        if (precioContratado.signum() < 0) {
            throw new IllegalStateException("El precio contratado no puede ser negativo");
        }
        if (status == SuscripcionStatus.TRIAL && precioContratado.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("El precio contratado de una suscripción TRIAL debe ser cero");
        }
    }
}
