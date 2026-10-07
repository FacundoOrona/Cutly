package com.dev.cutly.negocio.repository;

import com.dev.cutly.negocio.entity.DireccionNegocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DireccionRepository extends JpaRepository<DireccionNegocio, Long> {

    Optional<DireccionNegocio> findByNegocio_NegocioId(Long negocioId);

    boolean existsByNegocio_NegocioId(Long negocioId);
}
