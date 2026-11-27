package com.dev.cutly.liquidacion.entity;

import com.dev.cutly.remuneracion.entity.RemuneracionEmpleado;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "aplicaciones_liquidacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_aplicacion_liquidacion_remuneracion", columnNames = {"liquidacion_id", "remuneracion_id"}),
        indexes = @Index(name = "ix_aplicacion_remuneracion", columnList = "remuneracion_id"))
@Getter @NoArgsConstructor
public class AplicacionLiquidacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aplicacion_liquidacion_id") private Long aplicacionLiquidacionId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "liquidacion_id", nullable = false)
    private LiquidacionEmpleado liquidacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "remuneracion_id", nullable = false)
    private RemuneracionEmpleado remuneracion;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal importe;
    @PrePersist @PreUpdate
    private void validar() {
        if (liquidacion == null || remuneracion == null || importe == null || importe.signum() <= 0
                || !liquidacion.getNegocio().getNegocioId().equals(remuneracion.getNegocio().getNegocioId())
                || !liquidacion.getEmpleado().getEmpleadoId().equals(remuneracion.getEmpleado().getEmpleadoId())) {
            throw new IllegalStateException("La aplicación debe corresponder a una remuneración positiva del empleado y negocio liquidados");
        }
    }
    public AplicacionLiquidacion(LiquidacionEmpleado liquidacion, RemuneracionEmpleado remuneracion, BigDecimal importe) {
        this.liquidacion = liquidacion; this.remuneracion = remuneracion; this.importe = importe;
    }
}
