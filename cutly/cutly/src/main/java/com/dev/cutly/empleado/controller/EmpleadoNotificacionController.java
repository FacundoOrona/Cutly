package com.dev.cutly.empleado.controller;

import com.dev.cutly.empleado.dto.notificacion.EmpleadoNotificacionDto;
import com.dev.cutly.empleado.dto.notificacion.EmpleadoNotificacionesLeidasDto;
import com.dev.cutly.empleado.dto.perfil.EmpleadoErrorResponseDto;
import com.dev.cutly.empleado.service.EmpleadoNotificacionService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/empleado/notificaciones")
public class EmpleadoNotificacionController {
    private final EmpleadoNotificacionService service;

    public EmpleadoNotificacionController(EmpleadoNotificacionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<EmpleadoNotificacionDto> notificaciones = service.listar(emailEmpleadoAutenticado());
            return ResponseEntity.ok(notificaciones);
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de notificaciones");
        }
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<?> marcarComoLeida(@PathVariable("id") Long notificacionId) {
        try {
            return ResponseEntity.ok(service.marcarComoLeida(notificacionId, emailEmpleadoAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo marcar la notificación como leída");
        }
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<?> marcarTodasComoLeidas() {
        try {
            int actualizadas = service.marcarTodasComoLeidas(emailEmpleadoAutenticado());
            return ResponseEntity.ok(new EmpleadoNotificacionesLeidasDto(actualizadas));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron marcar las notificaciones como leídas");
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
