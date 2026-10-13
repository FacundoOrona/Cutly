package com.dev.cutly.empleado.dto.horario;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmpleadoDisponibilidadDto(LocalDate fecha, LocalDateTime fechaHoraInicio,
                                        LocalDateTime fechaHoraFin) {
}
