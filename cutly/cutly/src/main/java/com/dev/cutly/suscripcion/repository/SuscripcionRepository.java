package com.dev.cutly.suscripcion.repository;

import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

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
