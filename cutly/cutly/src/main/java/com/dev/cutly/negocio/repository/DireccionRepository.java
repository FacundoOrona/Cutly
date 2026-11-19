package com.dev.cutly.negocio.repository;

import com.dev.cutly.negocio.entity.DireccionNegocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface DireccionRepository extends JpaRepository<DireccionNegocio, Long> {

    Optional<DireccionNegocio> findByNegocio_NegocioId(Long negocioId);

    boolean existsByNegocio_NegocioId(Long negocioId);

    List<DireccionNegocio> findAllByNegocio_NegocioIdIn(Collection<Long> negocioIds);
}
