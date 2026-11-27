package com.dev.cutly.servicio.repository;

import com.dev.cutly.servicio.entity.EmpleadoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmpleadoServiciorRepository extends JpaRepository<EmpleadoServicio, Long> {

    List<EmpleadoServicio> findByEmpleado_EmpleadoIdOrderByServicio_ServicioIdAsc(Long empleadoId);

    Optional<EmpleadoServicio> findByEmpleado_EmpleadoIdAndServicio_ServicioId(
            Long empleadoId,
            Long servicioId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select es from EmpleadoServicio es where es.empleado.empleadoId = :empleadoId and es.servicio.servicioId = :servicioId")
    Optional<EmpleadoServicio> findWithLockingByEmpleadoAndServicio(
            @Param("empleadoId") Long empleadoId, @Param("servicioId") Long servicioId);
}
