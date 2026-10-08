package com.dev.cutly.turno.repository;

import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, Long>, JpaSpecificationExecutor<Turno> {

    long countByStatus(TurnoStatus status);

    List<Turno> findByNegocio_NegocioIdOrderByFechaHoraInicioAscTurnoIdAsc(Long negocioId);

    Optional<Turno> findByTurnoIdAndNegocio_NegocioId(Long turnoId, Long negocioId);
}
