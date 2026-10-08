package com.dev.cutly.owner.service;

import com.dev.cutly.owner.dto.review.OwnerReviewDto;
import com.dev.cutly.review.entity.Review;
import com.dev.cutly.review.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OwnerReviewService {

    private final ReviewRepository reviewRepository;
    private final OwnerNegocioService ownerNegocioService;

    public OwnerReviewService(ReviewRepository reviewRepository, OwnerNegocioService ownerNegocioService) {
        this.reviewRepository = reviewRepository;
        this.ownerNegocioService = ownerNegocioService;
    }

    public List<OwnerReviewDto> listarReviews(Long negocioId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return reviewRepository.findByNegocio_NegocioIdOrderByReviewIdDesc(negocioId)
                .stream().map(this::aDto).toList();
    }

    public OwnerReviewDto obtenerReview(Long negocioId, Long reviewId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Review review = reviewRepository.findByReviewIdAndNegocio_NegocioId(reviewId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró la review solicitada"));
        return aDto(review);
    }

    private OwnerReviewDto aDto(Review review) {
        return new OwnerReviewDto(
                review.getReviewId(),
                review.getPuntuacion(),
                review.getComentario(),
                review.getCliente().getUsuarioId(),
                review.getCliente().getNombre(),
                review.getNegocio().getNegocioId(),
                review.getTurno().getTurnoId()
        );
    }
}
