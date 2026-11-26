package com.dev.cutly.empleado.controller;

import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.auth.security.JwtUtil;
import com.dev.cutly.auth.security.SecurityConfig;
import com.dev.cutly.empleado.dto.turno.CrearEmpleadoTurnoRequestDto;
import com.dev.cutly.empleado.dto.turno.EmpleadoTurnoDto;
import com.dev.cutly.empleado.service.EmpleadoAccesoService;
import com.dev.cutly.empleado.service.EmpleadoTurnoService;
import com.dev.cutly.turno.enums.TipoClienteTurno;
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

@WebMvcTest(EmpleadoTurnoController.class)
@Import(SecurityConfig.class)
class EmpleadoTurnoControllerSecurityTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private EmpleadoTurnoService service;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private TokenInvalidoRepository tokenInvalidoRepository;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private EmpleadoAccesoService empleadoAccesoService;

    @Test
    void crearTurnoRequiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/empleado/turnos").contentType("application/json").content(cuerpoValido()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void crearTurnoRequiereRolEmpleado() throws Exception {
        mockMvc.perform(post("/api/empleado/turnos").contentType("application/json").content(cuerpoValido()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "empleada@example.com", roles = "EMPLOYEE")
    void identidadAutenticadaSePasaAlServicioYNoSeAceptanIdsDeAsignacionDelBody() throws Exception {
        var inicio = LocalDateTime.of(2030, 1, 2, 10, 0);
        when(service.crear(any(CrearEmpleadoTurnoRequestDto.class), eq("empleada@example.com")))
                .thenReturn(new EmpleadoTurnoDto(90L, 20L, "Negocio", null, "Ana", "Local", 9L, 7L,
                        "Corte", inicio, inicio.plusMinutes(30), TurnoStatus.CONFIRMED, null, null,
                        31L, TipoClienteTurno.LOCAL));
        mockMvc.perform(post("/api/empleado/turnos").contentType("application/json")
                        .content("{\"servicioId\":7,\"fechaHoraInicio\":\"2030-01-02T10:00:00\",\"clienteLocalId\":31,\"empleadoId\":999,\"negocioId\":888}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.empleadoId").value(9));
        var captor = org.mockito.ArgumentCaptor.forClass(CrearEmpleadoTurnoRequestDto.class);
        verify(service).crear(captor.capture(), eq("empleada@example.com"));
        org.junit.jupiter.api.Assertions.assertEquals(7L, captor.getValue().servicioId());
        org.junit.jupiter.api.Assertions.assertEquals(31L, captor.getValue().clienteLocalId());
    }

    private String cuerpoValido() {
        return "{\"servicioId\":7,\"fechaHoraInicio\":\"2030-01-02T10:00:00\",\"clienteLocalId\":31}";
    }
}
