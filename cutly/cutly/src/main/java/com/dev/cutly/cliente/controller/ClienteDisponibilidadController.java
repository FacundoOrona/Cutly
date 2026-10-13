package com.dev.cutly.cliente.controller;

import com.dev.cutly.cliente.dto.ClienteErrorResponseDto;
import com.dev.cutly.cliente.dto.disponibilidad.ClienteDisponibilidadDto;
import com.dev.cutly.cliente.service.ClienteDisponibilidadService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cliente/negocios/{negocioId}")
public class ClienteDisponibilidadController {
    private final ClienteDisponibilidadService service;

    public ClienteDisponibilidadController(ClienteDisponibilidadService service) {
        this.service = service;
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<?> listar(
            @PathVariable Long negocioId,
            @RequestParam LocalDate fecha,
            @RequestParam Long servicioId,
            @RequestParam(required = false) Long empleadoId
    ) {
        try {
            return ResponseEntity.ok(service.listar(negocioId, fecha, servicioId, empleadoId));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la disponibilidad del negocio");
        }
    }

    @GetMapping("/empleados/{empleadoId}/disponibilidad")
    public ResponseEntity<?> listarPorEmpleado(
            @PathVariable Long negocioId,
            @PathVariable Long empleadoId,
            @RequestParam LocalDate fecha,
            @RequestParam Long servicioId
    ) {
        try {
            return ResponseEntity.ok(service.listarPorEmpleado(negocioId, empleadoId, fecha, servicioId));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la disponibilidad del empleado");
        }
    }

    private ResponseEntity<ClienteErrorResponseDto> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ClienteErrorResponseDto(e.getReason()));
    }

    private ResponseEntity<ClienteErrorResponseDto> errorInterno(String mensaje) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ClienteErrorResponseDto(mensaje));
    }
}
