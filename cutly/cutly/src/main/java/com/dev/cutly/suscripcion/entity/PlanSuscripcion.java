package com.dev.cutly.suscripcion.entity;

import com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "planes_suscripcion")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlanSuscripcion {

    private static final String NOMBRE_PLAN_TRIAL = "Prueba gratuita";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Column(nullable = false)
    private boolean activo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = true, length = 20)
    private TipoPlanSuscripcion tipo;

    public static PlanSuscripcion crearPlanTrial() {
        PlanSuscripcion plan = new PlanSuscripcion();
        plan.nombre = NOMBRE_PLAN_TRIAL;
        plan.precioMensual = BigDecimal.ZERO;
        plan.activo = true;
        plan.tipo = TipoPlanSuscripcion.TRIAL;
        return plan;
    }

    public static PlanSuscripcion crearPlanComercial(String nombre, String descripcion, BigDecimal precioMensual) {
        PlanSuscripcion plan = new PlanSuscripcion();
        plan.tipo = TipoPlanSuscripcion.COMERCIAL;
        plan.actualizarDatosComerciales(nombre, descripcion, precioMensual);
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

    public void clasificarComoComercial() {
        if (tipo != null && tipo != TipoPlanSuscripcion.COMERCIAL) {
            throw new IllegalStateException("Un plan TRIAL no puede convertirse en COMERCIAL");
        }
        if (nombre == null || nombre.isBlank() || nombre.length() > 50) {
            throw new IllegalStateException("Un plan preexistente tiene un nombre inválido");
        }
        if (esNombreReservadoTrial(nombre)) {
            throw new IllegalStateException("El nombre Prueba gratuita está reservado para el plan TRIAL");
        }
        tipo = TipoPlanSuscripcion.COMERCIAL;
    }

    public void actualizarDatosComerciales(String nombre, String descripcion, BigDecimal precioMensual) {
        if (tipo == TipoPlanSuscripcion.TRIAL) {
            throw new IllegalStateException("El plan TRIAL no puede cambiar sus datos comerciales");
        }
        if (tipo == null) {
            throw new IllegalStateException("No se puede editar un plan sin tipo asignado");
        }
        if (nombre == null || nombre.isBlank() || nombre.length() > 50) {
            throw new IllegalArgumentException("El nombre del plan debe contener entre 1 y 50 caracteres");
        }
        if (esNombreReservadoTrial(nombre)) {
            throw new IllegalArgumentException("El nombre Prueba gratuita está reservado para el plan TRIAL");
        }
        if (descripcion != null && descripcion.length() > 500) {
            throw new IllegalArgumentException("La descripción del plan no puede superar los 500 caracteres");
        }
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioMensual = Objects.requireNonNull(precioMensual, "El precio mensual es obligatorio");
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
                || !activo) {
            throw new IllegalStateException(
                    "El plan TRIAL debe llamarse Prueba gratuita, tener precio cero y permanecer activo"
            );
        }
    }

    public void validarEliminable() {
        if (tipo == TipoPlanSuscripcion.TRIAL) {
            throw new IllegalStateException("El plan TRIAL no puede eliminarse");
        }
        if (tipo == null) {
            throw new IllegalStateException("No se puede eliminar un plan sin tipo asignado");
        }
    }

    private boolean esNombreReservadoTrial(String nombrePlan) {
        return nombrePlan != null && NOMBRE_PLAN_TRIAL.equalsIgnoreCase(nombrePlan.trim());
    }

    @PrePersist
    @PreUpdate
    private void validarAntesDeGuardar() {
        if (tipo == null) {
            throw new IllegalStateException("Todo plan debe tener un tipo asignado antes de guardarse");
        }
        if (tipo == TipoPlanSuscripcion.TRIAL) {
            validarPropiedadesTrial();
        } else if (esNombreReservadoTrial(nombre)) {
            throw new IllegalStateException("El nombre Prueba gratuita está reservado para el plan TRIAL");
        }
    }

    @PreRemove
    private void impedirEliminacionTrial() {
        if (tipo == TipoPlanSuscripcion.TRIAL) {
            throw new IllegalStateException("El plan TRIAL no puede eliminarse");
        }
    }
}
