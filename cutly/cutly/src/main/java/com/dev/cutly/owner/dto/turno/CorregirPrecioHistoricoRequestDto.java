package com.dev.cutly.owner.dto.turno;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CorregirPrecioHistoricoRequestDto(
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal importeHistorico,
        @NotBlank @Size(min = 3, max = 250) String motivo
) { }
