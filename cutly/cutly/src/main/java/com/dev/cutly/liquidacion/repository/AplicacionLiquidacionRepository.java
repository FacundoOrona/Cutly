package com.dev.cutly.liquidacion.repository;

import com.dev.cutly.liquidacion.entity.AplicacionLiquidacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface AplicacionLiquidacionRepository extends JpaRepository<AplicacionLiquidacion, Long> {
    @Query("select coalesce(sum(a.importe),0) from AplicacionLiquidacion a where a.remuneracion.remuneracionId=:id " +
            "and a.liquidacion.estado=com.dev.cutly.liquidacion.enums.EstadoLiquidacion.VIGENTE")
    BigDecimal totalVigente(@Param("id") Long remuneracionId);
    @Query("select a.remuneracion.remuneracionId, sum(a.importe) from AplicacionLiquidacion a " +
            "where a.liquidacion.estado=com.dev.cutly.liquidacion.enums.EstadoLiquidacion.VIGENTE " +
            "and a.remuneracion.remuneracionId in :ids group by a.remuneracion.remuneracionId")
    List<Object[]> totalesVigentes(@Param("ids") java.util.Collection<Long> ids);
    List<AplicacionLiquidacion> findByLiquidacion_LiquidacionIdOrderByAplicacionLiquidacionId(Long liquidacionId);
    @EntityGraph(attributePaths = {"remuneracion", "remuneracion.turno", "remuneracion.turno.servicio",
            "remuneracion.turno.cliente", "remuneracion.turno.clienteLocal"})
    List<AplicacionLiquidacion> findByLiquidacion_LiquidacionIdInOrderByAplicacionLiquidacionId(java.util.Collection<Long> liquidacionIds);
    List<AplicacionLiquidacion> findByRemuneracion_RemuneracionIdOrderByAplicacionLiquidacionId(Long remuneracionId);
    long countByLiquidacion_Negocio_NegocioId(Long negocioId);
}
