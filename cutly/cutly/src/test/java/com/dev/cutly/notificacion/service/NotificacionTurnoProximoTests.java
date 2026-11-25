package com.dev.cutly.notificacion.service;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.notificacion.entity.Notificacion;
import com.dev.cutly.notificacion.repository.NotificacionRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionTurnoProximoTests {
    @Mock private NotificacionRepository repository;

    @Test
    void notificacionProximaIncluyeClienteHorarioYNegocio() {
        when(repository.findByClaveEvento(anyString())).thenReturn(Optional.empty());
        NotificacionTurnoService service = new NotificacionTurnoService(repository);
        Turno turno = turno();

        service.notificarTurnoProximo(turno);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(captor.capture());
        Notificacion notificacion = captor.getValue();
        assertEquals("TURNO_PROXIMO", notificacion.getTipoEvento());
        assertTrue(notificacion.getMensaje().contains("Ana Cliente"));
        assertTrue(notificacion.getMensaje().contains("Barbería Central"));
        assertTrue(notificacion.getMensaje().contains("10:30"));
    }

    @Test
    void noGuardaLaNotificacionSiYaExisteLaClaveDelEvento() {
        when(repository.findByClaveEvento("turno:9:turno-proximo:usuario:3"))
                .thenReturn(Optional.of(new Notificacion()));

        new NotificacionTurnoService(repository).notificarTurnoProximo(turno());

        verify(repository, never()).save(any());
    }

    private Turno turno() {
        Usuario owner = new Usuario();
        owner.setUsuarioId(3L);
        Usuario cliente = new Usuario();
        cliente.setUsuarioId(4L);
        cliente.setNombre("Ana");
        cliente.setApellido("Cliente");
        Negocio negocio = new Negocio();
        negocio.setNegocioId(5L);
        negocio.setNombre("Barbería Central");
        negocio.setOwner(owner);
        Turno turno = new Turno();
        turno.setTurnoId(9L);
        turno.setStatus(TurnoStatus.CONFIRMED);
        turno.setFechaHoraInicio(LocalDateTime.of(2026, 11, 25, 10, 30));
        turno.setNegocio(negocio);
        turno.setCliente(cliente);
        return turno;
    }
}
