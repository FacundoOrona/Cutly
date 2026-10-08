package com.dev.cutly.servicio.repository;

import com.dev.cutly.servicio.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findByNegocio_NegocioIdOrderByServicioIdAsc(Long negocioId);

    Optional<Servicio> findByServicioIdAndNegocio_NegocioId(Long servicioId, Long negocioId);

}
