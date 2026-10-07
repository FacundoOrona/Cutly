package com.dev.cutly.negocio.repository;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository <Negocio, Long> {

    long countByStatus(NegocioStatus status);
}
