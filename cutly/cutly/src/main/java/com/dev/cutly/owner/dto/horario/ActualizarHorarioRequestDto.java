package com.dev.cutly.owner.dto.horario;

import com.dev.cutly.negocio.enums.DiaSemana;

import java.time.LocalTime;

public record ActualizarHorarioRequestDto(
        DiaSemana diaSemana,
        LocalTime horaApertura,
        LocalTime horaCierre,
        Boolean cerrado
) {
}
