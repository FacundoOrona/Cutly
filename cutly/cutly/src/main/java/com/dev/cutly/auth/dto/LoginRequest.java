package com.dev.cutly.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El email no puede estar vacio")
        @Email(message = "Formato de email no valido")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacia")
        String contrasena
) {
}
