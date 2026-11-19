package com.dev.cutly.negocio.repository;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NegocioRepository extends JpaRepository <Negocio, Long> {

    long countByStatus(NegocioStatus status);

    List<Negocio> findByOwner_UsuarioId(Long ownerId);

    Optional<Negocio> findByNegocioIdAndOwner_UsuarioId(Long negocioId, Long ownerId);

    Page<Negocio> findByStatusAndNombreContainingIgnoreCase(NegocioStatus status, String nombre, Pageable pageable);

    @Query(value = "select n from Negocio n where n.status = :status "
            + "and lower(n.nombre) like lower(concat('%', :nombre, '%')) "
            + "and exists (select s.suscripcionId from Suscripcion s where s.negocio.negocioId = n.negocioId "
            + "and s.status not in (com.dev.cutly.suscripcion.enums.SuscripcionStatus.SUSPENDED, "
            + "com.dev.cutly.suscripcion.enums.SuscripcionStatus.CANCELLED))",
            countQuery = "select count(n) from Negocio n where n.status = :status "
                    + "and lower(n.nombre) like lower(concat('%', :nombre, '%')) "
                    + "and exists (select s.suscripcionId from Suscripcion s where s.negocio.negocioId = n.negocioId "
                    + "and s.status not in (com.dev.cutly.suscripcion.enums.SuscripcionStatus.SUSPENDED, "
                    + "com.dev.cutly.suscripcion.enums.SuscripcionStatus.CANCELLED))")
    Page<Negocio> findDisponiblesParaClientes(@Param("status") NegocioStatus status,
                                              @Param("nombre") String nombre,
                                              Pageable pageable);

    Optional<Negocio> findByNegocioIdAndStatus(Long negocioId, NegocioStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Negocio> findWithLockingByNegocioId(Long negocioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Negocio> findWithLockingByNegocioIdAndOwner_UsuarioId(Long negocioId, Long ownerId);
}
