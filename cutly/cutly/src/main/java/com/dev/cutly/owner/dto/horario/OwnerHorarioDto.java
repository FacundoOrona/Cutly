package com.dev.cutly.owner.dto.horario;

import com.dev.cutly.negocio.enums.DiaSemana;

import java.time.LocalTime;

public record OwnerHorarioDto(
        Long horarioId,
        Long negocioId,
        DiaSemana diaSemana,
        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean cerrado
) {
}
