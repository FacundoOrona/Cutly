package com.dev.cutly.turno.service;

import com.dev.cutly.notificacion.service.NotificacionTurnoService;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.remuneracion.service.RemuneracionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TurnoLifecycleScheduler {
    private static final List<TurnoStatus> ESTADOS_NOTIFICABLES = List.of(
            TurnoStatus.PENDING, TurnoStatus.CONFIRMED);

    private final TurnoRepository turnoRepository;
    private final NotificacionTurnoService notificacionTurnoService;
    private final RemuneracionService remuneracionService;

    public TurnoLifecycleScheduler(TurnoRepository turnoRepository,
                                   NotificacionTurnoService notificacionTurnoService,
                                   RemuneracionService remuneracionService) {
        this.turnoRepository = turnoRepository;
        this.notificacionTurnoService = notificacionTurnoService;
        this.remuneracionService = remuneracionService;
    }

    @Scheduled(fixedDelayString = "${cutly.turnos.lifecycle-delay-ms:30000}")
    @Transactional
    public void procesarTurnos() {
        LocalDateTime ahora = LocalDateTime.now();
        for (Long turnoId : turnoRepository.findIdsForReminder(ESTADOS_NOTIFICABLES, ahora, ahora.plusMinutes(15))) {
            turnoRepository.findWithLockingByTurnoId(turnoId).ifPresent(turno -> notificarSiCorresponde(turno));
        }

        LocalDateTime momentoFin = LocalDateTime.now();
        for (Long turnoId : turnoRepository.findIdsForAutoCompletion(TurnoStatus.IN_PROGRESS, momentoFin)) {
            turnoRepository.findWithLockingByTurnoId(turnoId).ifPresent(turno -> completarSiCorresponde(turno, momentoFin));
        }
    }

    private void notificarSiCorresponde(Turno turno) {
        LocalDateTime ahora = LocalDateTime.now();
        if (ESTADOS_NOTIFICABLES.contains(turno.getStatus())
                && turno.getFechaHoraInicio().isAfter(ahora)
                && !turno.getFechaHoraInicio().isAfter(ahora.plusMinutes(15))) {
            notificacionTurnoService.notificarTurnoProximo(turno);
        }
    }

    private void completarSiCorresponde(Turno turno, LocalDateTime ahora) {
        if (turno.getStatus() == TurnoStatus.IN_PROGRESS && !turno.getFechaHoraFin().isAfter(ahora)) {
            turno.setStatus(TurnoStatus.COMPLETED);
            Turno guardado = turnoRepository.save(turno);
            remuneracionService.asegurarPendiente(guardado);
        }
    }
}
