package com.dev.cutly.suscripcion.repository;

import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Suscripcion> findWithLockingBySuscripcionId(Long suscripcionId);

    boolean existsByNegocio_NegocioId(Long negocioId);

    long countByStatus(SuscripcionStatus status);

    long countByStatusIn(Collection<SuscripcionStatus> estados);

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
