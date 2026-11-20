package com.dev.cutly.suscripcion.service;

import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private boolean tieneAcceso(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        return control.tieneAcceso("owner@example.com", "ROLE_OWNER", request);
    }
}
