package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.service.OwnerTransaccionesService;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.pago.enums.TipoTransaccion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/transacciones")
public class OwnerTransaccionesController {
    private static final Logger log = LoggerFactory.getLogger(OwnerTransaccionesController.class);
    private final OwnerTransaccionesService service;

    public OwnerTransaccionesController(OwnerTransaccionesService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> listar(@PathVariable Long negocioId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) List<MetodoPago> metodoPago,
            @RequestParam(required = false) TipoTransaccion tipo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long clienteLocalId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaPago,desc") String sort) {
        try {
            return ResponseEntity.ok(service.listar(negocioId, desde, hasta, metodoPago, tipo, clienteId,
                    clienteLocalId, q, page, size, sort, emailOwner()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new OwnerErrorResponseDto(e.getReason()));
        } catch (Exception e) {
            log.error("Error consultando transacciones del negocio {}", negocioId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudieron consultar las transacciones"));
        }
    }

    private String emailOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
