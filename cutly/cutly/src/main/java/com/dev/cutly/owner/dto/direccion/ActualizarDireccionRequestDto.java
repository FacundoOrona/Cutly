package com.dev.cutly.owner.dto.direccion;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarDireccionRequestDto(
        @Pattern(regexp = ".*\\S.*", message = "La calle no puede estar vacía")
        @Size(max = 100, message = "La calle no puede superar los 100 caracteres")
        String calle,
        @Pattern(regexp = ".*\\S.*", message = "El número no puede estar vacío")
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
