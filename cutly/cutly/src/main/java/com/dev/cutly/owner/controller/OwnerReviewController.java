package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.dto.review.OwnerReviewDto;
import com.dev.cutly.owner.service.OwnerReviewService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/reviews")
public class OwnerReviewController {

    private final OwnerReviewService ownerReviewService;

    public OwnerReviewController(OwnerReviewService ownerReviewService) {
        this.ownerReviewService = ownerReviewService;
    }

    @GetMapping
    public ResponseEntity<?> listarReviews(@PathVariable Long negocioId) {
        try {
            List<OwnerReviewDto> reviews = ownerReviewService.listarReviews(
                    negocioId, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(reviews);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de reviews");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerReview(
            @PathVariable Long negocioId,
            @PathVariable("id") Long reviewId
    ) {
        try {
            return ResponseEntity.ok(ownerReviewService.obtenerReview(
                    negocioId, reviewId, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la review");
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
