package com.dev.cutly.remuneracion.repository;

import com.dev.cutly.remuneracion.entity.RemuneracionEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RemuneracionEmpleadoRepository extends JpaRepository<RemuneracionEmpleado, Long> {
    Optional<RemuneracionEmpleado> findByTurno_TurnoId(Long turnoId);
}
