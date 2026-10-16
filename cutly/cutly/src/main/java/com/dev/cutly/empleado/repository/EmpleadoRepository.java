package com.dev.cutly.empleado.repository;

import com.dev.cutly.empleado.entity.Empleado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    List<Empleado> findByNegocio_NegocioIdOrderByEmpleadoIdAsc(Long negocioId);

    List<Empleado> findByNegocio_NegocioIdAndActivoTrueOrderByEmpleadoIdAsc(Long negocioId);

    Optional<Empleado> findByEmpleadoIdAndNegocio_NegocioId(Long empleadoId, Long negocioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empleado e where e.empleadoId = :id")
    Optional<Empleado> findWithLockingByEmpleadoId(@Param("id") Long id);

    Optional<Empleado> findByUsuario_UsuarioId(Long usuarioId);

    boolean existsByUsuario_UsuarioId(Long usuarioId);
}
