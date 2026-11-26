package com.dev.cutly.empleado.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearEmpleadoClienteLocalRequestDto(
        @NotBlank @Size(min = 2, max = 20) String nombre,
        @NotBlank @Size(min = 2, max = 20) String apellido,
        @NotBlank @Size(max = 20) String dni,
        @NotBlank @Size(max = 30) String telefono
) { }
