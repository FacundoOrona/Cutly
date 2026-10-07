package com.dev.cutly.negocio.repository;

import com.dev.cutly.negocio.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository <Negocio, Long> {
}
