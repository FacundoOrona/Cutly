package com.dev.cutly.owner.dto.empleado;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarEmpleadoRequestDto(
        @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
        @Size(min = 2, max = 20, message = "El nombre debe contener entre 2 y 20 caracteres")
        String nombre,
        @Pattern(regexp = ".*\\S.*", message = "El apellido no puede estar vacío")
        @Size(min = 2, max = 20, message = "El apellido debe contener entre 2 y 20 caracteres")
        String apellido,
        @Email(message = "El formato del email no es válido")
        String email,
        @Pattern(regexp = "^[0-9]{7,8}$", message = "El DNI debe contener 7 u 8 dígitos")
        String dni
) {
}
