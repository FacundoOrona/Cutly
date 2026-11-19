package com.dev.cutly.negocio.repository;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface NegocioRepository extends JpaRepository <Negocio, Long> {

    long countByStatus(NegocioStatus status);

    @Query("select count(n) from Negocio n where exists (select s.suscripcionId from Suscripcion s "
            + "where s.negocio.negocioId = n.negocioId and s.status in "
            + "(com.dev.cutly.suscripcion.enums.SuscripcionStatus.ACTIVE, com.dev.cutly.suscripcion.enums.SuscripcionStatus.TRIAL) "
            + "and s.fechaInicio <= :hoy and s.fechaVencimiento >= :hoy)")
    long contarConSuscripcionHabilitada(@Param("hoy") java.time.LocalDate hoy);

    @EntityGraph(attributePaths = "owner")
    List<Negocio> findByOwner_UsuarioId(Long ownerId);

    @Override
    @EntityGraph(attributePaths = "owner")
    List<Negocio> findAll();

    Optional<Negocio> findByNegocioIdAndOwner_UsuarioId(Long negocioId, Long ownerId);

    Page<Negocio> findByStatusAndNombreContainingIgnoreCase(NegocioStatus status, String nombre, Pageable pageable);

    @Query(value = "select n from Negocio n where n.status = :status "
            + "and lower(n.nombre) like lower(concat('%', :nombre, '%')) "
            + "and exists (select s.suscripcionId from Suscripcion s where s.negocio.negocioId = n.negocioId "
            + "and (s.status = com.dev.cutly.suscripcion.enums.SuscripcionStatus.ACTIVE "
            + "or s.status = com.dev.cutly.suscripcion.enums.SuscripcionStatus.EXPIRED "
            + "or (s.status = com.dev.cutly.suscripcion.enums.SuscripcionStatus.TRIAL "
            + "and s.fechaVencimiento >= :hoy)))",
            countQuery = "select count(n) from Negocio n where n.status = :status "
                    + "and lower(n.nombre) like lower(concat('%', :nombre, '%')) "
                    + "and exists (select s.suscripcionId from Suscripcion s where s.negocio.negocioId = n.negocioId "
                    + "and (s.status = com.dev.cutly.suscripcion.enums.SuscripcionStatus.ACTIVE "
                    + "or s.status = com.dev.cutly.suscripcion.enums.SuscripcionStatus.EXPIRED "
                    + "or (s.status = com.dev.cutly.suscripcion.enums.SuscripcionStatus.TRIAL "
                    + "and s.fechaVencimiento >= :hoy)))")
    Page<Negocio> findDisponiblesParaClientes(@Param("status") NegocioStatus status,
                                              @Param("nombre") String nombre,
                                              @Param("hoy") LocalDate hoy,
                                              Pageable pageable);

    Optional<Negocio> findByNegocioIdAndStatus(Long negocioId, NegocioStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Negocio> findWithLockingByNegocioId(Long negocioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Negocio> findWithLockingByNegocioIdAndOwner_UsuarioId(Long negocioId, Long ownerId);
}
