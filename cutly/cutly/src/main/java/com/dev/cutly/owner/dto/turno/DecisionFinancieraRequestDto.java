package com.dev.cutly.owner.dto.turno;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.pago.enums.MetodoPago;
import java.math.BigDecimal;
public record DecisionFinancieraRequestDto(DecisionFinanciera decisionFinanciera, BigDecimal monto, MetodoPago metodoPago, String descripcionOtro, String motivoGratuidad) { }
