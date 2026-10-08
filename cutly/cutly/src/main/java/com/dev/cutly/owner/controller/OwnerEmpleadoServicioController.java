package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.dto.servicio.OwnerServicioDto;
import com.dev.cutly.owner.service.OwnerEmpleadoServicioService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/empleados/{empleadoId}/servicios")
public class OwnerEmpleadoServicioController {

    private final OwnerEmpleadoServicioService ownerEmpleadoServicioService;

    public OwnerEmpleadoServicioController(OwnerEmpleadoServicioService ownerEmpleadoServicioService) {
        this.ownerEmpleadoServicioService = ownerEmpleadoServicioService;
    }

    @GetMapping
    public ResponseEntity<?> listarServiciosAsignados(
            @PathVariable Long negocioId,
            @PathVariable Long empleadoId
    ) {
        try {
            List<OwnerServicioDto> servicios = ownerEmpleadoServicioService.listarServiciosAsignados(
                    negocioId, empleadoId, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(servicios);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar los servicios asignados al empleado");
        }
    }

    @PostMapping("/{servicioId}")
    public ResponseEntity<?> asignarServicio(
            @PathVariable Long negocioId,
            @PathVariable Long empleadoId,
            @PathVariable Long servicioId
    ) {
        try {
            OwnerServicioDto servicio = ownerEmpleadoServicioService.asignarServicio(
                    negocioId, empleadoId, servicioId, emailOwnerAutenticado()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(servicio);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo asignar el servicio al empleado");
        }
    }

    @DeleteMapping("/{servicioId}")
    public ResponseEntity<?> quitarServicio(
            @PathVariable Long negocioId,
            @PathVariable Long empleadoId,
            @PathVariable Long servicioId
    ) {
        try {
            ownerEmpleadoServicioService.quitarServicio(
                    negocioId, empleadoId, servicioId, emailOwnerAutenticado()
            );
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo quitar el servicio al empleado");
        }
    }

    private String emailOwnerAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<OwnerErrorResponseDto> respuestaError(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new OwnerErrorResponseDto(e.getReason()));
    }

    private ResponseEntity<OwnerErrorResponseDto> errorInterno(String mensaje) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OwnerErrorResponseDto(mensaje));
    }
}
