package com.dev.cutly.cliente.controller;

import com.dev.cutly.cliente.dto.ClienteErrorResponseDto;
import com.dev.cutly.cliente.dto.review.ActualizarReviewRequestDto;
import com.dev.cutly.cliente.dto.review.ClienteReviewDto;
import com.dev.cutly.cliente.dto.review.CrearReviewRequestDto;
import com.dev.cutly.cliente.service.ClienteReviewService;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/cliente/reviews")
public class ClienteReviewController {
    private final ClienteReviewService service;

    public ClienteReviewController(ClienteReviewService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearReviewRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, emailClienteAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo crear la reseña");
        }
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<ClienteReviewDto> reviews = service.listar(emailClienteAutenticado());
            return ResponseEntity.ok(reviews);
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de reseñas");
        }
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<?> actualizar(@PathVariable Long reviewId, @Valid @RequestBody ActualizarReviewRequestDto request) {
        try {
            return ResponseEntity.ok(service.actualizar(reviewId, request, emailClienteAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo actualizar la reseña");
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> eliminar(@PathVariable Long reviewId) {
        try {
            service.eliminar(reviewId, emailClienteAutenticado());
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo eliminar la reseña");
        }
    }

    private String emailClienteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<ClienteErrorResponseDto> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ClienteErrorResponseDto(e.getReason()));
    }

    private ResponseEntity<ClienteErrorResponseDto> errorInterno(String mensaje) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ClienteErrorResponseDto(mensaje));
    }
}
