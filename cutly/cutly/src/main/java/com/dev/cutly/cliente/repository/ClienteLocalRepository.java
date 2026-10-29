package com.dev.cutly.cliente.repository;

import com.dev.cutly.cliente.entity.ClienteLocal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteLocalRepository extends JpaRepository<ClienteLocal, Long> {

    java.util.Optional<ClienteLocal> findByClienteLocalIdAndNegocio_NegocioId(
            Long clienteLocalId,
            Long negocioId
    );

    boolean existsByNegocio_NegocioIdAndDniNormalizado(Long negocioId, String dniNormalizado);

    @Query("select c from ClienteLocal c "
            + "where c.negocio.negocioId = :negocioId "
            + "and (lower(c.nombre) like lower(concat('%', :texto, '%')) "
            + "or lower(c.apellido) like lower(concat('%', :texto, '%')))" )
    List<ClienteLocal> buscarPorNombreOApellido(
            @Param("negocioId") Long negocioId,
            @Param("texto") String texto,
            Pageable pageable
    );

    @Query("select c from ClienteLocal c where c.negocio.negocioId = :negocioId")
    Page<ClienteLocal> listarPorNegocio(@Param("negocioId") Long negocioId, Pageable pageable);

    @Query("select c from ClienteLocal c where c.negocio.negocioId = :negocioId "
            + "and (lower(c.nombre) like lower(concat('%', :texto, '%')) "
            + "or lower(c.apellido) like lower(concat('%', :texto, '%')))")
    Page<ClienteLocal> listarPorNegocioYNombreOApellido(
            @Param("negocioId") Long negocioId,
            @Param("texto") String texto,
            Pageable pageable
    );
}
