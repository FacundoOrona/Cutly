package com.dev.cutly.owner.dto.liquidacion;

import com.dev.cutly.pago.enums.MetodoPago;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record CrearLiquidacionEmpleadoRequestDto(
        @NotNull LocalDate fechaEfectiva,
        @NotNull MetodoPago metodoPago,
        String descripcionMetodo,
        String observaciones,
        @NotEmpty Map<Long, @NotNull BigDecimal> aplicaciones
) {}
