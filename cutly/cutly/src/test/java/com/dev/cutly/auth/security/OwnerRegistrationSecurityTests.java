package com.dev.cutly.auth.security;

import com.dev.cutly.auth.controller.AuthController;
import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class OwnerRegistrationSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenInvalidoRepository tokenInvalidoRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void registroOwnerPublicoEsRechazadoAntesDeCrearLaCuenta() throws Exception {
        mockMvc.perform(post("/auth/registro-owner")
                        .contentType("application/json")
                        .content("""
                                {
                                  "nombre": "Ana",
                                  "apellido": "Pérez",
                                  "email": "ana@example.com",
                                  "dni": "12345678",
                                  "contrasena": "password-segura"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    @Test
    void registroClienteSigueAccesibleSinAutenticacion() throws Exception {
        mockMvc.perform(post("/auth/registro-cliente")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}
