package com.dev.cutly.empleado.repository;

import com.dev.cutly.empleado.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
}
