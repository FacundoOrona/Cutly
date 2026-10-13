package com.dev.cutly.cliente.controller;

import com.dev.cutly.cliente.dto.ClienteErrorResponseDto;
import com.dev.cutly.cliente.dto.cliente.ClienteTurnoDto;
import com.dev.cutly.cliente.dto.cliente.CrearTurnoRequestDto;
import com.dev.cutly.cliente.service.ClienteTurnoService;
import com.dev.cutly.turno.enums.TurnoStatus;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cliente/turnos")
public class ClienteTurnoController {
    private final ClienteTurnoService service;

    public ClienteTurnoController(ClienteTurnoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> crearTurno(@Valid @RequestBody CrearTurnoRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, emailClienteAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo solicitar el turno");
        }
    }

    @GetMapping
    public ResponseEntity<?> listarTurno(@RequestParam(required = false) LocalDate fecha,
                                    @RequestParam(required = false) TurnoStatus status) {
        return listarTurnos(fecha, status, false);
    }

    @GetMapping("/historial")
    public ResponseEntity<?> historialTurnos(@RequestParam(required = false) LocalDate fecha,
                                       @RequestParam(required = false) TurnoStatus status) {
        return listarTurnos(fecha, status, true);
    }

    @GetMapping("/{turnoId}")
    public ResponseEntity<?> obtenerTurno(@PathVariable Long turnoId) {
        try {
            return ResponseEntity.ok(service.obtener(turnoId, emailClienteAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el turno");
        }
    }

    @PatchMapping("/{turnoId}/cancelar")
    public ResponseEntity<?> cancelarTurno(@PathVariable Long turnoId) {
        try {
            return ResponseEntity.ok(service.cancelar(turnoId, emailClienteAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo cancelar el turno");
        }
    }

    private ResponseEntity<?> listarTurnos(LocalDate fecha, TurnoStatus status, boolean historial) {
        try {
            List<ClienteTurnoDto> turnos = service.listar(emailClienteAutenticado(), fecha, status, historial);
            return ResponseEntity.ok(turnos);
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de turnos");
        }
    }

    private String emailClienteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<ClienteErrorResponseDto> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ClienteErrorResponseDto(e.getReason()));
    }

    private ResponseEntity<ClienteErrorResponseDto> errorInterno(String mensaje) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ClienteErrorResponseDto(mensaje));
    }
}
