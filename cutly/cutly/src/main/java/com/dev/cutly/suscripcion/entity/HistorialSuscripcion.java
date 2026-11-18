package com.dev.cutly.suscripcion.entity;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.enums.TipoEventoSuscripcion;
import com.dev.cutly.usuario.entity.Usuario;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoSuscripcion tipoEvento;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "fecha_inicio_periodo")
    private LocalDate fechaInicioPeriodo;

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
            LocalDate fechaVencimientoAnterior,
            LocalDate fechaVencimientoNueva,
            SuscripcionStatus estadoAnterior,
            SuscripcionStatus estadoNuevo,
            Usuario administradorResponsable,
            String identificacionSistema,
            String observacion
    ) {
        HistorialSuscripcion historial = new HistorialSuscripcion();
        historial.suscripcion = suscripcion;
        historial.tipoEvento = tipoEvento;
        historial.fechaEvento = LocalDateTime.now();
        historial.fechaInicioPeriodo = fechaInicioPeriodo;
        historial.fechaVencimientoAnterior = fechaVencimientoAnterior;
        historial.fechaVencimientoNueva = fechaVencimientoNueva;
        historial.estadoAnterior = estadoAnterior;
        historial.estadoNuevo = estadoNuevo;
        historial.administradorResponsable = administradorResponsable;
        historial.identificacionSistema = identificacionSistema;
        historial.observacion = observacion;
        return historial;
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
