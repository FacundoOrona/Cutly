package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.AdminReviewDto;
import com.dev.cutly.review.entity.Review;
import com.dev.cutly.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AdminReviewService {

    private final ReviewRepository reviewRepository;

    public AdminReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminReviewDto> listarReviews() {
        return reviewRepository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminReviewDto obtenerReview(Long id) {
        return reviewRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la review con id " + id));
    }

    @Transactional
    public void eliminarReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la review con id " + id));
        reviewRepository.delete(review);
    }

    private AdminReviewDto aDto(Review review) {
        return new AdminReviewDto(
                review.getReviewId(),
                review.getPuntuacion(),
                review.getComentario(),
                review.getCliente().getUsuarioId(),
                review.getCliente().getNombre(),
                review.getNegocio().getNegocioId(),
                review.getNegocio().getNombre(),
                review.getTurno().getTurnoId()
        );
    }
}
