package com.dev.cutly.owner.dto.liquidacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularLiquidacionRequestDto(@NotBlank @Size(max=500) String motivo) {}
