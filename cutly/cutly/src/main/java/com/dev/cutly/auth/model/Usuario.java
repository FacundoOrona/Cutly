package com.dev.cutly.auth.model;

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

    @NotBlank(message = "El nombre de usuario no debe estar vacio")
    @Column(nullable = false)
    @Size(max = 20, min = 5, message = "El nombre de usuario debe contener entre 5 y 20 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 2, max = 20, message = "El nombre debe tener entre 2 y 20 caracteres")
    @Column(nullable = false, length = 20)
    private String nombre;

    @NotBlank(message = "El apellido no puede estar vacío")
    @Size(min = 2, max = 20, message = "El apellido debe tener entre 2 y 20 caracteres")
    @Column(nullable = false, length = 20)
    private String apellido;

    @NotBlank(message = "El email no puede estar vacio")
    @Email(message = "El formato del email no es valido")
    @Column(nullable = false)
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacia")
    @Column(name = "password_hash", nullable = false, length = 255)
    @Size(min = 8, max = 50, message = "La contraseña debe contener minimo 8 caracteres")
    private String contrasena;
}
