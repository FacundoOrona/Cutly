package com.dev.cutly.usuario.entity;

import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long usuarioId;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 2, max = 20, message = "El nombre debe tener entre 2 y 20 caracteres")
    @Column(nullable = false, length = 20)
    private String nombre;

    @NotBlank(message = "El apellido no puede estar vacío")
    @Size(min = 2, max = 20, message = "El apellido debe tener entre 2 y 20 caracteres")
    @Column(nullable = false, length = 20)
    private String apellido;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "El dni no puede estar vacio")
    @Size(min = 7, max = 9, message = "El dni debe contener entre 7 y 9 caracteres")
    private String dni;

    @NotBlank(message = "El email no puede estar vacio")
    @Email(message = "El formato del email no es valido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacia")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsuarioStatus status;
}
