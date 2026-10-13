package com.dev.cutly.empleado.controller;

import com.dev.cutly.empleado.dto.perfil.EmpleadoErrorResponseDto;
import com.dev.cutly.empleado.dto.turno.EmpleadoTurnoDto;
import com.dev.cutly.empleado.service.EmpleadoTurnoService;
import com.dev.cutly.turno.enums.TurnoStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;

@RestController
@RequestMapping("/api/empleado/turnos")
public class EmpleadoTurnoController {
    private final EmpleadoTurnoService service;

    public EmpleadoTurnoController(EmpleadoTurnoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) LocalDate fecha,
                                    @RequestParam(required = false) TurnoStatus status) {
        return consultar(fecha, status, false);
    }

    @GetMapping("/historial")
    public ResponseEntity<?> historial(@RequestParam(required = false) LocalDate fecha,
                                       @RequestParam(required = false) TurnoStatus status) {
        return consultar(fecha, status, true);
    }

    @GetMapping("/{turnoId}")
    public ResponseEntity<?> obtener(@PathVariable Long turnoId) {
        try {
            return ResponseEntity.ok(service.obtener(turnoId, emailEmpleadoAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el turno");
        }
    }

    @PatchMapping("/{turnoId}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long turnoId) {
        return actualizar(turnoId, service::confirmar, "No se pudo confirmar el turno");
    }

    @PatchMapping("/{turnoId}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long turnoId) {
        return actualizar(turnoId, service::cancelar, "No se pudo cancelar el turno");
    }

    @PatchMapping("/{turnoId}/completar")
    public ResponseEntity<?> completar(@PathVariable Long turnoId) {
        return actualizar(turnoId, service::completar, "No se pudo completar el turno");
    }

    @PatchMapping("/{turnoId}/no-show")
    public ResponseEntity<?> noShow(@PathVariable Long turnoId) {
        return actualizar(turnoId, service::noShow, "No se pudo registrar la ausencia del cliente");
    }

    private ResponseEntity<?> consultar(LocalDate fecha, TurnoStatus status, boolean historial) {
        try {
            List<EmpleadoTurnoDto> turnos = service.listar(emailEmpleadoAutenticado(), fecha, status, historial);
            return ResponseEntity.ok(turnos);
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de turnos");
        }
    }

    private ResponseEntity<?> actualizar(Long turnoId, BiFunction<Long, String, EmpleadoTurnoDto> actualizador, String mensaje) {
        try {
            return ResponseEntity.ok(actualizador.apply(turnoId, emailEmpleadoAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno(mensaje);
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
