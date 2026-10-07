package com.dev.cutly.owner.dto.direccion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearDireccionRequestDto(
        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 100, message = "La calle no puede superar los 100 caracteres")
        String calle,
        @NotBlank(message = "El número es obligatorio")
        @Size(max = 10, message = "El número no puede superar los 10 caracteres")
        String numero,
        @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
        String ciudad,
        @Size(max = 100, message = "La provincia no puede superar los 100 caracteres")
        String provincia,
        @Size(max = 10, message = "El código postal no puede superar los 10 caracteres")
        String codigoPostal
) {
}
