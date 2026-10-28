package com.dev.cutly.negocio.repository;

import com.dev.cutly.negocio.entity.HorarioNegocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HorarioNegocioRepository extends JpaRepository<HorarioNegocio,Long> {

    List<HorarioNegocio> findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(Long negocioId);

    Optional<HorarioNegocio> findByHorarioIdAndNegocio_NegocioId(Long horarioId, Long negocioId);

}
