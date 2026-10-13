package com.dev.cutly.empleado.dto.turno;

import com.dev.cutly.turno.enums.TurnoStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmpleadoTurnoDto(
        Long turnoId,
        Long negocioId,
        String negocioNombre,
        Long clienteId,
        String clienteNombre,
        String clienteApellido,
        Long empleadoId,
        Long servicioId,
        String servicioNombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        TurnoStatus status,
        BigDecimal precio,
        String observaciones
) {
}
