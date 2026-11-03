package com.dev.cutly.owner.dto.transaccion;

import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.pago.enums.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerTransaccionDto(
        Long pagoId,
        Long negocioId,
        LocalDateTime fechaPago,
        BigDecimal monto,
        MetodoPago metodoPago,
        String descripcionOtro,
        TipoTransaccion tipo,
        Long clienteId,
        Long clienteLocalId,
        String nombre,
        String apellido,
        Long turnoId,
        Long deudaId
) {}
