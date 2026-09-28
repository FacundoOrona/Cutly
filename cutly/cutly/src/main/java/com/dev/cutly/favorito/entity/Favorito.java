package com.dev.cutly.favorito.entity;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "favoritos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cliente_negocio_favorito",
                        columnNames = {"cliente_id", "negocio_id"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorito_id")
    private Long favoritoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;
}