package com.dev.cutly.owner.controller;

import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.auth.security.JwtUtil;
import com.dev.cutly.auth.security.SecurityConfig;
import com.dev.cutly.owner.service.OwnerClienteLocalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OwnerClienteLocalController.class)
@Import(SecurityConfig.class)
class OwnerClienteLocalControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerClienteLocalService ownerClienteLocalService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenInvalidoRepository tokenInvalidoRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void requiereAutenticacion() throws Exception {
        mockMvc.perform(get("/api/owner/negocios/20/clientes-locales?q=Ana"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ownerClienteLocalService);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void requiereRolOwner() throws Exception {
        mockMvc.perform(get("/api/owner/negocios/20/clientes-locales?q=Ana"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ownerClienteLocalService);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void elListadoPaginadoRequiereRolOwner() throws Exception {
        mockMvc.perform(get("/api/owner/negocios/20/clientes-locales/paginado"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ownerClienteLocalService);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void elListadoPaginadoRechazaPageNoNumerico() throws Exception {
        mockMvc.perform(get("/api/owner/negocios/20/clientes-locales/paginado?page=x"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ownerClienteLocalService);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void rechazaCreacionConCamposInvalidosAntesDeLlamarAlServicio() throws Exception {
        mockMvc.perform(post("/api/owner/negocios/20/clientes-locales")
                        .contentType("application/json")
                        .content("""
                                {
                                  "nombre": "A",
                                  "apellido": "",
                                  "dni": "12345678",
                                  "telefono": "5555"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ownerClienteLocalService);
    }
}
