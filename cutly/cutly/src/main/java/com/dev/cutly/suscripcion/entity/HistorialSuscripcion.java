package com.dev.cutly.suscripcion.entity;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.enums.TipoEventoSuscripcion;
import com.dev.cutly.suscripcion.service.RelojSuscripcion;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "historial_suscripciones")
@Immutable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class HistorialSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_suscripcion_id")
    private Long historialSuscripcionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @Column(name = "plan_id_anterior")
    private Long planIdAnterior;

    @Column(name = "nombre_plan_anterior", length = 50)
    private String nombrePlanAnterior;

    @Column(name = "plan_id_nuevo")
    private Long planIdNuevo;

    @Column(name = "nombre_plan_nuevo", length = 50)
    private String nombrePlanNuevo;

    @Column(name = "precio_contratado_anterior", precision = 10, scale = 2)
    private BigDecimal precioContratadoAnterior;

    @Column(name = "precio_contratado_nuevo", precision = 10, scale = 2)
    private BigDecimal precioContratadoNuevo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoSuscripcion tipoEvento;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "fecha_inicio_periodo")
    private LocalDate fechaInicioPeriodo;

    @Column(name = "fecha_inicio_anterior")
    private LocalDate fechaInicioAnterior;

    @Column(name = "fecha_inicio_nueva")
    private LocalDate fechaInicioNueva;

    @Column(name = "fecha_vencimiento_anterior")
    private LocalDate fechaVencimientoAnterior;

    @Column(name = "fecha_vencimiento_nueva")
    private LocalDate fechaVencimientoNueva;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private SuscripcionStatus estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", length = 20)
    private SuscripcionStatus estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_id")
    private Usuario administradorResponsable;

    @Column(name = "identificacion_sistema", length = 100)
    private String identificacionSistema;

    @Column(length = 1000)
    private String observacion;

    public static HistorialSuscripcion registrar(
            Suscripcion suscripcion,
            TipoEventoSuscripcion tipoEvento,
            LocalDate fechaInicioPeriodo,
            SnapshotSuscripcion anterior,
            SnapshotSuscripcion nuevo,
            Usuario administradorResponsable,
            String identificacionSistema,
            String observacion,
            LocalDateTime fechaEvento
    ) {
        validarActor(administradorResponsable, identificacionSistema, tipoEvento);
        if (suscripcion == null || tipoEvento == null || fechaEvento == null) {
            throw new IllegalArgumentException("Suscripción, evento y fecha del evento son obligatorios");
        }
        HistorialSuscripcion historial = new HistorialSuscripcion();
        historial.suscripcion = suscripcion;
        historial.tipoEvento = tipoEvento;
        historial.fechaEvento = fechaEvento;
        historial.fechaInicioPeriodo = fechaInicioPeriodo;
        historial.aplicarSnapshotAnterior(anterior);
        historial.aplicarSnapshotNuevo(nuevo);
        historial.administradorResponsable = administradorResponsable;
        historial.identificacionSistema = identificacionSistema;
        historial.observacion = observacion;
        return historial;
    }

    private void aplicarSnapshotAnterior(SnapshotSuscripcion snapshot) {
        if (snapshot == null) return;
        planIdAnterior = snapshot.planId();
        nombrePlanAnterior = snapshot.nombrePlan();
        precioContratadoAnterior = snapshot.precioContratado();
        fechaInicioAnterior = snapshot.fechaInicio();
        fechaVencimientoAnterior = snapshot.fechaVencimiento();
        estadoAnterior = snapshot.estado();
    }

    private void aplicarSnapshotNuevo(SnapshotSuscripcion snapshot) {
        if (snapshot == null) return;
        planIdNuevo = snapshot.planId();
        nombrePlanNuevo = snapshot.nombrePlan();
        precioContratadoNuevo = snapshot.precioContratado();
        fechaInicioNueva = snapshot.fechaInicio();
        fechaVencimientoNueva = snapshot.fechaVencimiento();
        estadoNuevo = snapshot.estado();
    }

    private static void validarActor(Usuario administrador, String sistema, TipoEventoSuscripcion tipoEvento) {
        boolean administradorPresente = administrador != null;
        boolean sistemaPresente = sistema != null;
        boolean rolValido = administradorPresente
                && (administrador.getRol() == Rol.SUPER_ADMIN
                || (administrador.getRol() == Rol.OWNER && tipoEvento == TipoEventoSuscripcion.RENEWED));
        if (administradorPresente == sistemaPresente
                || (administradorPresente && (administrador.getUsuarioId() == null
                || !rolValido))
                || (sistemaPresente && !RelojSuscripcion.IDENTIFICACION_SISTEMA.equals(sistema))) {
            throw new IllegalArgumentException(
                    "El evento debe tener un actor autorizado o el actor SISTEMA"
            );
        }
    }

    @PrePersist
    private void validarAntesDeCrear() {
        validarActor(administradorResponsable, identificacionSistema, tipoEvento);
        if (suscripcion == null || tipoEvento == null || fechaEvento == null) {
            throw new IllegalStateException("El evento de historial está incompleto");
        }
    }

    @PreUpdate
    private void impedirActualizacion() {
        throw new IllegalStateException("El historial de suscripciones es inmutable");
    }

    @PreRemove
    private void impedirEliminacion() {
        throw new IllegalStateException("El historial de suscripciones no se puede eliminar");
    }
}
