package com.dev.cutly.review.repository;

import com.dev.cutly.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review,Long> {

    List<Review> findByNegocio_NegocioIdOrderByReviewIdDesc(Long negocioId);

    Optional<Review> findByReviewIdAndNegocio_NegocioId(Long reviewId, Long negocioId);
}
