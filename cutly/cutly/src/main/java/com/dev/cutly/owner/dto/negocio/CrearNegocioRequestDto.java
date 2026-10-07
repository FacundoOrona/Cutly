package com.dev.cutly.owner.dto.negocio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearNegocioRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
        String telefono,
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email
) {
}
