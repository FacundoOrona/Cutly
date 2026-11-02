package com.dev.cutly.pago.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "idempotencia_abono", uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotencia_abono_key", columnNames = "idempotency_key"))
@Getter @Setter @NoArgsConstructor
public class IdempotenciaAbono {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String key;

    @Column(name = "solicitud_hash", nullable = false, length = 64)
    private String requestHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deuda_id", nullable = false)
    private DeudaCliente deuda;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false, unique = true)
    private Pago pago;
}
