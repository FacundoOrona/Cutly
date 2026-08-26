package com.dev.cutly.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank(message = "El nombre de usuario no puede estar vacio")
        @Size(max = 20, min = 5, message = "El nombre de usuario debe contener entre 5 y 20 caracteres")
        String nombreUsuario,

        @NotBlank(message = "El nombre no puede estar vacio")
        @Size(max = 20, min = 2, message = "El nombre debe contener entre 2 y 20 caracteres")
        String nombre,

        @NotBlank(message = "El apellido no puede estar vacio")
        @Size(max = 20, min = 2, message = "El apellido debe contener entre 2 y 20 caracteres")
        String apellido,

        @NotBlank(message = "El email no debe estar vacio")
        @Email(message = "Formato de email no valido")
        String email,

        @NotBlank(message = "La contraseña no debe estar vacia")
        @Size(min = 8, message = "La contraseña debe contener minimo 8 caracteres")
        String contrasena

) {
}
