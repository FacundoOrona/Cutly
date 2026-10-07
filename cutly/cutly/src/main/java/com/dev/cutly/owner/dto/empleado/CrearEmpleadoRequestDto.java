package com.dev.cutly.owner.dto.empleado;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearEmpleadoRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 20, message = "El nombre debe contener entre 2 y 20 caracteres")
        String nombre,
        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 20, message = "El apellido debe contener entre 2 y 20 caracteres")
        String apellido,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "^[0-9]{7,8}$", message = "El DNI debe contener 7 u 8 dígitos")
        String dni,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe contener al menos 8 caracteres")
        String contrasena
) {
}
