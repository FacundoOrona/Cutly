package com.dev.cutly.auth.security;

import com.dev.cutly.admin.controller.AdminOwnerController;
import com.dev.cutly.admin.dto.usuario.AdminUsuarioDto;
import com.dev.cutly.admin.service.AdminUsuarioService;
import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.auth.service.AuthService;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminOwnerController.class, com.dev.cutly.auth.controller.AuthController.class})
@Import(SecurityConfig.class)
class OwnerRegistrationSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AdminUsuarioService adminUsuarioService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenInvalidoRepository tokenInvalidoRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void registroOwnerRequiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/admin/owners")
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
    @WithMockUser(roles = "OWNER")
    void registroOwnerEsRechazadoParaUnOwnerAutenticado() throws Exception {
        mockMvc.perform(post("/api/admin/owners")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void registroOwnerPermiteAlSuperAdministrador() throws Exception {
        mockMvc.perform(post("/api/admin/owners")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void rutaAnteriorDeRegistroOwnerYaNoExiste() throws Exception {
        mockMvc.perform(post("/auth/registro-owner")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(authService);
    }

    @Test
    void activarOwnerRequiereAutenticacion() throws Exception {
        mockMvc.perform(patch("/api/admin/owners/42/activar"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUsuarioService);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void activarOwnerEsRechazadoParaUnOwnerAutenticado() throws Exception {
        mockMvc.perform(patch("/api/admin/owners/42/activar"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUsuarioService);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SUPER_ADMIN")
    void activarOwnerPermiteAlSuperAdministrador() throws Exception {
        AdminUsuarioDto owner = new AdminUsuarioDto(
                42L, "Ana", "Pérez", "12345678", "ana@example.com", Rol.OWNER, UsuarioStatus.ACTIVE
        );
        when(adminUsuarioService.actualizarEstadoOwner(42L, UsuarioStatus.ACTIVE, "admin@example.com"))
                .thenReturn(owner);

        mockMvc.perform(patch("/api/admin/owners/42/activar"))
                .andExpect(status().isOk());

        verify(adminUsuarioService).actualizarEstadoOwner(42L, UsuarioStatus.ACTIVE, "admin@example.com");
    }

    @Test
    void desactivarOwnerRequiereAutenticacion() throws Exception {
        mockMvc.perform(patch("/api/admin/owners/42/desactivar"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUsuarioService);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void desactivarOwnerEsRechazadoParaUnOwnerAutenticado() throws Exception {
        mockMvc.perform(patch("/api/admin/owners/42/desactivar"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUsuarioService);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SUPER_ADMIN")
    void desactivarOwnerPermiteAlSuperAdministrador() throws Exception {
        AdminUsuarioDto owner = new AdminUsuarioDto(
                42L, "Ana", "Pérez", "12345678", "ana@example.com", Rol.OWNER, UsuarioStatus.INACTIVE
        );
        when(adminUsuarioService.actualizarEstadoOwner(42L, UsuarioStatus.INACTIVE, "admin@example.com"))
                .thenReturn(owner);

        mockMvc.perform(patch("/api/admin/owners/42/desactivar"))
                .andExpect(status().isOk());

        verify(adminUsuarioService).actualizarEstadoOwner(42L, UsuarioStatus.INACTIVE, "admin@example.com");
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void rutasDeClienteEnEspanolExigenRolCliente() throws Exception {
        mockMvc.perform(post("/api/cliente/turnos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void rutasDeEmpleadoEnEspanolExigenRolEmpleado() throws Exception {
        mockMvc.perform(post("/api/empleado/turnos"))
                .andExpect(status().isForbidden());
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
