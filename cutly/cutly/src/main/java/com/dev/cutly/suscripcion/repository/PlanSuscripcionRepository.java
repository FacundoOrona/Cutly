package com.dev.cutly.suscripcion.repository;

import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PlanSuscripcionRepository extends Repository<PlanSuscripcion, Long> {

    PlanSuscripcion save(PlanSuscripcion planSuscripcion);

    List<PlanSuscripcion> findAll();

    Optional<PlanSuscripcion> findById(Long planId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PlanSuscripcion> findWithLockingByPlanId(Long planId);

    List<PlanSuscripcion> findByNombreNormalizado(String nombreNormalizado);

    boolean existsByNombreNormalizado(String nombreNormalizado);

    @Modifying
    @Query(value = "UPDATE planes_suscripcion SET version_plan = 0 WHERE version_plan IS NULL", nativeQuery = true)
    int inicializarVersionesNulas();
}
