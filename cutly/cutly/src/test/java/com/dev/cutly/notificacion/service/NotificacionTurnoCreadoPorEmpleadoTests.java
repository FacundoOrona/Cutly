package com.dev.cutly.notificacion.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.notificacion.entity.Notificacion;
import com.dev.cutly.notificacion.repository.NotificacionRepository;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionTurnoCreadoPorEmpleadoTests {
    @Mock private NotificacionRepository repository;
    @InjectMocks private NotificacionTurnoService service;

    @Test
    void notificaAlOwnerConInformacionDelEmpleadoSinNotificarAlCreador() {
        Usuario owner = usuario(1L, "Olga", "Owner");
        Usuario empleadoUsuario = usuario(2L, "Eva", "Empleado");
        Negocio negocio = new Negocio(); negocio.setNegocioId(8L); negocio.setNombre("Cutly"); negocio.setOwner(owner);
        Empleado empleado = new Empleado(); empleado.setUsuario(empleadoUsuario);
        ClienteLocal cliente = new ClienteLocal(); cliente.setNombre("Ana"); cliente.setApellido("Cliente");
        Servicio servicio = new Servicio(); servicio.setNombre("Corte");
        Turno turno = new Turno(); turno.setTurnoId(50L); turno.setNegocio(negocio); turno.setEmpleado(empleado);
        turno.setClienteLocal(cliente); turno.setServicio(servicio); turno.setFechaHoraInicio(LocalDateTime.of(2030, 1, 2, 10, 0));
        when(repository.findByClaveEvento(anyString())).thenReturn(Optional.empty());

        service.notificarTurnoCreadoPorEmpleado(turno);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(captor.capture());
        Notificacion notificacion = captor.getValue();
        assertEquals(owner, notificacion.getUsuario());
        assertEquals("TURNO_CREADO_POR_EMPLEADO", notificacion.getTipoEvento());
        org.junit.jupiter.api.Assertions.assertTrue(notificacion.getMensaje().contains("Eva Empleado"));
        org.junit.jupiter.api.Assertions.assertTrue(notificacion.getMensaje().contains("Ana Cliente"));
    }

    @Test
    void alCompletarTurnoNotificaAlOwnerConTurnoYClienteSinDuplicar() {
        Usuario owner = usuario(1L, "Olga", "Owner");
        Usuario empleadoUsuario = usuario(2L, "Eva", "Empleado");
        Negocio negocio = new Negocio(); negocio.setNegocioId(8L); negocio.setNombre("Cutly"); negocio.setOwner(owner);
        Empleado empleado = new Empleado(); empleado.setUsuario(empleadoUsuario);
        ClienteLocal cliente = new ClienteLocal(); cliente.setNombre("Ana"); cliente.setApellido("Cliente");
        Servicio servicio = new Servicio(); servicio.setNombre("Corte");
        Turno turno = new Turno(); turno.setTurnoId(50L); turno.setNegocio(negocio); turno.setEmpleado(empleado);
        turno.setClienteLocal(cliente); turno.setServicio(servicio);
        turno.setFechaHoraInicio(LocalDateTime.of(2030, 1, 2, 10, 0));
        when(repository.findByClaveEvento("turno:50:completado-por-empleado:usuario:1"))
                .thenReturn(Optional.empty(), Optional.of(new Notificacion()));

        service.notificarCambioEstado(turno, TurnoStatus.IN_PROGRESS, TurnoStatus.COMPLETED, empleadoUsuario);
        service.notificarCambioEstado(turno, TurnoStatus.IN_PROGRESS, TurnoStatus.COMPLETED, empleadoUsuario);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository, times(1)).save(captor.capture());
        Notificacion notificacion = captor.getValue();
        assertEquals(owner, notificacion.getUsuario());
        assertEquals("TURNO_COMPLETADO_POR_EMPLEADO", notificacion.getTipoEvento());
        assertEquals(50L, notificacion.getRecursoId());
        org.junit.jupiter.api.Assertions.assertTrue(notificacion.getMensaje().contains("Eva Empleado"));
        org.junit.jupiter.api.Assertions.assertTrue(notificacion.getMensaje().contains("turno #50"));
        org.junit.jupiter.api.Assertions.assertTrue(notificacion.getMensaje().contains("Ana Cliente"));
    }

    @Test
    void completarTurnoPorOwnerNoGeneraAvisoDeEmpleado() {
        Usuario owner = usuario(1L, "Olga", "Owner");
        Usuario empleadoUsuario = usuario(2L, "Eva", "Empleado");
        Negocio negocio = new Negocio(); negocio.setNegocioId(8L); negocio.setOwner(owner);
        Empleado empleado = new Empleado(); empleado.setUsuario(empleadoUsuario);
        Turno turno = new Turno(); turno.setTurnoId(50L); turno.setNegocio(negocio); turno.setEmpleado(empleado);

        service.notificarCambioEstado(turno, TurnoStatus.IN_PROGRESS, TurnoStatus.COMPLETED, owner);

        verify(repository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any(Notificacion.class));
    }

    private Usuario usuario(Long id, String nombre, String apellido) {
        Usuario usuario = new Usuario(); usuario.setUsuarioId(id); usuario.setNombre(nombre); usuario.setApellido(apellido);
        return usuario;
    }
}
