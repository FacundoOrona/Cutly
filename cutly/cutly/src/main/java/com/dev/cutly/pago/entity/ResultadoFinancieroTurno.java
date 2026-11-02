package com.dev.cutly.pago.entity;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "resultados_financieros_turno", uniqueConstraints = @UniqueConstraint(name = "uk_resultado_turno", columnNames = "turno_id"))
@Checks({
        @Check(constraints = "(cliente_id is null and cliente_local_id is not null) or (cliente_id is not null and cliente_local_id is null)"),
        @Check(constraints = "(tipo_resultado = 'GRATUIDAD' and monto_acordado = 0) or (tipo_resultado <> 'GRATUIDAD' and monto_acordado > 0)"),
        @Check(constraints = "(tipo_resultado = 'GRATUIDAD' and motivo_gratuidad is not null and length(trim(motivo_gratuidad)) between 3 and 250) or (tipo_resultado <> 'GRATUIDAD' and motivo_gratuidad is null)")
})
@Getter @Setter @NoArgsConstructor
public class ResultadoFinancieroTurno {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "resultado_financiero_id")
    private Long resultadoFinancieroId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "turno_id", nullable = false, unique = true)
    private Turno turno;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cliente_id") private Usuario cliente;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cliente_local_id") private ClienteLocal clienteLocal;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id", nullable = false) private Usuario owner;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_resultado", nullable = false, length = 30) private DecisionFinanciera tipoResultado;
    @Column(name = "monto_acordado", nullable = false, precision = 10, scale = 2) private BigDecimal montoAcordado;
    @Column(name = "fecha_registro", nullable = false) private LocalDateTime fechaRegistro;
    @Column(name = "motivo_gratuidad", length = 250) private String motivoGratuidad;
    @PrePersist @PreUpdate void validar() {
        if ((cliente == null) == (clienteLocal == null)) throw new IllegalStateException("Debe existir exactamente un cliente");
        if (turno == null || negocio == null || owner == null || !turno.getNegocio().getNegocioId().equals(negocio.getNegocioId()) || !negocio.getOwner().getUsuarioId().equals(owner.getUsuarioId()) || !java.util.Objects.equals(turno.getCliente() == null ? null : turno.getCliente().getUsuarioId(), cliente == null ? null : cliente.getUsuarioId()) || !java.util.Objects.equals(turno.getClienteLocal() == null ? null : turno.getClienteLocal().getClienteLocalId(), clienteLocal == null ? null : clienteLocal.getClienteLocalId())) throw new IllegalStateException("El resultado debe conservar el negocio, owner y cliente del turno");
        if (tipoResultado == DecisionFinanciera.GRATUIDAD) {
            if (montoAcordado == null || montoAcordado.signum() != 0 || motivoGratuidad == null || motivoGratuidad.trim().length() < 3 || motivoGratuidad.trim().length() > 250) throw new IllegalStateException("Gratuidad requiere monto cero y motivo válido");
        } else if (montoAcordado == null || montoAcordado.signum() <= 0 || motivoGratuidad != null) throw new IllegalStateException("El resultado requiere importe positivo y no admite motivo");
    }
}
