package com.dev.cutly.pago.repository;
import com.dev.cutly.pago.entity.DeudaCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
public interface DeudaClienteRepository extends JpaRepository<DeudaCliente, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeudaCliente> findWithLockingByDeudaIdAndNegocio_NegocioId(Long deudaId, Long negocioId);
    List<DeudaCliente> findByNegocio_NegocioIdOrderByFechaOrigenDesc(Long negocioId);
    Optional<DeudaCliente> findByDeudaIdAndNegocio_NegocioId(Long deudaId, Long negocioId);

    @Query(value = "select new com.dev.cutly.owner.dto.pago.OwnerDeudaListadoDto(d.deudaId, d.negocio.negocioId, d.resultadoFinanciero.turno.turnoId, d.cliente.usuarioId, d.clienteLocal.clienteLocalId, coalesce(d.cliente.nombre, d.clienteLocal.nombre), coalesce(d.cliente.apellido, d.clienteLocal.apellido), d.montoOriginal, coalesce(sum(p.monto), 0), d.montoOriginal - coalesce(sum(p.monto), 0), case when coalesce(sum(p.monto), 0) = 0 then 'PENDIENTE' when coalesce(sum(p.monto), 0) = d.montoOriginal then 'PAGADA' else 'PARCIAL' end, d.fechaOrigen) from DeudaCliente d left join Pago p on p.deuda = d where d.negocio.negocioId = :negocioId and (:texto is null or lower(concat(coalesce(d.cliente.nombre, d.clienteLocal.nombre), ' ', coalesce(d.cliente.apellido, d.clienteLocal.apellido))) like lower(concat('%', :texto, '%'))) group by d.deudaId, d.negocio.negocioId, d.resultadoFinanciero.turno.turnoId, d.cliente.usuarioId, d.clienteLocal.clienteLocalId, d.cliente.nombre, d.clienteLocal.nombre, d.cliente.apellido, d.clienteLocal.apellido, d.montoOriginal, d.fechaOrigen having (:estado is null or (case when coalesce(sum(p.monto), 0) = 0 then 'PENDIENTE' when coalesce(sum(p.monto), 0) = d.montoOriginal then 'PAGADA' else 'PARCIAL' end) = :estado)",
            countQuery = "select count(d) from DeudaCliente d where d.negocio.negocioId = :negocioId and (:texto is null or lower(concat(coalesce(d.cliente.nombre, d.clienteLocal.nombre), ' ', coalesce(d.cliente.apellido, d.clienteLocal.apellido))) like lower(concat('%', :texto, '%'))) and (:estado is null or (:estado = 'PENDIENTE' and (select coalesce(sum(p.monto), 0) from Pago p where p.deuda = d) = 0) or (:estado = 'PARCIAL' and (select coalesce(sum(p.monto), 0) from Pago p where p.deuda = d) > 0 and (select coalesce(sum(p2.monto), 0) from Pago p2 where p2.deuda = d) < d.montoOriginal) or (:estado = 'PAGADA' and (select coalesce(sum(p.monto), 0) from Pago p where p.deuda = d) = d.montoOriginal))")
    Page<com.dev.cutly.owner.dto.pago.OwnerDeudaListadoDto> listarResumenPorNegocio(
            @Param("negocioId") Long negocioId,
            @Param("texto") String texto,
            @Param("estado") String estado,
            Pageable pageable
    );

    @Query("select coalesce(sum(d.montoOriginal - (select coalesce(sum(p.monto), 0) from Pago p where p.deuda = d)), 0) from DeudaCliente d where d.negocio.negocioId = :negocioId and (:desde is null or d.fechaOrigen >= :desde) and (:hasta is null or d.fechaOrigen < :hasta) and d.montoOriginal > (select coalesce(sum(p2.monto), 0) from Pago p2 where p2.deuda = d)")
    BigDecimal sumarSaldoPendiente(@Param("negocioId") Long negocioId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(d) from DeudaCliente d where d.negocio.negocioId = :negocioId and (:desde is null or d.fechaOrigen >= :desde) and (:hasta is null or d.fechaOrigen < :hasta) and (select coalesce(sum(p.monto), 0) from Pago p where p.deuda = d) = 0")
    long contarPendientes(@Param("negocioId") Long negocioId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(d) from DeudaCliente d where d.negocio.negocioId = :negocioId and (:desde is null or d.fechaOrigen >= :desde) and (:hasta is null or d.fechaOrigen < :hasta) and (select coalesce(sum(p.monto), 0) from Pago p where p.deuda = d) > 0 and (select coalesce(sum(p2.monto), 0) from Pago p2 where p2.deuda = d) < d.montoOriginal")
    long contarParciales(@Param("negocioId") Long negocioId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(d) from DeudaCliente d where d.negocio.negocioId = :negocioId and (:desde is null or d.fechaOrigen >= :desde) and (:hasta is null or d.fechaOrigen < :hasta) and (select coalesce(sum(p.monto), 0) from Pago p where p.deuda = d) = d.montoOriginal")
    long contarPagadas(@Param("negocioId") Long negocioId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
