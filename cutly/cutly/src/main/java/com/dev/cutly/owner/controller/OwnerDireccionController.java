package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.direccion.ActualizarDireccionRequestDto;
import com.dev.cutly.owner.dto.direccion.CrearDireccionRequestDto;
import com.dev.cutly.owner.dto.direccion.OwnerDireccionDto;
import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.service.OwnerDireccionService;
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

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/direccion")
public class OwnerDireccionController {

    private final OwnerDireccionService ownerDireccionService;

    public OwnerDireccionController(OwnerDireccionService ownerDireccionService) {
        this.ownerDireccionService = ownerDireccionService;
    }

    @PostMapping
    public ResponseEntity<?> crearDireccion(
            @PathVariable Long negocioId,
            @Valid @RequestBody CrearDireccionRequestDto request
    ) {
        try {
            OwnerDireccionDto direccion = ownerDireccionService.crearDireccion(
                    negocioId, request, emailOwnerAutenticado()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(direccion);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo crear la dirección del negocio"));
        }
    }

    @GetMapping
    public ResponseEntity<?> obtenerDireccion(@PathVariable Long negocioId) {
        try {
            return ResponseEntity.ok(ownerDireccionService.obtenerDireccion(
                    negocioId, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo consultar la dirección del negocio"));
        }
    }

    @PatchMapping
    public ResponseEntity<?> actualizarDireccion(
            @PathVariable Long negocioId,
            @Valid @RequestBody ActualizarDireccionRequestDto request
    ) {
        try {
            OwnerDireccionDto direccion = ownerDireccionService.actualizarDireccion(
                    negocioId, request, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(direccion);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo actualizar la dirección del negocio"));
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
