package com.dev.cutly.cliente.dto.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaRequestDto(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String contrasenaActual,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La nueva contraseña debe contener mínimo 8 caracteres")
        String nuevaContrasena
) {
}
