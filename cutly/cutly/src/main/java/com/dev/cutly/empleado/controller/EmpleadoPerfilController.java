package com.dev.cutly.empleado.controller;

import com.dev.cutly.empleado.dto.perfil.ActualizarEmpleadoPerfilRequestDto;
import com.dev.cutly.empleado.dto.perfil.EmpleadoErrorResponseDto;
import com.dev.cutly.empleado.service.EmpleadoPerfilService;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/empleado/perfil")
public class EmpleadoPerfilController {
    private final EmpleadoPerfilService service;

    public EmpleadoPerfilController(EmpleadoPerfilService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> obtener() {
        try {
            return ResponseEntity.ok(service.obtener(emailEmpleadoAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el perfil del empleado");
        }
    }

    @PatchMapping
    public ResponseEntity<?> actualizar(@Valid @RequestBody ActualizarEmpleadoPerfilRequestDto request) {
        try {
            return ResponseEntity.ok(service.actualizar(request, emailEmpleadoAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo actualizar el perfil del empleado");
        }
    }

    @GetMapping("/negocio")
    public ResponseEntity<?> obtenerNegocio() {
        try {
            return ResponseEntity.ok(service.obtenerNegocio(emailEmpleadoAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el negocio del empleado");
        }
    }

    @GetMapping("/servicios")
    public ResponseEntity<?> obtenerServicios() {
        try {
            return ResponseEntity.ok(service.obtenerServicios(emailEmpleadoAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron consultar los servicios asignados");
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
