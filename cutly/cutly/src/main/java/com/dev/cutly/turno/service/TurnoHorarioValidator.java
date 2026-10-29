package com.dev.cutly.turno.service;

import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.enums.DiaSemana;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class TurnoHorarioValidator {

    private final HorarioNegocioRepository horarioRepository;

    public TurnoHorarioValidator(HorarioNegocioRepository horarioRepository) {
        this.horarioRepository = horarioRepository;
    }

    public void validar(Long negocioId, LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La duración del turno debe ser positiva");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha del turno debe ser futura");
        }
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno no puede atravesar el cierre diario");
        }

        boolean dentroDeHorario = horarioRepository
                .findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(negocioId).stream()
                .filter(horario -> horario.getDiaSemana() == DiaSemana.valueOf(inicio.getDayOfWeek().name()))
                .filter(horario -> !horario.isCerrado())
                .anyMatch(horario -> dentroDeHorario(horario, inicio, fin));
        if (!dentroDeHorario) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay horario laboral para la fecha solicitada");
        }
    }

    private boolean dentroDeHorario(HorarioNegocio horario, LocalDateTime inicio, LocalDateTime fin) {
        return !inicio.toLocalTime().isBefore(horario.getHoraApertura())
                && !fin.toLocalTime().isAfter(horario.getHoraCierre());
    }
}
