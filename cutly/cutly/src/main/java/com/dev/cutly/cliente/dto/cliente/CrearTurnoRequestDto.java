package com.dev.cutly.cliente.dto.cliente;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CrearTurnoRequestDto(
        @NotNull Long negocioId,
        @NotNull Long servicioId,
        Long empleadoId,
        @NotNull LocalDateTime fechaHoraInicio,
        String observaciones
) {
}
