package com.dev.cutly.turno.repository;

import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.owner.dto.estadistica.OwnerEmpleadoEstadisticaDto;
import com.dev.cutly.owner.dto.estadistica.OwnerServicioEstadisticaDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface TurnoRepository extends JpaRepository<Turno, Long>, JpaSpecificationExecutor<Turno> {
    @Override
    @EntityGraph(attributePaths = {"negocio", "cliente", "clienteLocal", "empleado.usuario", "ownerResponsable", "servicio"})
    List<Turno> findAll(Specification<Turno> spec, Sort sort);
    @Query("select n, count(t) from Turno t join t.negocio n "
            + "where n.status = com.dev.cutly.negocio.enums.NegocioStatus.ACTIVE "
            + "and t.fechaHoraInicio >= :desde and t.fechaHoraInicio < :hasta "
            + "and t.status not in (com.dev.cutly.turno.enums.TurnoStatus.CANCELLED, com.dev.cutly.turno.enums.TurnoStatus.REJECTED) "
            + "and exists (select s.suscripcionId from com.dev.cutly.suscripcion.entity.Suscripcion s "
            + "where s.negocio.negocioId = n.negocioId and ("
            + "s.status = com.dev.cutly.suscripcion.enums.SuscripcionStatus.ACTIVE "
            + "or s.status = com.dev.cutly.suscripcion.enums.SuscripcionStatus.EXPIRED "
            + "or (s.status = com.dev.cutly.suscripcion.enums.SuscripcionStatus.TRIAL "
            + "and s.fechaVencimiento >= :hoy))) "
            + "group by n order by count(t) desc, n.negocioId asc")
    List<Object[]> rankingNegociosClientes(@Param("desde") LocalDateTime desde,
                                           @Param("hasta") LocalDateTime hasta,
                                           @Param("hoy") LocalDate hoy,
                                           Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Turno t where t.turnoId = :turnoId and t.negocio.negocioId = :negocioId")
    Optional<Turno> findWithLockingByTurnoIdAndNegocioId(@Param("turnoId") Long turnoId, @Param("negocioId") Long negocioId);

    long countByStatus(TurnoStatus status);

    @Query("select t.status, count(t) from Turno t where t.fechaHoraInicio >= :desde and t.fechaHoraInicio < :hasta group by t.status")
    List<Object[]> contarPorEstadoPeriodoGlobal(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select function('date', t.fechaHoraInicio), count(t) from Turno t "
            + "where t.fechaHoraInicio >= :desde and t.fechaHoraInicio < :hasta "
            + "group by function('date', t.fechaHoraInicio) order by function('date', t.fechaHoraInicio)")
    List<Object[]> evolucionDiariaGlobal(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(distinct t.negocio.negocioId) from Turno t where t.fechaHoraInicio >= :desde and t.fechaHoraInicio < :hasta")
    long contarNegociosConActividad(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(t) from Turno t where t.negocio.negocioId = :negocioId and t.status = com.dev.cutly.turno.enums.TurnoStatus.COMPLETED and not exists (select r.resultadoFinancieroId from com.dev.cutly.pago.entity.ResultadoFinancieroTurno r where r.turno = t) and (:desde is null or t.fechaHoraInicio >= :desde) and (:hasta is null or t.fechaHoraInicio < :hasta)")
    long contarCompletadosSinRegularizacion(@Param("negocioId") Long negocioId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    List<Turno> findByNegocio_NegocioIdOrderByFechaHoraInicioAscTurnoIdAsc(Long negocioId);

    @Query("select t from Turno t "
            + "where t.negocio.negocioId = :negocioId "
            + "and t.fechaHoraInicio < :hasta "
            + "and t.fechaHoraFin > :desde "
            + "order by t.fechaHoraInicio asc")
    List<Turno> findTurnosSolapados(
            @Param("negocioId") Long negocioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("select t from Turno t where t.empleado.empleadoId = :empleadoId "
            + "and t.status not in (com.dev.cutly.turno.enums.TurnoStatus.CANCELLED, com.dev.cutly.turno.enums.TurnoStatus.REJECTED) "
            + "and t.fechaHoraInicio < :hasta and t.fechaHoraFin > :desde")
    List<Turno> findTurnosSolapadosPorEmpleado(
            @Param("empleadoId") Long empleadoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("select t from Turno t where t.ownerResponsable.usuarioId = :ownerId "
            + "and t.status not in (com.dev.cutly.turno.enums.TurnoStatus.CANCELLED, com.dev.cutly.turno.enums.TurnoStatus.REJECTED) "
            + "and t.fechaHoraInicio < :hasta and t.fechaHoraFin > :desde")
    List<Turno> findTurnosSolapadosPorOwner(
            @Param("ownerId") Long ownerId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("select t from Turno t "
            + "where t.cliente.usuarioId = :clienteId "
            + "and t.status not in (com.dev.cutly.turno.enums.TurnoStatus.CANCELLED, com.dev.cutly.turno.enums.TurnoStatus.REJECTED) "
            + "and t.fechaHoraInicio < :hasta "
            + "and t.fechaHoraFin > :desde "
            + "order by t.fechaHoraInicio asc")
    List<Turno> findTurnosClienteSolapados(
            @Param("clienteId") Long clienteId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    Optional<Turno> findByTurnoIdAndNegocio_NegocioId(Long turnoId, Long negocioId);

    Optional<Turno> findByTurnoIdAndCliente_UsuarioId(Long turnoId, Long clienteId);

    Optional<Turno> findByTurnoIdAndEmpleado_EmpleadoId(Long turnoId, Long empleadoId);

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
