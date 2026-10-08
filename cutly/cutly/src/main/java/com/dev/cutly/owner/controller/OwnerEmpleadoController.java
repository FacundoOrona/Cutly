package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.empleado.ActualizarEmpleadoRequestDto;
import com.dev.cutly.owner.dto.empleado.CrearEmpleadoRequestDto;
import com.dev.cutly.owner.dto.empleado.OwnerEmpleadoDto;
import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.service.OwnerEmpleadoService;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/empleados")
public class OwnerEmpleadoController {

    private final OwnerEmpleadoService ownerEmpleadoService;

    public OwnerEmpleadoController(OwnerEmpleadoService ownerEmpleadoService) {
        this.ownerEmpleadoService = ownerEmpleadoService;
    }

    @GetMapping
    public ResponseEntity<?> listarEmpleados(@PathVariable Long negocioId) {
        try {
            List<OwnerEmpleadoDto> empleados = ownerEmpleadoService.listarEmpleados(
                    negocioId, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(empleados);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo consultar la lista de empleados"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerEmpleado(
            @PathVariable Long negocioId,
            @PathVariable("id") Long empleadoId
    ) {
        try {
            return ResponseEntity.ok(ownerEmpleadoService.obtenerEmpleado(
                    negocioId, empleadoId, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo consultar el empleado"));
        }
    }

    @PostMapping
    public ResponseEntity<?> agregarEmpleado(
            @PathVariable Long negocioId,
            @Valid @RequestBody CrearEmpleadoRequestDto request
    ) {
        try {
            OwnerEmpleadoDto empleado = ownerEmpleadoService.agregarEmpleado(
                    negocioId, request, emailOwnerAutenticado()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(empleado);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo agregar el empleado"));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarEmpleado(
            @PathVariable Long negocioId,
            @PathVariable("id") Long empleadoId,
            @Valid @RequestBody ActualizarEmpleadoRequestDto request
    ) {
        try {
            OwnerEmpleadoDto empleado = ownerEmpleadoService.actualizarEmpleado(
                    negocioId, empleadoId, request, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(empleado);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo modificar el empleado"));
        }
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activarEmpleado(
            @PathVariable Long negocioId,
            @PathVariable("id") Long empleadoId
    ) {
        return actualizarEstado(negocioId, empleadoId, true);
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivarEmpleado(
            @PathVariable Long negocioId,
            @PathVariable("id") Long empleadoId
    ) {
        return actualizarEstado(negocioId, empleadoId, false);
    }

    private ResponseEntity<?> actualizarEstado(Long negocioId, Long empleadoId, boolean activo) {
        try {
            OwnerEmpleadoDto empleado = ownerEmpleadoService.actualizarEstado(
                    negocioId, empleadoId, activo, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(empleado);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo actualizar el estado del empleado"));
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
}
