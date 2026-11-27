package com.dev.cutly.owner.dto.liquidacion;

import com.dev.cutly.pago.enums.MetodoPago;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record CrearLiquidacionEmpleadoRequestDto(
        @NotNull LocalDate fechaEfectiva,
        @NotNull MetodoPago metodoPago,
        @Size(max = 100) String descripcionMetodo,
        @Size(max = 500) String observaciones,
        @NotEmpty Map<Long, @NotNull BigDecimal> aplicaciones
) {}
