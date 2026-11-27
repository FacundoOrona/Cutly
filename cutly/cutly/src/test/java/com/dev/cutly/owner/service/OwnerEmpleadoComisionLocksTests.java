package com.dev.cutly.owner.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerEmpleadoComisionLocksTests {
    @Mock EmpleadoRepository empleadoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock OwnerNegocioService ownerNegocioService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmpleadoServiciorRepository asignacionRepository;

    private OwnerEmpleadoService service;
    private Empleado empleado;
    private EmpleadoServicio asignacion;

    @BeforeEach
    void setUp() {
        service = new OwnerEmpleadoService(empleadoRepository, usuarioRepository, ownerNegocioService,
                passwordEncoder, asignacionRepository);
        Negocio negocio = new Negocio(); negocio.setNegocioId(2L);
        empleado = new Empleado(); empleado.setEmpleadoId(3L); empleado.setNegocio(negocio);
        Servicio servicio = new Servicio(); servicio.setServicioId(4L); servicio.setNegocio(negocio);
        asignacion = new EmpleadoServicio(); asignacion.setEmpleado(empleado); asignacion.setServicio(servicio);
        when(ownerNegocioService.obtenerEntidadNegocioPropio(2L, "owner@test.local")).thenReturn(negocio);
        when(empleadoRepository.findWithLockingByEmpleadoId(3L)).thenReturn(Optional.of(empleado));
        when(asignacionRepository.findByEmpleado_EmpleadoIdOrderByServicio_ServicioIdAsc(3L))
                .thenReturn(List.of(asignacion));
    }

    @Test
    void porcentajeGeneralSeSerializaConResolucionDeComisionPorLockDeEmpleado() {
        when(empleadoRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        service.actualizarPorcentajeGeneral(2L, 3L, new BigDecimal("18.25"), "owner@test.local");

        verify(empleadoRepository).findWithLockingByEmpleadoId(3L);
        verify(empleadoRepository).save(empleado);
    }

    @Test
    void cambioYEliminacionDeExcepcionBloqueanEmpleadoAntesDeLaAsignacion() {
        when(asignacionRepository.findWithLockingByEmpleadoAndServicio(3L, 4L))
                .thenReturn(Optional.of(asignacion));
        when(asignacionRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        service.actualizarExcepcion(2L, 3L, 4L, new BigDecimal("27.50"), "owner@test.local");
        service.eliminarExcepcion(2L, 3L, 4L, "owner@test.local");
        InOrder lockOrder = inOrder(empleadoRepository, asignacionRepository);
        lockOrder.verify(empleadoRepository).findWithLockingByEmpleadoId(3L);
        lockOrder.verify(asignacionRepository).findWithLockingByEmpleadoAndServicio(3L, 4L);
        lockOrder.verify(empleadoRepository).findWithLockingByEmpleadoId(3L);
        lockOrder.verify(asignacionRepository).findWithLockingByEmpleadoAndServicio(3L, 4L);
    }
}
