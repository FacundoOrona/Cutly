package com.dev.cutly.remuneracion.repository;

import com.dev.cutly.remuneracion.entity.RemuneracionEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
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
    org.springframework.data.domain.Page<RemuneracionEmpleado> listar(@Param("negocioId") Long negocioId,
            @Param("empleadoId") Long empleadoId, @Param("estado") com.dev.cutly.remuneracion.enums.EstadoRemuneracion estado,
            @Param("desde") java.time.LocalDateTime desde, @Param("hasta") java.time.LocalDateTime hasta,
            org.springframework.data.domain.Pageable pageable);

    java.util.List<RemuneracionEmpleado> findByNegocio_NegocioIdAndEmpleado_EmpleadoId(Long negocioId, Long empleadoId);
    java.util.List<RemuneracionEmpleado> findByNegocio_NegocioId(Long negocioId);
}
