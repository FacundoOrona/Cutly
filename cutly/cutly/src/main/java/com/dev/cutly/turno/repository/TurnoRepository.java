package com.dev.cutly.turno.repository;

import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.owner.dto.estadistica.OwnerEmpleadoEstadisticaDto;
import com.dev.cutly.owner.dto.estadistica.OwnerServicioEstadisticaDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, Long>, JpaSpecificationExecutor<Turno> {

    long countByStatus(TurnoStatus status);

    List<Turno> findByNegocio_NegocioIdOrderByFechaHoraInicioAscTurnoIdAsc(Long negocioId);

    Optional<Turno> findByTurnoIdAndNegocio_NegocioId(Long turnoId, Long negocioId);

    @Query("select t.status, count(t) from Turno t "
            + "where t.negocio.negocioId = :negocioId "
            + "and (:desde is null or t.fechaHoraInicio >= :desde) "
            + "and (:hasta is null or t.fechaHoraInicio < :hasta) "
            + "group by t.status")
    List<Object[]> contarPorEstadoYPeriodo(
            @Param("negocioId") Long negocioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("select new com.dev.cutly.owner.dto.estadistica.OwnerEmpleadoEstadisticaDto("
            + "e.empleadoId, u.nombre, u.apellido, count(t), "
            + "count(case when t.status = com.dev.cutly.turno.enums.TurnoStatus.COMPLETED then t.turnoId else null end), "
            + "count(case when t.status = com.dev.cutly.turno.enums.TurnoStatus.CANCELLED then t.turnoId else null end)) "
            + "from Turno t join t.empleado e join e.usuario u "
            + "where t.negocio.negocioId = :negocioId "
            + "and (:desde is null or t.fechaHoraInicio >= :desde) "
            + "and (:hasta is null or t.fechaHoraInicio < :hasta) "
            + "group by e.empleadoId, u.nombre, u.apellido order by e.empleadoId")
    List<OwnerEmpleadoEstadisticaDto> estadisticasPorEmpleado(
            @Param("negocioId") Long negocioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("select new com.dev.cutly.owner.dto.estadistica.OwnerServicioEstadisticaDto("
            + "s.servicioId, s.nombre, count(t), "
            + "count(case when t.status = com.dev.cutly.turno.enums.TurnoStatus.COMPLETED then t.turnoId else null end), "
            + "count(case when t.status = com.dev.cutly.turno.enums.TurnoStatus.CANCELLED then t.turnoId else null end)) "
            + "from Turno t join t.servicio s "
            + "where t.negocio.negocioId = :negocioId "
            + "and (:desde is null or t.fechaHoraInicio >= :desde) "
            + "and (:hasta is null or t.fechaHoraInicio < :hasta) "
            + "group by s.servicioId, s.nombre order by s.servicioId")
    List<OwnerServicioEstadisticaDto> estadisticasPorServicio(
            @Param("negocioId") Long negocioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
