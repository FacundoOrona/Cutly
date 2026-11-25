package com.dev.cutly.suscripcion.service;

import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlAccesoSuscripcionTests {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NegocioRepository negocioRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private SuscripcionRepository suscripcionRepository;

    private ControlAccesoSuscripcion control;

    @BeforeEach
    void crearControl() {
        control = new ControlAccesoSuscripcion(usuarioRepository, negocioRepository,
                empleadoRepository, suscripcionRepository);
    }

    @Test
    void permiteAmbosPatchDeNotificacionesAunqueElAccesoOperativoEsteRestringido() {
        assertTrue(tieneAcceso("PATCH", "/api/owner/notificaciones/42/leer"));
        assertTrue(tieneAcceso("PATCH", "/api/owner/notificaciones/leer-todas"));
    }

    @Test
    void mantieneBloqueadosOtrosPatchDelOwnerYOtrosMetodosEnNotificaciones() {
        assertFalse(tieneAcceso("PATCH", "/api/owner/negocios/20/turnos/70"));
        assertFalse(tieneAcceso("POST", "/api/owner/notificaciones/42/leer"));
        assertFalse(tieneAcceso("PATCH", "/api/owner/notificaciones/42/leer/"));
    }

    @Test
    void accesoOwnerSeDeterminaPorSuSuscripcionYEstado() {
        Usuario owner = owner();
        when(usuarioRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(suscripcionRepository.findByOwner_UsuarioId(7L)).thenReturn(Optional.of(suscripcion(owner, SuscripcionStatus.ACTIVE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2))));
        assertTrue(tieneAcceso("POST", "/api/owner/negocios/1/turnos"));

        when(suscripcionRepository.findByOwner_UsuarioId(7L)).thenReturn(Optional.of(suscripcion(owner, SuscripcionStatus.PENDING, LocalDate.now(), LocalDate.now().plusDays(30))));
        assertFalse(tieneAcceso("POST", "/api/owner/negocios/1/turnos"));
    }

    @Test
    void suscripcionVencidaPermiteLecturasPeroNoEscriturasYFechaFuturaNoHabilita() {
        Usuario owner = owner();
        when(usuarioRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(suscripcionRepository.findByOwner_UsuarioId(7L)).thenReturn(Optional.of(suscripcion(owner, SuscripcionStatus.ACTIVE, LocalDate.now().minusDays(40), LocalDate.now().minusDays(1))));
        assertTrue(tieneAcceso("GET", "/api/owner/negocios/1"));
        assertFalse(tieneAcceso("POST", "/api/owner/negocios/1/turnos"));

        when(suscripcionRepository.findByOwner_UsuarioId(7L)).thenReturn(Optional.of(suscripcion(owner, SuscripcionStatus.ACTIVE, LocalDate.now().plusDays(1), LocalDate.now().plusDays(30))));
        assertFalse(tieneAcceso("GET", "/api/owner/negocios/1"));
    }

    @Test
    void employeeYClientSiguenLaSuscripcionDelOwnerDelNegocio() {
        Usuario owner = owner();
        Negocio negocio = new Negocio();
        negocio.setNegocioId(41L);
        negocio.setOwner(owner);
        Empleado empleado = new Empleado();
        empleado.setNegocio(negocio);
        when(empleadoRepository.findByUsuario_Email("employee@example.com")).thenReturn(Optional.of(empleado));
        when(negocioRepository.findById(41L)).thenReturn(Optional.of(negocio));
        when(suscripcionRepository.findByOwner_UsuarioId(7L)).thenReturn(Optional.of(
                suscripcion(owner, SuscripcionStatus.ACTIVE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2))));

        assertTrue(tieneAcceso("GET", "/api/empleado/turnos", "employee@example.com", "ROLE_EMPLOYEE"));
        assertTrue(tieneAcceso("GET", "/api/cliente/negocios/41", "client@example.com", "ROLE_CLIENT"));

        when(suscripcionRepository.findByOwner_UsuarioId(7L)).thenReturn(Optional.of(
                suscripcion(owner, SuscripcionStatus.PENDING, LocalDate.now(), LocalDate.now().plusDays(30))));
        assertFalse(tieneAcceso("GET", "/api/cliente/negocios/41", "client@example.com", "ROLE_CLIENT"));
    }

    @Test
    void employeeQuedaBloqueadoSiLaSuscripcionDelOwnerNoPermiteOperar() {
        Usuario owner = owner();
        Negocio negocio = new Negocio();
        negocio.setOwner(owner);
        Empleado empleado = new Empleado();
        empleado.setNegocio(negocio);
        when(empleadoRepository.findByUsuario_Email("employee@example.com")).thenReturn(Optional.of(empleado));

        for (SuscripcionStatus status : new SuscripcionStatus[]{
                SuscripcionStatus.PENDING, SuscripcionStatus.SUSPENDED, SuscripcionStatus.CANCELLED,
                SuscripcionStatus.EXPIRED
        }) {
            when(suscripcionRepository.findByOwner_UsuarioId(7L)).thenReturn(Optional.of(
                    suscripcion(owner, status, LocalDate.now().minusDays(30), LocalDate.now().minusDays(1))));
            assertFalse(tieneAcceso("GET", "/api/empleado/turnos", "employee@example.com", "ROLE_EMPLOYEE"),
                    "El empleado no debe acceder con estado " + status);
        }
        when(suscripcionRepository.findByOwner_UsuarioId(7L)).thenReturn(Optional.of(
                suscripcion(owner, SuscripcionStatus.ACTIVE, LocalDate.now().minusDays(30), LocalDate.now().minusDays(1))));
        assertFalse(tieneAcceso("GET", "/api/empleado/turnos", "employee@example.com", "ROLE_EMPLOYEE"),
                "El empleado no debe acceder a una suscripción ACTIVE vencida");
    }

    private Usuario owner() {
        Usuario owner = new Usuario();
        owner.setUsuarioId(7L);
        owner.setRol(Rol.OWNER);
        return owner;
    }

    private Suscripcion suscripcion(Usuario owner, SuscripcionStatus status, LocalDate inicio, LocalDate vencimiento) {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setOwner(owner);
        suscripcion.setStatus(status);
        suscripcion.setFechaInicio(inicio);
        suscripcion.setFechaVencimiento(vencimiento);
        suscripcion.setPlan(PlanSuscripcion.crearPlanTrial());
        suscripcion.asignarPrecioContratado(BigDecimal.ZERO);
        return suscripcion;
    }

    private boolean tieneAcceso(String method, String path) {
        return tieneAcceso(method, path, "owner@example.com", "ROLE_OWNER");
    }

    private boolean tieneAcceso(String method, String path, String email, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        return control.tieneAcceso(email, role, request);
    }
}
