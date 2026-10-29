package com.dev.cutly.owner.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearClienteLocalRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 20, message = "El nombre debe contener entre 2 y 20 caracteres")
        String nombre,
        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 20, message = "El apellido debe contener entre 2 y 20 caracteres")
        String apellido,
        @NotBlank(message = "El DNI es obligatorio")
        @Size(max = 20, message = "El DNI no puede superar los 20 caracteres")
        String dni,
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
        String telefono
) {
}
