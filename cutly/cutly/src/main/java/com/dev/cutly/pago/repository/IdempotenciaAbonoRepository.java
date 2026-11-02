package com.dev.cutly.pago.repository;

import com.dev.cutly.pago.entity.IdempotenciaAbono;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotenciaAbonoRepository extends JpaRepository<IdempotenciaAbono, Long> {
    Optional<IdempotenciaAbono> findByKey(String key);
}
