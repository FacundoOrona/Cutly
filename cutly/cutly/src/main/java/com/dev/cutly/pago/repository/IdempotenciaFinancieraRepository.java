package com.dev.cutly.pago.repository;
import com.dev.cutly.pago.entity.IdempotenciaFinanciera;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface IdempotenciaFinancieraRepository extends JpaRepository<IdempotenciaFinanciera, Long> {
    Optional<IdempotenciaFinanciera> findByKey(String key);
    Optional<IdempotenciaFinanciera> findByKeyAndNegocio_NegocioIdAndTurno_TurnoId(String key, Long negocioId, Long turnoId);
}
