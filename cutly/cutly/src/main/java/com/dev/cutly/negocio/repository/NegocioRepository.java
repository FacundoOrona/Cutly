package com.dev.cutly.negocio.repository;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NegocioRepository extends JpaRepository <Negocio, Long> {

    long countByStatus(NegocioStatus status);

    List<Negocio> findByOwner_UsuarioId(Long ownerId);

    Optional<Negocio> findByNegocioIdAndOwner_UsuarioId(Long negocioId, Long ownerId);
}
