package com.dev.cutly.pago.entity;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "deudas_cliente", uniqueConstraints = @UniqueConstraint(name = "uk_deuda_resultado", columnNames = "resultado_financiero_id"))
@Checks({
        @Check(constraints = "monto_original > 0"),
        @Check(constraints = "(cliente_id is null and cliente_local_id is not null) or (cliente_id is not null and cliente_local_id is null)")
})
@Getter @Setter @NoArgsConstructor
public class DeudaCliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "deuda_id") private Long deudaId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "resultado_financiero_id", nullable = false, unique = true) private ResultadoFinancieroTurno resultadoFinanciero;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "negocio_id", nullable = false) private Negocio negocio;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cliente_id") private Usuario cliente;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cliente_local_id") private ClienteLocal clienteLocal;
    @Column(name = "monto_original", nullable = false, precision = 10, scale = 2) private BigDecimal montoOriginal;
    @Column(name = "fecha_origen", nullable = false) private LocalDateTime fechaOrigen;
    @PrePersist @PreUpdate void validar() {
        if ((cliente == null) == (clienteLocal == null) || montoOriginal == null || montoOriginal.signum() <= 0) throw new IllegalStateException("Deuda inválida");
        if (resultadoFinanciero == null || resultadoFinanciero.getTipoResultado() != com.dev.cutly.pago.enums.DecisionFinanciera.CUENTA_CORRIENTE || negocio == null || !negocio.getNegocioId().equals(resultadoFinanciero.getNegocio().getNegocioId()) || !java.util.Objects.equals(cliente == null ? null : cliente.getUsuarioId(), resultadoFinanciero.getCliente() == null ? null : resultadoFinanciero.getCliente().getUsuarioId()) || !java.util.Objects.equals(clienteLocal == null ? null : clienteLocal.getClienteLocalId(), resultadoFinanciero.getClienteLocal() == null ? null : resultadoFinanciero.getClienteLocal().getClienteLocalId()) || montoOriginal.compareTo(resultadoFinanciero.getMontoAcordado()) != 0) throw new IllegalStateException("La deuda debe reflejar un resultado de cuenta corriente del mismo negocio y cliente");
    }
}
