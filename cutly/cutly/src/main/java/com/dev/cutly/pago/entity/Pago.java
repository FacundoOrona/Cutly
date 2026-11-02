package com.dev.cutly.pago.entity;

import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos", uniqueConstraints = @UniqueConstraint(name = "uk_pago_turno", columnNames = "turno_id"))
@Check(constraints = "monto > 0 and ((resultado_financiero_id is not null and deuda_id is null and turno_id is not null) or (resultado_financiero_id is null and deuda_id is not null and turno_id is null))")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pago_id")
    private Long pagoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private LocalDateTime fechaPago;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deuda_id")
    private DeudaCliente deuda;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resultado_financiero_id", unique = true)
    private ResultadoFinancieroTurno resultadoFinanciero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_registrante_id", nullable = false)
    private Usuario ownerRegistrante;

    @Column(length = 100)
    private String descripcionOtro;

    @PrePersist
    @PreUpdate
    void validarOrigen() {
        if ((resultadoFinanciero == null) == (deuda == null)) {
            throw new IllegalStateException("El pago debe ser cobro directo o abono de una deuda");
        }
        if (resultadoFinanciero != null && (turno == null || resultadoFinanciero.getTipoResultado() != com.dev.cutly.pago.enums.DecisionFinanciera.COBRO_DIRECTO || !turno.getTurnoId().equals(resultadoFinanciero.getTurno().getTurnoId()) || !turno.getNegocio().getNegocioId().equals(resultadoFinanciero.getNegocio().getNegocioId()) || ownerRegistrante == null || !ownerRegistrante.getUsuarioId().equals(resultadoFinanciero.getNegocio().getOwner().getUsuarioId()))) throw new IllegalStateException("El cobro debe corresponder a un resultado directo del mismo turno, negocio y owner");
        if (deuda != null && turno != null) throw new IllegalStateException("Un abono no puede vincular un turno directo");
        if (deuda != null && (ownerRegistrante == null || !ownerRegistrante.getUsuarioId().equals(deuda.getNegocio().getOwner().getUsuarioId()))) throw new IllegalStateException("El abono debe ser registrado por el owner del negocio de la deuda");
        if (monto == null || monto.signum() <= 0) throw new IllegalStateException("El monto debe ser positivo");
        if (descripcionOtro != null && (metodoPago != MetodoPago.OTRO || descripcionOtro.length() > 100)) {
            throw new IllegalStateException("La descripción solo corresponde al método OTRO y admite hasta 100 caracteres");
        }
    }
}
