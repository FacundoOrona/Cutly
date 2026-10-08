package com.dev.cutly.owner.dto.pago;

import com.dev.cutly.pago.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerPagoDto(
        Long pagoId,
        Long negocioId,
        Long turnoId,
        BigDecimal monto,
        MetodoPago metodoPago,
        LocalDateTime fechaPago
) {
}
