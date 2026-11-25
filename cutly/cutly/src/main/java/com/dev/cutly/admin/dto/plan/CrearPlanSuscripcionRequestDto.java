package com.dev.cutly.admin.dto.plan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CrearPlanSuscripcionRequestDto(
        @NotBlank(message = "El nombre del plan es obligatorio")
        String nombre,
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,
        @NotNull(message = "El precio mensual es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio mensual debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
        BigDecimal precioMensual,
        @NotNull(message = "La cantidad máxima de negocios es obligatoria")
        @jakarta.validation.constraints.Positive(message = "La cantidad máxima de negocios debe ser mayor que cero")
        Integer maxNegocios
) {
}
