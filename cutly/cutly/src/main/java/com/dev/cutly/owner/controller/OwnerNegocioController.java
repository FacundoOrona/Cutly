package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.negocio.ActualizarNegocioRequestDto;
import com.dev.cutly.owner.dto.negocio.CrearNegocioRequestDto;
import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.dto.negocio.OwnerNegocioDto;
import com.dev.cutly.owner.service.OwnerNegocioService;
import com.dev.cutly.negocio.enums.NegocioStatus;
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
@RequestMapping("/api/owner/negocios")
public class OwnerNegocioController {

    private final OwnerNegocioService ownerNegocioService;

    public OwnerNegocioController(OwnerNegocioService ownerNegocioService) {
        this.ownerNegocioService = ownerNegocioService;
    }

    @PostMapping
    public ResponseEntity<?> crearNegocio(@Valid @RequestBody CrearNegocioRequestDto request) {
        try {
            OwnerNegocioDto negocio = ownerNegocioService.crearNegocio(request, emailOwnerAutenticado());
            return ResponseEntity.status(HttpStatus.CREATED).body(negocio);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo crear el negocio"));
        }
    }

    @GetMapping
    public ResponseEntity<?> listarNegocios() {
        try {
            List<OwnerNegocioDto> negocios = ownerNegocioService.listarNegocios(emailOwnerAutenticado());
            return ResponseEntity.ok(negocios);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo consultar la lista de negocios"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerNegocio(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ownerNegocioService.obtenerNegocio(id, emailOwnerAutenticado()));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo consultar el negocio"));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarNegocio(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarNegocioRequestDto request
    ) {
        try {
            OwnerNegocioDto negocio = ownerNegocioService.actualizarNegocio(
                    id, request, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(negocio);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo actualizar el negocio"));
        }
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activarNegocio(@PathVariable Long id) {
        return actualizarEstado(id, NegocioStatus.ACTIVE);
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivarNegocio(@PathVariable Long id) {
        return actualizarEstado(id, NegocioStatus.INACTIVE);
    }

    private ResponseEntity<?> actualizarEstado(Long id, NegocioStatus status) {
        try {
            OwnerNegocioDto negocio = ownerNegocioService.actualizarEstado(
                    id, status, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(negocio);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo actualizar el estado del negocio"));
        }
    }

    private String emailOwnerAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<OwnerErrorResponseDto> respuestaError(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new OwnerErrorResponseDto(e.getReason()));
    }
}

