package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.AdminReviewDto;
import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.service.AdminReviewService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping("/reviews")
    public ResponseEntity<?> listarReviews() {
        try {
            List<AdminReviewDto> reviews = adminReviewService.listarReviews();
            return ResponseEntity.ok(reviews);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la lista de reviews"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al listar las reviews"));
        }
    }

    @GetMapping("/reviews/{id}")
    public ResponseEntity<?> obtenerReview(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminReviewService.obtenerReview(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró la review con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la review"));
        }
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<?> eliminarReview(@PathVariable Long id) {
        try {
            adminReviewService.eliminarReview(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró la review con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo eliminar la review"));
        }
    }
}
