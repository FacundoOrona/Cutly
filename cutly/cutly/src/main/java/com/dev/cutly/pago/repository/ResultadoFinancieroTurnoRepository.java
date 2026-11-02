package com.dev.cutly.pago.repository;
import com.dev.cutly.pago.entity.ResultadoFinancieroTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
public interface ResultadoFinancieroTurnoRepository extends JpaRepository<ResultadoFinancieroTurno, Long> {
    boolean existsByTurno_TurnoId(Long turnoId);
    Optional<ResultadoFinancieroTurno> findByTurno_TurnoId(Long turnoId);

    @Query("select r.turno.turnoId from ResultadoFinancieroTurno r where r.turno.turnoId in :turnoIds")
    java.util.Set<Long> findTurnoIdsConResultadoFinanciero(@Param("turnoIds") java.util.Collection<Long> turnoIds);

    @Query("select count(r) from ResultadoFinancieroTurno r where r.negocio.negocioId = :negocioId and r.tipoResultado = com.dev.cutly.pago.enums.DecisionFinanciera.COBRO_DIRECTO and (:desde is null or r.fechaRegistro >= :desde) and (:hasta is null or r.fechaRegistro < :hasta)")
    long contarTurnosCobrados(@Param("negocioId") Long negocioId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(r) from ResultadoFinancieroTurno r where r.negocio.negocioId = :negocioId and r.tipoResultado = com.dev.cutly.pago.enums.DecisionFinanciera.GRATUIDAD and (:desde is null or r.fechaRegistro >= :desde) and (:hasta is null or r.fechaRegistro < :hasta)")
    long contarTurnosGratuitos(@Param("negocioId") Long negocioId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
