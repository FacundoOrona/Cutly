package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.dto.pago.CrearPagoRequestDto;
import com.dev.cutly.owner.dto.pago.OwnerPagoDto;
import com.dev.cutly.owner.service.OwnerPagosService;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/pagos")
public class OwnerPagosController {

    private final OwnerPagosService ownerPagosService;

    public OwnerPagosController(OwnerPagosService ownerPagosService) {
        this.ownerPagosService = ownerPagosService;
    }

    @GetMapping
    public ResponseEntity<?> listarPagos(@PathVariable Long negocioId) {
        try {
            List<OwnerPagoDto> pagos = ownerPagosService.listarPagos(
                    negocioId, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(pagos);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de pagos");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPago(
            @PathVariable Long negocioId,
            @PathVariable("id") Long pagoId
    ) {
        try {
            return ResponseEntity.ok(ownerPagosService.obtenerPago(
                    negocioId, pagoId, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el pago");
        }
    }

    @PostMapping
    public ResponseEntity<?> registrarPago(
            @PathVariable Long negocioId,
            @Valid @RequestBody CrearPagoRequestDto request
    ) {
        try {
            OwnerPagoDto pago = ownerPagosService.registrarPago(
                    negocioId, request, emailOwnerAutenticado()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(pago);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo registrar el pago");
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
