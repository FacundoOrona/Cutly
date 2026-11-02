package com.dev.cutly.pago.entity;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.turno.entity.Turno;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "idempotencia_financiera", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idempotencia_key", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_idempotencia_turno", columnNames = {"negocio_id", "turno_id"}),
        @UniqueConstraint(name = "uk_idempotencia_resultado", columnNames = "resultado_id")
})
@Getter @Setter @NoArgsConstructor
public class IdempotenciaFinanciera {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "idempotency_key", nullable = false, length = 100) private String key;
    @Column(name = "solicitud_hash", nullable = false, length = 64) private String requestHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "negocio_id", nullable = false) private Negocio negocio;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "turno_id", nullable = false) private Turno turno;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "resultado_id", nullable = false) private ResultadoFinancieroTurno resultado;
}
