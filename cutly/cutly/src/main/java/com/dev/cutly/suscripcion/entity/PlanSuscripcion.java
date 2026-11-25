package com.dev.cutly.suscripcion.entity;

import com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "planes_suscripcion", indexes = @Index(
        name = "uq_planes_suscripcion_nombre_normalizado",
        columnList = "nombre_normalizado",
        unique = true
))
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlanSuscripcion {

    private static final String NOMBRE_PLAN_TRIAL = "Prueba gratuita";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(
            name = "nombre_normalizado",
            columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin",
            length = 100,
            nullable = false
    )
    private String nombreNormalizado;

    @Column(length = 500)
    private String descripcion;

    /** Precio de una asignación comercial por un período de 30 días. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Column(name = "max_negocios", nullable = true)
    private Integer maxNegocios;

    @Column(nullable = false)
    private boolean activo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPlanSuscripcion tipo;

    @Version
    @Column(name = "version_plan", nullable = false)
    private Long version;

    public static PlanSuscripcion crearPlanTrial() {
        PlanSuscripcion plan = new PlanSuscripcion();
        plan.establecerNombre(NOMBRE_PLAN_TRIAL);
        plan.precioMensual = BigDecimal.ZERO;
        plan.maxNegocios = 1;
        plan.activo = true;
        plan.tipo = TipoPlanSuscripcion.TRIAL;
        return plan;
    }

    public static PlanSuscripcion crearPlanComercial(String nombre, String descripcion, BigDecimal precioMensual, Integer maxNegocios) {
        PlanSuscripcion plan = new PlanSuscripcion();
        plan.tipo = TipoPlanSuscripcion.COMERCIAL;
        plan.actualizarDatosComerciales(nombre, descripcion, precioMensual, maxNegocios);
        plan.activo = false;
        return plan;
    }

    public void clasificarComoTrial() {
        if (tipo != null && tipo != TipoPlanSuscripcion.TRIAL) {
            throw new IllegalStateException("Un plan COMERCIAL no puede convertirse en TRIAL");
        }
        validarPropiedadesTrial();
        tipo = TipoPlanSuscripcion.TRIAL;
    }

    public void asegurarCapacidadTrial() {
        if (tipo != null && tipo != TipoPlanSuscripcion.TRIAL) {
            throw new IllegalStateException("Un plan COMERCIAL no puede configurarse como TRIAL");
        }
        maxNegocios = 1;
    }

    public void clasificarComoComercial() {
        if (tipo != null && tipo != TipoPlanSuscripcion.COMERCIAL) {
            throw new IllegalStateException("Un plan TRIAL no puede convertirse en COMERCIAL");
        }
        String nombrePresentacion = normalizarNombrePresentacion(nombre);
        if (nombrePresentacion.isBlank() || nombrePresentacion.codePointCount(0, nombrePresentacion.length()) > 50) {
            throw new IllegalStateException("Un plan preexistente tiene un nombre inválido");
        }
        if (esNombreReservadoTrial(nombre)) {
            throw new IllegalStateException("El nombre Prueba gratuita está reservado para el plan TRIAL");
        }
        tipo = TipoPlanSuscripcion.COMERCIAL;
    }

    public void actualizarDatosComerciales(String nombre, String descripcion, BigDecimal precioMensual, Integer maxNegocios) {
        if (tipo == TipoPlanSuscripcion.TRIAL) {
            throw new IllegalStateException("El plan TRIAL no puede cambiar sus datos comerciales");
        }
        if (tipo == null) {
            throw new IllegalStateException("No se puede editar un plan sin tipo asignado");
        }
        String nombrePresentacion = normalizarNombrePresentacion(nombre);
        if (nombrePresentacion.isBlank() || nombrePresentacion.codePointCount(0, nombrePresentacion.length()) > 50) {
            throw new IllegalArgumentException("El nombre del plan debe contener entre 1 y 50 caracteres");
        }
        if (esNombreReservadoTrial(nombrePresentacion)) {
            throw new IllegalArgumentException("El nombre Prueba gratuita está reservado para el plan TRIAL");
        }
        if (descripcion != null && descripcion.length() > 500) {
            throw new IllegalArgumentException("La descripción del plan no puede superar los 500 caracteres");
        }
        BigDecimal precioValidado = Objects.requireNonNull(precioMensual, "El precio mensual es obligatorio");
        validarPrecioComercial(precioValidado);
        if (maxNegocios == null || maxNegocios < 1) {
            throw new IllegalArgumentException("La cantidad máxima de negocios debe ser mayor que cero");
        }
        establecerNombre(nombrePresentacion);
        this.descripcion = descripcion;
        this.precioMensual = precioValidado;
        this.maxNegocios = maxNegocios;
    }

    public void cambiarActivo(boolean activo) {
        if (tipo == TipoPlanSuscripcion.TRIAL && !activo) {
            throw new IllegalStateException("El plan TRIAL no puede desactivarse");
        }
        if (tipo == null) {
            throw new IllegalStateException("No se puede cambiar el estado de un plan sin tipo asignado");
        }
        this.activo = activo;
    }

    public void validarPropiedadesTrial() {
        if (!NOMBRE_PLAN_TRIAL.equals(nombre)
                || precioMensual == null
                || precioMensual.compareTo(BigDecimal.ZERO) != 0
                || !Objects.equals(maxNegocios, 1)
                || !activo) {
            throw new IllegalStateException(
                    "El plan TRIAL debe llamarse Prueba gratuita, tener precio cero, permitir un negocio y permanecer activo"
            );
        }
    }

    public void validarEliminable() {
        throw new IllegalStateException("Los planes de suscripción no pueden eliminarse físicamente");
    }

    private boolean esNombreReservadoTrial(String nombrePlan) {
        return nombrePlan != null && claveNombre(nombrePlan).equals(claveNombre(NOMBRE_PLAN_TRIAL));
    }

    public static String normalizarNombrePresentacion(String nombre) {
        if (nombre == null) {
            throw new IllegalArgumentException("El nombre del plan es obligatorio");
        }
        return Normalizer.normalize(nombre, Normalizer.Form.NFC)
                .replaceAll("(?U)\\s+", " ")
                .strip();
    }

    public static String claveNombre(String nombre) {
        return Normalizer.normalize(
                normalizarNombrePresentacion(nombre).toLowerCase(Locale.ROOT),
                Normalizer.Form.NFC
        );
    }

    public void normalizarNombrePreexistente() {
        this.nombre = normalizarNombrePresentacion(nombre);
        this.nombreNormalizado = claveNombre(nombre);
    }

    private void establecerNombre(String nombrePresentacion) {
        this.nombre = nombrePresentacion;
        this.nombreNormalizado = claveNombre(nombrePresentacion);
    }

    public void validarEditableComercial() {
        if (tipo != TipoPlanSuscripcion.COMERCIAL) {
            throw new IllegalStateException("Solo se pueden modificar planes COMERCIAL clasificados");
        }
    }

    private void validarPrecioComercial(BigDecimal precio) {
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio mensual de un plan comercial debe ser mayor que cero");
        }
        if (precio.precision() - precio.scale() > 8 || precio.scale() > 2) {
            throw new IllegalArgumentException("El precio admite hasta 8 enteros y 2 decimales");
        }
    }

    @PrePersist
    @PreUpdate
    private void validarAntesDeGuardar() {
        if (tipo == null) {
            throw new IllegalStateException("Todo plan debe tener un tipo asignado antes de guardarse");
        }
        if (nombreNormalizado == null || !nombreNormalizado.equals(claveNombre(nombre))) {
            throw new IllegalStateException("El nombre normalizado del plan no coincide con su nombre");
        }
        if (tipo == TipoPlanSuscripcion.TRIAL) {
            if (!Objects.equals(maxNegocios, 1)) {
                throw new IllegalStateException("El plan TRIAL debe permitir exactamente un negocio");
            }
            validarPropiedadesTrial();
        } else {
            if (esNombreReservadoTrial(nombre)) {
                throw new IllegalStateException("El nombre Prueba gratuita está reservado para el plan TRIAL");
            }
            if (precioMensual == null || precioMensual.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("El precio mensual de un plan comercial debe ser mayor que cero");
            }
            if (precioMensual.precision() - precioMensual.scale() > 8 || precioMensual.scale() > 2) {
                throw new IllegalStateException("El precio admite hasta 8 enteros y 2 decimales");
            }
            if (maxNegocios == null || maxNegocios < 1) {
                throw new IllegalStateException("La cantidad máxima de negocios debe ser mayor que cero");
            }
        }
    }

    @PreRemove
    private void impedirEliminacionFisica() {
        validarEliminable();
    }
}
