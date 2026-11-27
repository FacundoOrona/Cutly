package com.dev.cutly.owner.controller;

import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.auth.security.JwtUtil;
import com.dev.cutly.auth.security.SecurityConfig;
import com.dev.cutly.empleado.service.EmpleadoAccesoService;
import com.dev.cutly.owner.service.OwnerLiquidacionesService;
import com.dev.cutly.suscripcion.service.ControlAccesoSuscripcion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OwnerLiquidacionesController.class)
@Import(SecurityConfig.class)
class OwnerLiquidacionesControllerSecurityTests {
    @Autowired MockMvc mockMvc;
    @MockitoBean OwnerLiquidacionesService service;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean TokenInvalidoRepository tokenInvalidoRepository;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean EmpleadoAccesoService empleadoAccesoService;
    @MockitoBean ControlAccesoSuscripcion controlAccesoSuscripcion;

    @Test void requiereAutenticacion() throws Exception {
        mockMvc.perform(get("/api/owner/negocios/20/liquidaciones")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "EMPLOYEE") void empleadoNoAccede() throws Exception {
        mockMvc.perform(get("/api/owner/negocios/20/liquidaciones")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "SUPER_ADMIN") void superAdminNoAccedePorLaRutaOwner() throws Exception {
        mockMvc.perform(get("/api/owner/negocios/20/remuneraciones/4")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(username = "owner@test.local", roles = "OWNER") void ownerAlcanzaElServicioSujetoASuValidacionDePropiedad() throws Exception {
        mockMvc.perform(get("/api/owner/negocios/20/liquidaciones")).andExpect(status().isOk());
        verify(service).historial(20L, null, 0, 20, "owner@test.local");
    }
}
