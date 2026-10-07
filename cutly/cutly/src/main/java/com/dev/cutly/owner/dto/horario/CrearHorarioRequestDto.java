package com.dev.cutly.owner.dto.horario;

import com.dev.cutly.negocio.enums.DiaSemana;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CrearHorarioRequestDto(
        @NotNull(message = "El día de la semana es obligatorio")
        DiaSemana diaSemana,
        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean cerrado
) {
}
