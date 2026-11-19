package com.dev.cutly.notificacion.entity;

import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacion_id")
    private Long notificacionId;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Column(nullable = false)
    private boolean leida;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    @Column(name = "tipo_evento", length = 50)
    private String tipoEvento;

    @Column(name = "negocio_id")
    private Long negocioId;

    @Column(name = "recurso_tipo", length = 40)
    private String recursoTipo;

    @Column(name = "recurso_id")
    private Long recursoId;

    @Column(name = "clave_evento", unique = true, length = 160)
    private String claveEvento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
