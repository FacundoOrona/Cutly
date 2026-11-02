package com.dev.cutly.owner.dto.turno;

import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.pago.enums.MetodoPago;

import java.math.BigDecimal;

public record ResumenFinancieroTurnoDto(
        DecisionFinanciera decisionFinanciera,
        BigDecimal monto,
        MetodoPago metodoPago,
        BigDecimal totalAbonado,
        BigDecimal saldoPendiente,
        BigDecimal montoAcordado
) {
}
