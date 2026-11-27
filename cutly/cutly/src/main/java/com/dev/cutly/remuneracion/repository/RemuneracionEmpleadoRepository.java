package com.dev.cutly.remuneracion.repository;

import com.dev.cutly.remuneracion.entity.RemuneracionEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface RemuneracionEmpleadoRepository extends JpaRepository<RemuneracionEmpleado, Long> {
    Optional<RemuneracionEmpleado> findByTurno_TurnoId(Long turnoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RemuneracionEmpleado r where r.remuneracionId in :ids order by r.remuneracionId")
    java.util.List<RemuneracionEmpleado> bloquearEnOrden(@Param("ids") java.util.Collection<Long> ids);

    @Query("select r from RemuneracionEmpleado r where r.negocio.negocioId=:negocioId " +
            "and (:empleadoId is null or r.empleado.empleadoId=:empleadoId) " +
            "and (:estado is null or r.estado=:estado) " +
            "and (:desde is null or r.turno.fechaHoraInicio>=:desde) and (:hasta is null or r.turno.fechaHoraInicio<:hasta)")
    @EntityGraph(attributePaths = {"empleado", "turno", "turno.servicio", "turno.cliente", "turno.clienteLocal"})
    org.springframework.data.domain.Page<RemuneracionEmpleado> listar(@Param("negocioId") Long negocioId,
            @Param("empleadoId") Long empleadoId, @Param("estado") com.dev.cutly.remuneracion.enums.EstadoRemuneracion estado,
            @Param("desde") java.time.LocalDateTime desde, @Param("hasta") java.time.LocalDateTime hasta,
            org.springframework.data.domain.Pageable pageable);

    java.util.List<RemuneracionEmpleado> findByNegocio_NegocioIdAndEmpleado_EmpleadoId(Long negocioId, Long empleadoId);
    java.util.List<RemuneracionEmpleado> findByNegocio_NegocioId(Long negocioId);
    @EntityGraph(attributePaths = {"empleado", "turno", "turno.servicio", "turno.cliente", "turno.clienteLocal"})
    java.util.Optional<RemuneracionEmpleado> findByRemuneracionIdAndNegocio_NegocioId(Long remuneracionId, Long negocioId);

    @Query("select r.empleado.empleadoId, r.estado, r.montoCalculado, " +
            "coalesce(sum(case when a.liquidacion.estado=com.dev.cutly.liquidacion.enums.EstadoLiquidacion.VIGENTE then a.importe else 0 end), 0) " +
            "from RemuneracionEmpleado r left join com.dev.cutly.liquidacion.entity.AplicacionLiquidacion a on a.remuneracion=r " +
            "where r.negocio.negocioId=:negocioId " +
            "group by r.remuneracionId, r.empleado.empleadoId, r.estado, r.montoCalculado")
    java.util.List<Object[]> resumenPorEmpleado(@Param("negocioId") Long negocioId);
}
