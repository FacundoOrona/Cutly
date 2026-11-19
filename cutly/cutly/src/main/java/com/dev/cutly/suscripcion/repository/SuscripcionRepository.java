package com.dev.cutly.suscripcion.repository;

import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long>, JpaSpecificationExecutor<Suscripcion> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Suscripcion> findWithLockingBySuscripcionId(Long suscripcionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Suscripcion> findWithLockingByNegocio_NegocioId(Long negocioId);

    boolean existsByNegocio_NegocioId(Long negocioId);

    Optional<Suscripcion> findByNegocio_NegocioId(Long negocioId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "plan")
    List<Suscripcion> findAllByNegocio_NegocioIdIn(Collection<Long> negocioIds);

    long countByStatus(SuscripcionStatus status);

    long countByStatusIn(Collection<SuscripcionStatus> estados);

    @Query("select count(s) from Suscripcion s where s.status in "
            + "(com.dev.cutly.suscripcion.enums.SuscripcionStatus.ACTIVE, com.dev.cutly.suscripcion.enums.SuscripcionStatus.TRIAL, com.dev.cutly.suscripcion.enums.SuscripcionStatus.EXPIRED) "
            + "and s.fechaVencimiento < :hoy")
    long contarVencidasPorFecha(@Param("hoy") LocalDate hoy);

    @Query("select count(s) from Suscripcion s where s.status in "
            + "(com.dev.cutly.suscripcion.enums.SuscripcionStatus.ACTIVE, com.dev.cutly.suscripcion.enums.SuscripcionStatus.TRIAL) "
            + "and s.fechaVencimiento between :desde and :hasta and s.fechaInicio <= :desde")
    long contarProximasAVencer(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select s.plan.nombre, count(s) from Suscripcion s group by s.plan.nombre order by s.plan.nombre")
    List<Object[]> contarPorPlan();

    List<Suscripcion> findByStatusOrFechaVencimientoBefore(
            SuscripcionStatus status,
            LocalDate fecha
    );

    List<Suscripcion> findByFechaVencimientoBetweenAndStatusIn(
            LocalDate desde,
            LocalDate hasta,
            Collection<SuscripcionStatus> estados
    );
}
