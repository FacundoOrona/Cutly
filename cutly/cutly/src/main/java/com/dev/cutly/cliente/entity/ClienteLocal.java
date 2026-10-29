package com.dev.cutly.cliente.entity;

import com.dev.cutly.negocio.entity.Negocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "clientes_locales",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cliente_local_negocio_dni_normalizado",
                columnNames = {"negocio_id", "dni_normalizado"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class ClienteLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cliente_local_id")
    @ToString.Include
    private Long clienteLocalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(nullable = false, length = 20)
    private String nombre;

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(nullable = false, length = 20)
    private String apellido;

    @NotBlank
    @Size(max = 20)
    @Column(name = "dni_presentado", nullable = false, length = 20)
    private String dniPresentado;

    @NotBlank
    @Size(min = 7, max = 9)
    @Column(name = "dni_normalizado", nullable = false, length = 9)
    private String dniNormalizado;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String telefono;
}
