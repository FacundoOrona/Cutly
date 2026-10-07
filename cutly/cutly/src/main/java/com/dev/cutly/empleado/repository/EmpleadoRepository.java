package com.dev.cutly.empleado.repository;

import com.dev.cutly.empleado.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    List<Empleado> findByNegocio_NegocioIdOrderByEmpleadoIdAsc(Long negocioId);

    Optional<Empleado> findByEmpleadoIdAndNegocio_NegocioId(Long empleadoId, Long negocioId);

    boolean existsByUsuario_UsuarioId(Long usuarioId);
}
