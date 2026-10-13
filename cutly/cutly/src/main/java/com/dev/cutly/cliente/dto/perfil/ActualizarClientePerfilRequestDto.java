package com.dev.cutly.cliente.dto.perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarClientePerfilRequestDto(
        @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
        @Size(max = 20, message = "El nombre no puede superar los 20 caracteres")
        String nombre,
        @Pattern(regexp = ".*\\S.*", message = "El apellido no puede estar vacío")
        @Size(max = 20, message = "El apellido no puede superar los 20 caracteres")
        String apellido,
        @Pattern(regexp = ".*\\S.*", message = "El DNI no puede estar vacío")
        @Size(min = 7, max = 9, message = "El DNI debe contener entre 7 y 9 caracteres")
        String dni,
        @Email(message = "El formato del email no es válido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email
) {
}
