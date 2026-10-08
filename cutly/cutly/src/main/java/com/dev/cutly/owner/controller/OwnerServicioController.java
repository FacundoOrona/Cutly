package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.dto.servicio.ActualizarServicioRequestDto;
import com.dev.cutly.owner.dto.servicio.CrearServicioRequestDto;
import com.dev.cutly.owner.dto.servicio.OwnerServicioDto;
import com.dev.cutly.owner.service.OwnerServicioService;
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
@RequestMapping("/api/owner/negocios/{negocioId}/servicios")
public class OwnerServicioController {

    private final OwnerServicioService ownerServicioService;

    public OwnerServicioController(OwnerServicioService ownerServicioService) {
        this.ownerServicioService = ownerServicioService;
    }

    @GetMapping
    public ResponseEntity<?> listarServicios(@PathVariable Long negocioId) {
        try {
            List<OwnerServicioDto> servicios = ownerServicioService.listarServicios(
                    negocioId, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(servicios);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de servicios");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerServicio(
            @PathVariable Long negocioId,
            @PathVariable("id") Long servicioId
    ) {
        try {
            return ResponseEntity.ok(ownerServicioService.obtenerServicio(
                    negocioId, servicioId, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el servicio");
        }
    }

    @PostMapping
    public ResponseEntity<?> crearServicio(
            @PathVariable Long negocioId,
            @Valid @RequestBody CrearServicioRequestDto request
    ) {
        try {
            OwnerServicioDto servicio = ownerServicioService.crearServicio(
                    negocioId, request, emailOwnerAutenticado()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(servicio);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo crear el servicio");
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarServicio(
            @PathVariable Long negocioId,
            @PathVariable("id") Long servicioId,
            @Valid @RequestBody ActualizarServicioRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ownerServicioService.actualizarServicio(
                    negocioId, servicioId, request, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo modificar el servicio");
        }
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activarServicio(
            @PathVariable Long negocioId,
            @PathVariable("id") Long servicioId
    ) {
        return actualizarEstado(negocioId, servicioId, true);
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivarServicio(
            @PathVariable Long negocioId,
            @PathVariable("id") Long servicioId
    ) {
        return actualizarEstado(negocioId, servicioId, false);
    }

    private ResponseEntity<?> actualizarEstado(Long negocioId, Long servicioId, boolean activo) {
        try {
            return ResponseEntity.ok(ownerServicioService.actualizarEstado(
                    negocioId, servicioId, activo, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo actualizar el estado del servicio");
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
