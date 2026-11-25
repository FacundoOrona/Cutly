package com.dev.cutly.turno.service;

import com.dev.cutly.notificacion.service.NotificacionTurnoService;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoLifecycleSchedulerTests {
    @Mock private TurnoRepository turnoRepository;
    @Mock private NotificacionTurnoService notificacionTurnoService;

    @Test
    void notificaUnaVezPorEjecucionSoloTurnosVigentesYProximos() {
        Turno turno = turno(TurnoStatus.CONFIRMED, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusMinutes(35));
        when(turnoRepository.findIdsForReminder(anyList(), any(), any())).thenReturn(List.of(1L));
        when(turnoRepository.findWithLockingByTurnoId(1L)).thenReturn(Optional.of(turno));
        when(turnoRepository.findIdsForAutoCompletion(any(), any())).thenReturn(List.of());

        TurnoLifecycleScheduler scheduler = new TurnoLifecycleScheduler(turnoRepository, notificacionTurnoService);
        scheduler.procesarTurnos();

        verify(turnoRepository).findIdsForReminder(eq(List.of(TurnoStatus.PENDING, TurnoStatus.CONFIRMED)), any(), any());
        verify(notificacionTurnoService, times(1)).notificarTurnoProximo(turno);
    }

    @Test
    void completaSoloTurnosEnCursoAlLlegarSuFinSinCrearMovimientosFinancieros() {
        Turno enCurso = turno(TurnoStatus.IN_PROGRESS, LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(1));
        when(turnoRepository.findIdsForReminder(anyList(), any(), any())).thenReturn(List.of());
        when(turnoRepository.findIdsForAutoCompletion(TurnoStatus.IN_PROGRESS, any())).thenReturn(List.of(2L));
        when(turnoRepository.findWithLockingByTurnoId(2L)).thenReturn(Optional.of(enCurso));

        TurnoLifecycleScheduler scheduler = new TurnoLifecycleScheduler(turnoRepository, notificacionTurnoService);
        scheduler.procesarTurnos();

        assertEquals(TurnoStatus.COMPLETED, enCurso.getStatus());
        verify(turnoRepository).save(enCurso);
        verifyNoInteractions(notificacionTurnoService);
    }

    @Test
    void noNotificaTurnosCanceladosAusentesCompletadosEnCursoNiRechazados() {
        List<TurnoStatus> estadosExcluidos = List.of(TurnoStatus.CANCELLED, TurnoStatus.NO_SHOW,
                TurnoStatus.COMPLETED, TurnoStatus.IN_PROGRESS, TurnoStatus.REJECTED);
        when(turnoRepository.findIdsForReminder(anyList(), any(), any())).thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        when(turnoRepository.findIdsForAutoCompletion(any(), any())).thenReturn(List.of());
        for (int i = 0; i < estadosExcluidos.size(); i++) {
            when(turnoRepository.findWithLockingByTurnoId((long) i + 1))
                    .thenReturn(Optional.of(turno(estadosExcluidos.get(i), LocalDateTime.now().plusMinutes(5),
                            LocalDateTime.now().plusMinutes(35))));
        }

        new TurnoLifecycleScheduler(turnoRepository, notificacionTurnoService).procesarTurnos();

        verifyNoInteractions(notificacionTurnoService);
    }

    @Test
    void revalidaEstadoBloqueadoYNoCompletaTurnoQueNoEstaEnCurso() {
        Turno cancelado = turno(TurnoStatus.CANCELLED, LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(1));
        when(turnoRepository.findIdsForReminder(anyList(), any(), any())).thenReturn(List.of());
        when(turnoRepository.findIdsForAutoCompletion(TurnoStatus.IN_PROGRESS, any())).thenReturn(List.of(3L));
        when(turnoRepository.findWithLockingByTurnoId(3L)).thenReturn(Optional.of(cancelado));

        new TurnoLifecycleScheduler(turnoRepository, notificacionTurnoService).procesarTurnos();

        assertEquals(TurnoStatus.CANCELLED, cancelado.getStatus());
        verify(turnoRepository, never()).save(any());
    }

    private Turno turno(TurnoStatus estado, LocalDateTime inicio, LocalDateTime fin) {
        Turno turno = new Turno();
        turno.setStatus(estado);
        turno.setFechaHoraInicio(inicio);
        turno.setFechaHoraFin(fin);
        return turno;
    }
}
