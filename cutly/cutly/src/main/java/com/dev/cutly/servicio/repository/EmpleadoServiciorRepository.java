package com.dev.cutly.servicio.repository;

import com.dev.cutly.servicio.entity.EmpleadoServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpleadoServiciorRepository extends JpaRepository<EmpleadoServicio, Long> {

    List<EmpleadoServicio> findByEmpleado_EmpleadoIdOrderByServicio_ServicioIdAsc(Long empleadoId);

    Optional<EmpleadoServicio> findByEmpleado_EmpleadoIdAndServicio_ServicioId(
            Long empleadoId,
            Long servicioId
    );
}
