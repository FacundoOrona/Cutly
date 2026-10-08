package com.dev.cutly.pago.repository;

import com.dev.cutly.pago.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("select sum(p.monto) from Pago p")
    BigDecimal sumarMontos();

    List<Pago> findByTurno_Negocio_NegocioIdOrderByFechaPagoDescPagoIdDesc(Long negocioId);

    Optional<Pago> findByPagoIdAndTurno_Negocio_NegocioId(Long pagoId, Long negocioId);

    boolean existsByTurno_TurnoId(Long turnoId);
}
