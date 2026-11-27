package com.dev.cutly.owner.dto.liquidacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.dev.cutly.liquidacion.enums.EstadoLiquidacion;
import com.dev.cutly.pago.enums.MetodoPago;
public record AplicacionLiquidacionDto(Long remuneracionId, Long turnoId, BigDecimal importe,
        BigDecimal totalPagado, BigDecimal saldoPendiente, Long liquidacionId, LocalDate fechaEfectiva,
        MetodoPago metodoPago, EstadoLiquidacion estadoLiquidacion) {}
