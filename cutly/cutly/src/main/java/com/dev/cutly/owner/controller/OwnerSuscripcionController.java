package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.service.OwnerNegocioService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/owner/suscripcion")
public class OwnerSuscripcionController {
    private final OwnerNegocioService ownerNegocioService;

    public OwnerSuscripcionController(OwnerNegocioService ownerNegocioService) {
        this.ownerNegocioService = ownerNegocioService;
    }

    @GetMapping
    public ResponseEntity<?> consultar() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication == null ? null : authentication.getName();
            return ResponseEntity.ok(ownerNegocioService.consultarSuscripcion(email));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new OwnerErrorResponseDto(e.getReason()));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo consultar la suscripción"));
        }
    }
}
