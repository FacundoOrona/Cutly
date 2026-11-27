package com.dev.cutly.liquidacion.repository;

import com.dev.cutly.liquidacion.entity.LiquidacionEmpleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LiquidacionEmpleadoRepository extends JpaRepository<LiquidacionEmpleado, Long> {
    Optional<LiquidacionEmpleado> findByNegocio_NegocioIdAndClaveIdempotencia(Long negocioId, String clave);
    @EntityGraph(attributePaths = {"empleado.usuario", "ownerRegistrante", "ownerAnulador"})
    Optional<LiquidacionEmpleado> findByLiquidacionIdAndNegocio_NegocioId(Long id, Long negocioId);
    @EntityGraph(attributePaths = {"empleado.usuario", "ownerRegistrante", "ownerAnulador"})
    Page<LiquidacionEmpleado> findByNegocio_NegocioIdAndEmpleado_EmpleadoId(Long negocioId, Long empleadoId, Pageable pageable);
    @EntityGraph(attributePaths = {"empleado.usuario", "ownerRegistrante", "ownerAnulador"})
    Page<LiquidacionEmpleado> findByNegocio_NegocioId(Long negocioId, Pageable pageable);
    long countByNegocio_NegocioId(Long negocioId);
}
