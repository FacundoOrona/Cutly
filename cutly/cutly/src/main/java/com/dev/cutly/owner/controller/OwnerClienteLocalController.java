package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.dto.cliente.ClienteLocalDto;
import com.dev.cutly.owner.dto.cliente.ClienteLocalPageDto;
import com.dev.cutly.owner.dto.cliente.CrearClienteLocalRequestDto;
import com.dev.cutly.owner.service.OwnerClienteLocalService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/clientes-locales")
public class OwnerClienteLocalController {

    private final OwnerClienteLocalService ownerClienteLocalService;

    public OwnerClienteLocalController(OwnerClienteLocalService ownerClienteLocalService) {
        this.ownerClienteLocalService = ownerClienteLocalService;
    }

    @GetMapping
    public ResponseEntity<?> buscar(
            @PathVariable Long negocioId,
            @RequestParam String q
    ) {
        try {
            List<ClienteLocalDto> clientes = ownerClienteLocalService.buscar(
                    negocioId, q, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(clientes);
        } catch (ResponseStatusException exception) {
            return respuestaError(exception);
        } catch (DataAccessException exception) {
            return errorInterno("No se pudo buscar clientes locales");
        }
    }

    @GetMapping("/paginado")
    public ResponseEntity<?> listarPaginado(
            @PathVariable Long negocioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q
    ) {
        try {
            ClienteLocalPageDto clientes = ownerClienteLocalService.listarPaginado(
                    negocioId, page, size, q, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(clientes);
        } catch (ResponseStatusException exception) {
            return respuestaError(exception);
        } catch (DataAccessException exception) {
            return errorInterno("No se pudo listar clientes locales");
        }
    }

    @PostMapping
    public ResponseEntity<?> registrar(
            @PathVariable Long negocioId,
            @Valid @RequestBody CrearClienteLocalRequestDto request
    ) {
        try {
            ClienteLocalDto cliente = ownerClienteLocalService.registrar(
                    negocioId, request, emailOwnerAutenticado()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(cliente);
        } catch (ResponseStatusException exception) {
            return respuestaError(exception);
        } catch (DataAccessException exception) {
            return errorInterno("No se pudo registrar el cliente local");
        }
    }

    private String emailOwnerAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<OwnerErrorResponseDto> respuestaError(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new OwnerErrorResponseDto(exception.getReason()));
    }

    private ResponseEntity<OwnerErrorResponseDto> errorInterno(String mensaje) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OwnerErrorResponseDto(mensaje));
    }
}
