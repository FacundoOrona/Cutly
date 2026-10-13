package com.dev.cutly.empleado.dto.horario;

import com.dev.cutly.negocio.enums.DiaSemana;
import java.time.LocalTime;

public record EmpleadoHorarioDto(DiaSemana diaSemana, LocalTime horaApertura,
                                 LocalTime horaCierre, boolean cerrado) {
}
