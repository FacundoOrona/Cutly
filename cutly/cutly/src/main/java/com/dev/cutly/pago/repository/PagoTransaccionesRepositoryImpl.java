package com.dev.cutly.pago.repository;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.pago.entity.DeudaCliente;
import com.dev.cutly.pago.entity.Pago;
import com.dev.cutly.pago.entity.ResultadoFinancieroTurno;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.pago.enums.TipoTransaccion;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.usuario.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class PagoTransaccionesRepositoryImpl implements PagoTransaccionesRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Pago> buscarTransacciones(Long negocioId, LocalDateTime desde, LocalDateTime hasta,
            Collection<MetodoPago> metodos, TipoTransaccion tipo, Long clienteId,
            Long clienteLocalId, String q, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Pago> cq = cb.createQuery(Pago.class);
        Root<Pago> pago = cq.from(Pago.class);
        cq.select(pago).where(filtros(cb, pago, negocioId, desde, hasta, metodos, tipo, clienteId, clienteLocalId, q));
        if (pageable.getSort().isSorted()) {
            List<jakarta.persistence.criteria.Order> order = new ArrayList<>();
            pageable.getSort().forEach(o -> order.add(o.isAscending()
                    ? cb.asc(pago.get(o.getProperty())) : cb.desc(pago.get(o.getProperty()))));
            cq.orderBy(order);
        }
        TypedQuery<Pago> query = entityManager.createQuery(cq);
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        long total = contarTransaccionesFiltradas(negocioId, desde, hasta, metodos, tipo, clienteId, clienteLocalId, q);
        return new PageImpl<>(query.getResultList(), pageable, total);
    }

    @Override
    public long contarTransaccionesFiltradas(Long negocioId, LocalDateTime desde, LocalDateTime hasta,
            Collection<MetodoPago> metodos, TipoTransaccion tipo, Long clienteId,
            Long clienteLocalId, String q) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Pago> pago = cq.from(Pago.class);
        cq.select(cb.count(pago)).where(filtros(cb, pago, negocioId, desde, hasta, metodos, tipo, clienteId, clienteLocalId, q));
        return entityManager.createQuery(cq).getSingleResult();
    }

    @Override
    public BigDecimal sumarTransaccionesFiltradas(Long negocioId, LocalDateTime desde, LocalDateTime hasta,
            Collection<MetodoPago> metodos, TipoTransaccion tipo, Long clienteId,
            Long clienteLocalId, String q) {
        return sumar(negocioId, desde, hasta, metodos, tipo, clienteId, clienteLocalId, q, false);
    }

    @Override
    public BigDecimal sumarCobrosDirectosFiltrados(Long negocioId, LocalDateTime desde, LocalDateTime hasta,
            Collection<MetodoPago> metodos, TipoTransaccion tipo, Long clienteId,
            Long clienteLocalId, String q) {
        return sumar(negocioId, desde, hasta, metodos, tipo, clienteId, clienteLocalId, q, true);
    }

    private BigDecimal sumar(Long negocioId, LocalDateTime desde, LocalDateTime hasta,
            Collection<MetodoPago> metodos, TipoTransaccion tipo, Long clienteId,
            Long clienteLocalId, String q, boolean soloDirectos) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> cq = cb.createQuery(BigDecimal.class);
        Root<Pago> pago = cq.from(Pago.class);
        List<Predicate> predicates = new ArrayList<>(List.of(
                filtros(cb, pago, negocioId, desde, hasta, metodos, tipo, clienteId, clienteLocalId, q)));
        if (soloDirectos) predicates.add(cb.isNotNull(pago.get("resultadoFinanciero")));
        cq.select(cb.coalesce(cb.sum(pago.<BigDecimal>get("monto")), BigDecimal.ZERO))
                .where(cb.and(predicates.toArray(Predicate[]::new)));
        return entityManager.createQuery(cq).getSingleResult();
    }

    private Predicate filtros(CriteriaBuilder cb, Root<Pago> pago,
            Long negocioId, LocalDateTime desde, LocalDateTime hasta, Collection<MetodoPago> metodos,
            TipoTransaccion tipo, Long clienteId, Long clienteLocalId, String q) {
        Join<Pago, Turno> turno = pago.join("turno", JoinType.LEFT);
        Join<Pago, DeudaCliente> deuda = pago.join("deuda", JoinType.LEFT);
        Join<Pago, ResultadoFinancieroTurno> resultado = pago.join("resultadoFinanciero", JoinType.LEFT);
        Join<DeudaCliente, Usuario> deudaCliente = deuda.join("cliente", JoinType.LEFT);
        Join<DeudaCliente, ClienteLocal> deudaLocal = deuda.join("clienteLocal", JoinType.LEFT);
        Join<ResultadoFinancieroTurno, Usuario> resultadoCliente = resultado.join("cliente", JoinType.LEFT);
        Join<ResultadoFinancieroTurno, ClienteLocal> resultadoLocal = resultado.join("clienteLocal", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        Predicate directoNegocio = cb.and(cb.equal(turno.get("negocio").get("negocioId"), negocioId),
                cb.isNull(deuda.get("deudaId")), cb.isNotNull(resultado.get("resultadoFinancieroId")));
        Predicate deudaNegocio = cb.and(cb.equal(deuda.get("negocio").get("negocioId"), negocioId),
                cb.isNull(turno.get("turnoId")));
        predicates.add(cb.or(directoNegocio, deudaNegocio));
        if (desde != null) predicates.add(cb.greaterThanOrEqualTo(pago.get("fechaPago"), desde));
        if (hasta != null) predicates.add(cb.lessThan(pago.get("fechaPago"), hasta));
        if (metodos != null && !metodos.isEmpty()) predicates.add(pago.get("metodoPago").in(metodos));
        if (tipo == TipoTransaccion.COBRO_DIRECTO) predicates.add(cb.isNotNull(resultado.get("resultadoFinancieroId")));
        if (tipo == TipoTransaccion.PAGO_CUENTA_CORRIENTE) predicates.add(cb.isNotNull(deuda.get("deudaId")));
        if (clienteId != null) predicates.add(cb.or(
                cb.equal(deudaCliente.get("usuarioId"), clienteId),
                cb.equal(resultadoCliente.get("usuarioId"), clienteId)));
        if (clienteLocalId != null) predicates.add(cb.or(
                cb.equal(deudaLocal.get("clienteLocalId"), clienteLocalId),
                cb.equal(resultadoLocal.get("clienteLocalId"), clienteLocalId)));
        if (q != null && !q.isBlank()) {
            String patron = "%" + q + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(cb.concat(cb.concat(deudaCliente.get("nombre"), " "), deudaCliente.get("apellido"))), patron),
                    cb.like(cb.lower(cb.concat(cb.concat(resultadoCliente.get("nombre"), " "), resultadoCliente.get("apellido"))), patron),
                    cb.like(cb.lower(cb.concat(cb.concat(deudaLocal.get("nombre"), " "), deudaLocal.get("apellido"))), patron),
                    cb.like(cb.lower(cb.concat(cb.concat(resultadoLocal.get("nombre"), " "), resultadoLocal.get("apellido"))), patron)));
        }
        return cb.and(predicates.toArray(Predicate[]::new));
    }
}
