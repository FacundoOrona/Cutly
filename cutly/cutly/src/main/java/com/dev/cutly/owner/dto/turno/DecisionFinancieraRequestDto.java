package com.dev.cutly.owner.dto.turno;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.pago.enums.MetodoPago;
import java.math.BigDecimal;
public record DecisionFinancieraRequestDto(DecisionFinanciera decisionFinanciera, BigDecimal monto, BigDecimal pagoInicial, MetodoPago metodoPago, String descripcionOtro, String motivoGratuidad) { }
