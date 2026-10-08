package com.dev.cutly.pago.repository;

import com.dev.cutly.pago.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("select sum(p.monto) from Pago p")
    BigDecimal sumarMontos();

    List<Pago> findByTurno_Negocio_NegocioIdOrderByFechaPagoDescPagoIdDesc(Long negocioId);

    Optional<Pago> findByPagoIdAndTurno_Negocio_NegocioId(Long pagoId, Long negocioId);

    boolean existsByTurno_TurnoId(Long turnoId);

    @Query("select coalesce(sum(p.monto), 0) from Pago p "
            + "where p.turno.negocio.negocioId = :negocioId "
            + "and (:desde is null or p.fechaPago >= :desde) "
            + "and (:hasta is null or p.fechaPago < :hasta)")
    BigDecimal sumarMontosPorNegocioYPeriodo(
            @Param("negocioId") Long negocioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("select count(p) from Pago p "
            + "where p.turno.negocio.negocioId = :negocioId "
            + "and (:desde is null or p.fechaPago >= :desde) "
            + "and (:hasta is null or p.fechaPago < :hasta)")
    long contarPorNegocioYPeriodo(
            @Param("negocioId") Long negocioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
