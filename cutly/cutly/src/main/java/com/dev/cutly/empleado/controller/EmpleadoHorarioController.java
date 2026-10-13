package com.dev.cutly.empleado.controller;

import com.dev.cutly.empleado.dto.perfil.EmpleadoErrorResponseDto;
import com.dev.cutly.empleado.service.EmpleadoHorarioService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/empleado/horarios")
public class EmpleadoHorarioController {
    private final EmpleadoHorarioService service;

    public EmpleadoHorarioController(EmpleadoHorarioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            return ResponseEntity.ok(service.listar(emailEmpleadoAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron consultar los horarios laborales");
        }
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<?> disponibilidad(@RequestParam LocalDate fecha) {
        try {
            return ResponseEntity.ok(service.disponibilidad(fecha, emailEmpleadoAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la disponibilidad");
        }
    }

    private String emailEmpleadoAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<EmpleadoErrorResponseDto> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new EmpleadoErrorResponseDto(e.getReason()));
    }

    private ResponseEntity<EmpleadoErrorResponseDto> errorInterno(String mensaje) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmpleadoErrorResponseDto(mensaje));
    }
}
