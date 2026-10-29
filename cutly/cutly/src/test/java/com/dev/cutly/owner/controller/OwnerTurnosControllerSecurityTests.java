package com.dev.cutly.owner.controller;

import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.auth.security.JwtUtil;
import com.dev.cutly.auth.security.SecurityConfig;
import com.dev.cutly.owner.dto.turno.CrearOwnerTurnoManualRequestDto;
import com.dev.cutly.owner.dto.turno.OwnerTurnoDto;
import com.dev.cutly.owner.service.OwnerTurnosService;
import com.dev.cutly.turno.enums.TipoClienteTurno;
import com.dev.cutly.turno.enums.TipoResponsableTurno;
import com.dev.cutly.turno.enums.TurnoStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OwnerTurnosController.class)
@Import(SecurityConfig.class)
class OwnerTurnosControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerTurnosService ownerTurnosService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenInvalidoRepository tokenInvalidoRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void crearTurnoManualRequiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/owner/negocios/20/turnos")
                        .contentType("application/json")
                        .content(cuerpoValido()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ownerTurnosService);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void crearTurnoManualRequiereRolOwner() throws Exception {
        mockMvc.perform(post("/api/owner/negocios/20/turnos")
                        .contentType("application/json")
                        .content(cuerpoValido()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ownerTurnosService);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void rechazaPeticionSinClienteLocal() throws Exception {
        mockMvc.perform(post("/api/owner/negocios/20/turnos")
                        .contentType("application/json")
                        .content("""
                                {"servicioId":50,"fechaHoraInicio":"2030-01-02T10:00:00"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ownerTurnosService);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void rechazaPeticionConAmbasAlternativasDeCliente() throws Exception {
        mockMvc.perform(post("/api/owner/negocios/20/turnos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "servicioId":50,
                                  "fechaHoraInicio":"2030-01-02T10:00:00",
                                  "clienteLocalId":31,
                                  "clienteNuevo":{"nombre":"Ana","apellido":"Pérez","dni":"12345678","telefono":"5555"}
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ownerTurnosService);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void creaTurnoYDevuelveTipoDeClienteYResponsable() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2030, 1, 2, 10, 0);
        OwnerTurnoDto respuesta = new OwnerTurnoDto(
                70L, 20L, null, "Ana", "Pérez", null, null, null,
                50L, "Corte", inicio, inicio.plusMinutes(45), TurnoStatus.CONFIRMED,
                null, null, 31L, TipoClienteTurno.LOCAL, 3L, TipoResponsableTurno.OWNER,
                "Olga", "Owner"
        );
        when(ownerTurnosService.crearManual(eq(20L), any(CrearOwnerTurnoManualRequestDto.class),
                eq("owner@example.com"))).thenReturn(respuesta);

        mockMvc.perform(post("/api/owner/negocios/20/turnos")
                        .contentType("application/json")
                        .content("""
                                {"servicioId":50,"fechaHoraInicio":"2030-01-02T10:00:00","clienteLocalId":31,"ownerResponsableId":900}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.tipoCliente").value("LOCAL"))
                .andExpect(jsonPath("$.clienteLocalId").value(31))
                .andExpect(jsonPath("$.tipoResponsable").value("OWNER"))
                .andExpect(jsonPath("$.ownerResponsableId").value(3));

        verify(ownerTurnosService).crearManual(eq(20L), any(CrearOwnerTurnoManualRequestDto.class),
                eq("owner@example.com"));
    }

    private String cuerpoValido() {
        return """
                {"servicioId":50,"fechaHoraInicio":"2030-01-02T10:00:00","clienteLocalId":31}
                """;
    }
}
