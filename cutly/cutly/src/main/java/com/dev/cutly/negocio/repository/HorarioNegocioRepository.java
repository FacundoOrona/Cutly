package com.dev.cutly.negocio.repository;

import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HorarioNegocioRepository extends JpaRepository<HorarioNegocio,Long> {

    List<HorarioNegocio> findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(Long negocioId);

    Optional<HorarioNegocio> findByHorarioIdAndNegocio_NegocioId(Long horarioId, Long negocioId);

    boolean existsByNegocio_NegocioIdAndDiaSemana(Long negocioId, DiaSemana diaSemana);

    boolean existsByNegocio_NegocioIdAndDiaSemanaAndHorarioIdNot(
            Long negocioId,
            DiaSemana diaSemana,
            Long horarioId
    );
}
