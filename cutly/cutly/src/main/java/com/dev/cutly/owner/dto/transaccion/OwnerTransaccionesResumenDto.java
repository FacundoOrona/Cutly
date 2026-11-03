package com.dev.cutly.owner.dto.transaccion;

import java.math.BigDecimal;

public record OwnerTransaccionesResumenDto(
        BigDecimal totalIngresado,
        long cantidadTransacciones,
        BigDecimal totalCobrosDirectos,
        BigDecimal totalPagosCuentaCorriente
) {}
