package com.dev.cutly.liquidacion.entity;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.liquidacion.enums.EstadoLiquidacion;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "liquidaciones_empleado", uniqueConstraints =
        @UniqueConstraint(name = "uk_liquidacion_idempotencia", columnNames = {"negocio_id", "clave_idempotencia"}))
@Getter
@NoArgsConstructor
public class LiquidacionEmpleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "liquidacion_id")
    private Long liquidacionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_registrante_id", nullable = false)
    private Usuario ownerRegistrante;
    @Column(name = "fecha_efectiva", nullable = false) private LocalDate fechaEfectiva;
    @Column(name = "fecha_registro", nullable = false) private LocalDateTime fechaRegistro;
    @Enumerated(EnumType.STRING) @Column(name = "metodo_pago", nullable = false, length = 30)
    private MetodoPago metodoPago;
    @Column(name = "descripcion_metodo", length = 100) private String descripcionMetodo;
    @Column(length = 500) private String observaciones;
    @Column(name = "importe_total", nullable = false, precision = 12, scale = 2) private BigDecimal importeTotal;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoLiquidacion estado;
    @Column(name = "motivo_anulacion", length = 500) private String motivoAnulacion;
    @Column(name = "fecha_anulacion") private LocalDateTime fechaAnulacion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_anulador_id") private Usuario ownerAnulador;
    @Column(name = "clave_idempotencia", nullable = false, length = 100) private String claveIdempotencia;
    @Column(name = "hash_solicitud", nullable = false, length = 64) private String hashSolicitud;
    @OneToMany(mappedBy = "liquidacion", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<AplicacionLiquidacion> aplicaciones = new ArrayList<>();

    public List<AplicacionLiquidacion> getAplicaciones() { return Collections.unmodifiableList(aplicaciones); }

    public LiquidacionEmpleado(Negocio negocio, Empleado empleado, Usuario owner, LocalDate fechaEfectiva,
                              MetodoPago metodoPago, String descripcionMetodo, String observaciones,
                              BigDecimal total, String clave, String hash) {
        this.negocio = negocio; this.empleado = empleado; this.ownerRegistrante = owner;
        this.fechaEfectiva = fechaEfectiva; this.fechaRegistro = LocalDateTime.now();
        this.metodoPago = metodoPago; this.descripcionMetodo = descripcionMetodo; this.observaciones = observaciones;
        this.importeTotal = total; this.estado = EstadoLiquidacion.VIGENTE;
        this.claveIdempotencia = clave; this.hashSolicitud = hash;
    }

    public void agregar(AplicacionLiquidacion aplicacion) {
        if (liquidacionId != null) throw new IllegalStateException("No se pueden modificar aplicaciones de una liquidación persistida");
        if (aplicacion == null || aplicacion.getLiquidacion() != this) throw new IllegalArgumentException("La aplicación debe pertenecer a esta liquidación");
        aplicaciones.add(aplicacion);
    }

    @PrePersist
    private void validarIntegridad() {
        if (aplicaciones.isEmpty() || aplicaciones.stream().anyMatch(a -> a == null || a.getImporte() == null))
            throw new IllegalStateException("La liquidación debe incluir aplicaciones válidas");
        BigDecimal suma = aplicaciones.stream().map(AplicacionLiquidacion::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (importeTotal == null || importeTotal.compareTo(suma) != 0)
            throw new IllegalStateException("El total de la liquidación debe coincidir con sus aplicaciones");
    }
    public void anular(String motivo, Usuario owner) {
        if (estado != EstadoLiquidacion.VIGENTE) throw new IllegalStateException("La liquidación ya fue anulada");
        estado = EstadoLiquidacion.ANULADA; motivoAnulacion = motivo;
        fechaAnulacion = LocalDateTime.now(); ownerAnulador = owner;
    }
}
