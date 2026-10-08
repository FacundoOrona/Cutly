package com.dev.cutly.owner.dto.pago;

import com.dev.cutly.pago.enums.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CrearPagoRequestDto(
        @NotNull(message = "El turno es obligatorio")
        @Positive(message = "El identificador del turno debe ser mayor que cero")
        Long turnoId,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 8 enteros y 2 decimales")
        BigDecimal monto,
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago
) {
}
