package com.dev.cutly.owner.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.owner.dto.servicio.OwnerServicioDto;
import com.dev.cutly.pago.repository.ResultadoFinancieroTurnoRepository;
import com.dev.cutly.remuneracion.service.RemuneracionService;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerEmpleadoServicioRemuneracionTests {
    @Mock EmpleadoServiciorRepository asignacionRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ServicioRepository servicioRepository;
    @Mock OwnerNegocioService ownerNegocioService;
    @Mock TurnoRepository turnoRepository;
    @Mock RemuneracionService remuneracionService;
    @Mock ResultadoFinancieroTurnoRepository resultadoRepository;

    private OwnerEmpleadoServicioService service;
    private Negocio negocio;
    private Empleado empleado;
    private Servicio servicio;
    private EmpleadoServicio asignacion;

    @BeforeEach
    void setUp() {
        service = new OwnerEmpleadoServicioService(asignacionRepository, empleadoRepository, servicioRepository,
                ownerNegocioService, turnoRepository, remuneracionService, resultadoRepository);
        Usuario owner = new Usuario(); owner.setUsuarioId(1L);
        negocio = new Negocio(); negocio.setNegocioId(2L); negocio.setOwner(owner);
        empleado = new Empleado(); empleado.setEmpleadoId(3L); empleado.setNegocio(negocio);
        servicio = new Servicio(); servicio.setServicioId(4L); servicio.setNegocio(negocio);
        asignacion = new EmpleadoServicio(); asignacion.setEmpleado(empleado); asignacion.setServicio(servicio);
        lenient().when(ownerNegocioService.obtenerEntidadNegocioPropio(2L, "owner@test.local")).thenReturn(negocio);
        lenient().when(empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(3L, 2L)).thenReturn(Optional.of(empleado));
        lenient().when(servicioRepository.findByServicioIdAndNegocio_NegocioId(4L, 2L)).thenReturn(Optional.of(servicio));
    }

    @Test
    void desasignarConservaSoloLaExcepcionDeTurnosCompletadosSinResultado() {
        when(asignacionRepository.findWithLockingByEmpleadoAndServicio(3L, 4L)).thenReturn(Optional.of(asignacion));
        Turno pendiente = turno(10L, TurnoStatus.COMPLETED);
        Turno regularizado = turno(11L, TurnoStatus.COMPLETED);
        Turno noRealizado = turno(12L, TurnoStatus.IN_PROGRESS);
        when(turnoRepository.findActiveOrCompletedForEmpleadoServicioWithLocking(2L, 3L, 4L))
                .thenReturn(List.of(pendiente, regularizado, noRealizado));
        when(resultadoRepository.existsByTurno_TurnoId(10L)).thenReturn(false);
        when(resultadoRepository.existsByTurno_TurnoId(11L)).thenReturn(true);
        asignacion.setPorcentajeComision(new BigDecimal("27.50"));

        service.quitarServicio(2L, 3L, 4L, "owner@test.local");

        verify(remuneracionService).conservarExcepcionesAlDesasignar(List.of(pendiente), asignacion);
        verify(asignacionRepository).delete(asignacion);
        InOrder order = inOrder(turnoRepository, asignacionRepository, remuneracionService);
        order.verify(turnoRepository).findActiveOrCompletedForEmpleadoServicioWithLocking(2L, 3L, 4L);
        order.verify(asignacionRepository).findWithLockingByEmpleadoAndServicio(3L, 4L);
        order.verify(remuneracionService).conservarExcepcionesAlDesasignar(List.of(pendiente), asignacion);
        order.verify(asignacionRepository).delete(asignacion);
    }

    @Test
    void ownerAjenoNoPuedeDesasignarNiPrepararSnapshots() {
        when(ownerNegocioService.obtenerEntidadNegocioPropio(2L, "otro@test.local"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class,
                () -> service.quitarServicio(2L, 3L, 4L, "otro@test.local"));

        verifyNoInteractions(turnoRepository, remuneracionService);
    }

    @Test
    void asignarServicioConservaElContratoExistente() {
        when(asignacionRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(3L, 4L))
                .thenReturn(Optional.empty());
        when(asignacionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerServicioDto dto = service.asignarServicio(2L, 3L, 4L, "owner@test.local");

        assertEquals(4L, dto.servicioId());
        verify(remuneracionService, never()).conservarExcepcionesAlDesasignar(any(), any());
    }

    private Turno turno(Long id, TurnoStatus status) {
        Turno turno = new Turno(); turno.setTurnoId(id); turno.setNegocio(negocio);
        turno.setEmpleado(empleado); turno.setServicio(servicio); turno.setStatus(status);
        return turno;
    }
}
