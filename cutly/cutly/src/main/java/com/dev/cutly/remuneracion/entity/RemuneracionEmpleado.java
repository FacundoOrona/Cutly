package com.dev.cutly.remuneracion.entity;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.pago.entity.ResultadoFinancieroTurno;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.remuneracion.enums.EstadoRemuneracion;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "remuneraciones_empleado", uniqueConstraints =
        @UniqueConstraint(name = "uk_remuneracion_turno", columnNames = "turno_id"))
@Check(constraints = "(porcentaje_aplicado is null or porcentaje_aplicado between 0 and 100) "
        + "and (porcentaje_excepcion_preservado is null or porcentaje_excepcion_preservado between 0 and 100) "
        + "and (porcentaje_comision_pendiente is null or porcentaje_comision_pendiente between 0 and 100) "
        + "and (porcentaje_comision_resuelto_manualmente is null or porcentaje_comision_resuelto_manualmente between 0 and 100) "
        + "and (precio_aplicado is null or precio_aplicado >= 0) "
        + "and (monto_calculado is null or monto_calculado >= 0)")
@Getter
@NoArgsConstructor
public class RemuneracionEmpleado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "remuneracion_id")
    private Long remuneracionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false, unique = true)
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resultado_financiero_id", unique = true)
    private ResultadoFinancieroTurno resultadoFinanciero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoRemuneracion estado;

    @Column(name = "precio_turno_original", precision = 10, scale = 2)
    private BigDecimal precioTurnoOriginal;

    @Column(name = "precio_aplicado", precision = 10, scale = 2)
    private BigDecimal precioAplicado;

    @Column(name = "porcentaje_aplicado", precision = 5, scale = 2)
    private BigDecimal porcentajeAplicado;

    @Column(name = "monto_calculado", precision = 10, scale = 2)
    private BigDecimal montoCalculado;

    @Column(name = "porcentaje_excepcion_preservado", precision = 5, scale = 2)
    private BigDecimal porcentajeExcepcionPreservado;

    @Column(name = "porcentaje_comision_pendiente", precision = 5, scale = 2)
    private BigDecimal porcentajeComisionPendiente;

    @Column(name = "requiere_revision_comision", nullable = false)
    private boolean requiereRevisionComision;

    @Column(name = "detalle_revision", length = 250)
    private String detalleRevision;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "precio_corregido_manualmente", nullable = false)
    private boolean precioCorregidoManualmente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_corrector_id")
    private Usuario ownerCorrector;

    @Column(name = "fecha_correccion_precio")
    private LocalDateTime fechaCorreccionPrecio;

    @Column(name = "motivo_correccion_precio", length = 250)
    private String motivoCorreccionPrecio;

    @Column(name = "porcentaje_comision_resuelto_manualmente", precision = 5, scale = 2)
    private BigDecimal porcentajeComisionResueltoManualmente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_resolutor_comision_id")
    private Usuario ownerResolutorComision;

    @Column(name = "fecha_resolucion_comision")
    private LocalDateTime fechaResolucionComision;

    @Column(name = "motivo_resolucion_comision", length = 250)
    private String motivoResolucionComision;

    @PrePersist
    @PreUpdate
    private void validarIntegridad() {
        if (turno == null || empleado == null || negocio == null
                || turno.getEmpleado() == null || turno.getNegocio() == null
                || !empleado.getEmpleadoId().equals(turno.getEmpleado().getEmpleadoId())
                || !negocio.getNegocioId().equals(turno.getNegocio().getNegocioId())) {
            throw new IllegalStateException("La remuneración debe corresponder al empleado y negocio del turno");
        }
        if (resultadoFinanciero != null && (resultadoFinanciero.getTurno() == null
                || !turno.getTurnoId().equals(resultadoFinanciero.getTurno().getTurnoId())
                || !negocio.getNegocioId().equals(resultadoFinanciero.getNegocio().getNegocioId()))) {
            throw new IllegalStateException("El resultado financiero debe pertenecer al mismo turno y negocio");
        }
        switch (estado) {
            case PENDIENTE_DEFINICION_FINANCIERA -> {
                if (resultadoFinanciero != null || precioAplicado != null || porcentajeAplicado != null
                        || montoCalculado != null || fechaConfirmacion != null || requiereRevisionComision) {
                    throw new IllegalStateException("La remuneración pendiente no debe contener una decisión financiera");
                }
            }
            case PENDIENTE_REVISION_MANUAL -> {
                if (resultadoFinanciero == null || fechaConfirmacion != null) {
                    throw new IllegalStateException("La revisión manual debe corresponder a un resultado financiero pendiente");
                }
                if (porcentajeComisionPendiente != null && requiereRevisionComision) {
                    throw new IllegalStateException("Una comisión preservada no debe seguir pendiente de revisión");
                }
            }
            case CONFIRMADA -> {
                if (resultadoFinanciero == null || resultadoFinanciero.getTipoResultado() == DecisionFinanciera.GRATUIDAD
                        || precioAplicado == null || porcentajeAplicado == null || montoCalculado == null
                        || fechaConfirmacion == null || requiereRevisionComision || porcentajeComisionPendiente != null) {
                    throw new IllegalStateException("La remuneración confirmada requiere snapshots completos");
                }
                if (precioCorregidoManualmente && (precioTurnoOriginal != null || ownerCorrector == null
                        || fechaCorreccionPrecio == null || motivoCorreccionPrecio == null
                        || motivoCorreccionPrecio.isBlank())) {
                    throw new IllegalStateException("La corrección manual de precio requiere su auditoría completa");
                }
                if (!precioCorregidoManualmente && (precioTurnoOriginal == null
                        || precioTurnoOriginal.compareTo(precioAplicado) != 0)) {
                    throw new IllegalStateException("El precio aplicado debe conservar el precio histórico del turno");
                }
                if (porcentajeComisionResueltoManualmente != null && (ownerResolutorComision == null
                        || fechaResolucionComision == null || motivoResolucionComision == null
                        || motivoResolucionComision.isBlank() || porcentajeAplicado.compareTo(porcentajeComisionResueltoManualmente) != 0)) {
                    throw new IllegalStateException("La resolución manual de comisión requiere auditoría completa");
                }
            }
            case SIN_REMUNERACION -> {
                if (resultadoFinanciero == null
                        || resultadoFinanciero.getTipoResultado() != DecisionFinanciera.GRATUIDAD
                        || precioAplicado != null || porcentajeAplicado != null || montoCalculado != null
                        || fechaConfirmacion != null || requiereRevisionComision) {
                    throw new IllegalStateException("SIN_REMUNERACION solo corresponde a gratuidad");
                }
            }
        }
        if (estado != EstadoRemuneracion.CONFIRMADA && (porcentajeComisionResueltoManualmente != null
                || ownerResolutorComision != null || fechaResolucionComision != null
                || motivoResolucionComision != null)) {
            throw new IllegalStateException("La auditoría de comisión manual solo corresponde a una remuneración confirmada");
        }
    }

    public static RemuneracionEmpleado pendiente(Turno turno, LocalDateTime fecha) {
        if (turno.getEmpleado() == null) return null;
        RemuneracionEmpleado r = new RemuneracionEmpleado();
        r.turno = turno;
        r.empleado = turno.getEmpleado();
        r.negocio = turno.getNegocio();
        r.precioTurnoOriginal = turno.getPrecio();
        r.estado = EstadoRemuneracion.PENDIENTE_DEFINICION_FINANCIERA;
        r.fechaGeneracion = fecha;
        return r;
    }

    public static RemuneracionEmpleado pendienteRevisionAnterior(Turno turno, ResultadoFinancieroTurno resultado,
                                                                  LocalDateTime fecha) {
        RemuneracionEmpleado r = pendiente(turno, fecha);
        if (r == null) return null;
        r.resultadoFinanciero = resultado;
        r.estado = EstadoRemuneracion.PENDIENTE_REVISION_MANUAL;
        r.requiereRevisionComision = true;
        r.detalleRevision = "La decisión financiera es anterior al registro de snapshots de remuneración";
        return r;
    }

    public void preservarExcepcion(BigDecimal porcentaje) {
        exigirEstado(EstadoRemuneracion.PENDIENTE_DEFINICION_FINANCIERA);
        if (porcentaje != null && porcentajeExcepcionPreservado == null) {
            porcentajeExcepcionPreservado = porcentaje;
        }
    }

    public void pendienteRevision(ResultadoFinancieroTurno resultado, String detalle, boolean revisionComision) {
        exigirPendienteFinanciera();
        resultadoFinanciero = resultado;
        estado = EstadoRemuneracion.PENDIENTE_REVISION_MANUAL;
        requiereRevisionComision = revisionComision;
        detalleRevision = detalle;
    }

    public void preservarPorcentajePendiente(BigDecimal porcentaje) {
        if (estado != EstadoRemuneracion.PENDIENTE_REVISION_MANUAL || porcentaje == null) {
            throw new IllegalStateException("No se puede preservar la comisión para esta revisión");
        }
        porcentajeComisionPendiente = porcentaje;
    }

    public void marcarComisionEnRevision(String detalle) {
        if (estado != EstadoRemuneracion.PENDIENTE_REVISION_MANUAL) {
            throw new IllegalStateException("La remuneración no está pendiente de revisión manual");
        }
        requiereRevisionComision = true;
        detalleRevision = detalle;
    }

    public void confirmar(ResultadoFinancieroTurno resultado, BigDecimal precio, BigDecimal porcentaje,
                          BigDecimal monto, LocalDateTime fecha) {
        exigirPendienteFinanciera();
        if (precio == null || porcentaje == null || monto == null || requiereRevisionComision) {
            throw new IllegalStateException("Faltan datos para confirmar la remuneración");
        }
        resultadoFinanciero = resultado;
        precioAplicado = precio;
        porcentajeAplicado = porcentaje;
        montoCalculado = monto;
        fechaConfirmacion = fecha;
        estado = EstadoRemuneracion.CONFIRMADA;
        detalleRevision = null;
        porcentajeExcepcionPreservado = null;
        porcentajeComisionPendiente = null;
    }

    public void corregirPrecio(BigDecimal precio, Usuario owner, String motivo, LocalDateTime fecha) {
        if (estado != EstadoRemuneracion.PENDIENTE_REVISION_MANUAL || precioAplicado != null
                || precioCorregidoManualmente) {
            throw new IllegalStateException("La remuneración no admite corrección manual de precio");
        }
        precioAplicado = precio;
        ownerCorrector = owner;
        motivoCorreccionPrecio = motivo;
        fechaCorreccionPrecio = fecha;
        precioCorregidoManualmente = true;
    }

    public void confirmarPrecioCorregido(BigDecimal porcentaje, BigDecimal monto, LocalDateTime fecha) {
        if (estado != EstadoRemuneracion.PENDIENTE_REVISION_MANUAL || !precioCorregidoManualmente
                || requiereRevisionComision || porcentaje == null || monto == null) {
            throw new IllegalStateException("La remuneración corregida no puede confirmarse");
        }
        porcentajeAplicado = porcentaje;
        montoCalculado = monto;
        fechaConfirmacion = fecha;
        estado = EstadoRemuneracion.CONFIRMADA;
        detalleRevision = null;
        porcentajeExcepcionPreservado = null;
        porcentajeComisionPendiente = null;
    }

    public void resolverComisionManualmente(BigDecimal porcentaje, Usuario owner, String motivo,
                                             LocalDateTime fecha, BigDecimal monto) {
        if (estado != EstadoRemuneracion.PENDIENTE_REVISION_MANUAL || !requiereRevisionComision
                || porcentajeExcepcionPreservado != null || (precioAplicado == null && precioTurnoOriginal == null)
                || porcentaje == null || monto == null || owner == null || motivo == null || motivo.isBlank()) {
            throw new IllegalStateException("La remuneración no admite resolución manual de comisión");
        }
        if (precioAplicado == null) precioAplicado = precioTurnoOriginal;
        porcentajeComisionResueltoManualmente = porcentaje;
        ownerResolutorComision = owner;
        fechaResolucionComision = fecha;
        motivoResolucionComision = motivo;
        porcentajeAplicado = porcentaje;
        montoCalculado = monto;
        fechaConfirmacion = fecha;
        requiereRevisionComision = false;
        estado = EstadoRemuneracion.CONFIRMADA;
        detalleRevision = null;
        porcentajeExcepcionPreservado = null;
        porcentajeComisionPendiente = null;
    }

    public void cerrarSinRemuneracion(ResultadoFinancieroTurno resultado) {
        exigirPendienteFinanciera();
        resultadoFinanciero = resultado;
        estado = EstadoRemuneracion.SIN_REMUNERACION;
        detalleRevision = null;
        porcentajeExcepcionPreservado = null;
        porcentajeComisionPendiente = null;
    }

    private void exigirPendienteFinanciera() {
        exigirEstado(EstadoRemuneracion.PENDIENTE_DEFINICION_FINANCIERA);
    }

    private void exigirEstado(EstadoRemuneracion esperado) {
        if (estado != esperado) throw new IllegalStateException("Transición de remuneración incompatible");
    }
}
