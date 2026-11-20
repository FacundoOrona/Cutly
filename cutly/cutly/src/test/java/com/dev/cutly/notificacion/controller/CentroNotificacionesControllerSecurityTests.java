package com.dev.cutly.notificacion.controller;

import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.auth.security.JwtUtil;
import com.dev.cutly.auth.security.SecurityConfig;
import com.dev.cutly.empleado.service.EmpleadoAccesoService;
import com.dev.cutly.notificacion.service.CentroNotificacionesService;
import com.dev.cutly.suscripcion.service.ControlAccesoSuscripcion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CentroNotificacionesController.class)
@Import(SecurityConfig.class)
class CentroNotificacionesControllerSecurityTests {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CentroNotificacionesService service;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private TokenInvalidoRepository tokenInvalidoRepository;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private EmpleadoAccesoService empleadoAccesoService;
    @MockitoBean private ControlAccesoSuscripcion controlAccesoSuscripcion;

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void ownerPuedeMarcarUnaNotificacionPropiaComoLeidaConAccesoRestringido() throws Exception {
        permitirSoloPatchesPersonales();

        mockMvc.perform(patch("/api/owner/notificaciones/42/leer"))
                .andExpect(status().isOk());

        verify(service).leer("owner@example.com", 42L);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void ownerPuedeMarcarTodasSusNotificacionesComoLeidasConAccesoRestringido() throws Exception {
        permitirSoloPatchesPersonales();

        mockMvc.perform(patch("/api/owner/notificaciones/leer-todas"))
                .andExpect(status().isOk());

        verify(service).leerTodas("owner@example.com");
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void otroPatchOwnerSigueBloqueadoConAccesoRestringido() throws Exception {
        permitirSoloPatchesPersonales();

        mockMvc.perform(patch("/api/owner/negocios/20/turnos/70"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void patchesDeNotificacionesSiguenRequiriendoRolOwner() throws Exception {
        mockMvc.perform(patch("/api/owner/notificaciones/42/leer"))
                .andExpect(status().isForbidden());
    }

    private void permitirSoloPatchesPersonales() {
        when(controlAccesoSuscripcion.tieneAcceso(any(), any(), any())).thenAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest request = invocation.getArgument(2);
            return "PATCH".equals(request.getMethod())
                    && (request.getRequestURI().matches("/api/owner/notificaciones/\\d+/leer")
                    || "/api/owner/notificaciones/leer-todas".equals(request.getRequestURI()));
        });
    }
}
