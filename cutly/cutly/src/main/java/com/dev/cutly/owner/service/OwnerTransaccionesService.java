package com.dev.cutly.owner.service;

import com.dev.cutly.owner.dto.transaccion.OwnerTransaccionDto;
import com.dev.cutly.owner.dto.transaccion.OwnerTransaccionesPageDto;
import com.dev.cutly.owner.dto.transaccion.OwnerTransaccionesResumenDto;
import com.dev.cutly.pago.entity.Pago;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.pago.enums.TipoTransaccion;
import com.dev.cutly.pago.repository.PagoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OwnerTransaccionesService {
    private final PagoRepository pagoRepository;
    private final OwnerNegocioService ownerNegocioService;

    public OwnerTransaccionesService(PagoRepository pagoRepository, OwnerNegocioService ownerNegocioService) {
        this.pagoRepository = pagoRepository;
        this.ownerNegocioService = ownerNegocioService;
    }

    public OwnerTransaccionesPageDto listar(Long negocioId, LocalDate desde, LocalDate hasta,
            List<MetodoPago> metodosPago, TipoTransaccion tipo, Long clienteId, Long clienteLocalId,
            String q, int page, int size, String sort, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a la fecha hasta");
        }
        if (clienteId != null && clienteLocalId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clienteId y clienteLocalId son mutuamente excluyentes");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación inválida; size debe estar entre 1 y 100");
        }
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay();
        List<MetodoPago> metodos = metodosPago == null ? List.of() : metodosPago.stream().distinct().toList();
        String busqueda = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        PageableSpec orden = ordenar(sort);
        Page<Pago> resultados = pagoRepository.buscarTransacciones(negocioId, inicio, fin, metodos,
                tipo, clienteId, clienteLocalId, busqueda,
                PageRequest.of(page, size, orden.sort().and(Sort.by(Sort.Direction.DESC, "pagoId"))));
        BigDecimal total = pagoRepository.sumarTransaccionesFiltradas(negocioId, inicio, fin,
                metodos, tipo, clienteId, clienteLocalId, busqueda);
        BigDecimal directos = pagoRepository.sumarCobrosDirectosFiltrados(negocioId, inicio, fin,
                metodos, tipo, clienteId, clienteLocalId, busqueda);
        BigDecimal cuentas = total.subtract(directos);
        long cantidad = pagoRepository.contarTransaccionesFiltradas(negocioId, inicio, fin,
                metodos, tipo, clienteId, clienteLocalId, busqueda);
        return new OwnerTransaccionesPageDto(resultados.getContent().stream().map(this::aDto).toList(),
                resultados.getNumber(), resultados.getSize(), cantidad, resultados.getTotalPages(),
                new OwnerTransaccionesResumenDto(total, cantidad, directos, cuentas));
    }

    private PageableSpec ordenar(String sort) {
        if (sort == null || sort.isBlank()) return new PageableSpec(Sort.by(Sort.Direction.DESC, "fechaPago"));
        String[] partes = sort.split(",", -1);
        if (partes.length != 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordenamiento inválido; use campo,direccion");
        String campo = partes[0].trim();
        Sort.Direction direccion;
        try { direccion = Sort.Direction.fromString(partes[1].trim()); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dirección de ordenamiento inválida"); }
        if (!List.of("fechaPago", "monto", "pagoId", "metodoPago").contains(campo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo de ordenamiento no permitido");
        }
        return new PageableSpec(Sort.by(direccion, campo));
    }

    private OwnerTransaccionDto aDto(Pago pago) {
        boolean cuenta = pago.getDeuda() != null;
        var resultado = cuenta ? pago.getDeuda().getResultadoFinanciero() : pago.getResultadoFinanciero();
        var cliente = cuenta ? pago.getDeuda().getCliente() : resultado.getCliente();
        var clienteLocal = cuenta ? pago.getDeuda().getClienteLocal() : resultado.getClienteLocal();
        return new OwnerTransaccionDto(pago.getPagoId(), cuenta ? pago.getDeuda().getNegocio().getNegocioId()
                : pago.getTurno().getNegocio().getNegocioId(), pago.getFechaPago(), pago.getMonto(), pago.getMetodoPago(),
                pago.getDescripcionOtro(), cuenta ? TipoTransaccion.PAGO_CUENTA_CORRIENTE : TipoTransaccion.COBRO_DIRECTO,
                cliente == null ? null : cliente.getUsuarioId(), clienteLocal == null ? null : clienteLocal.getClienteLocalId(),
                cliente == null ? clienteLocal.getNombre() : cliente.getNombre(),
                cliente == null ? clienteLocal.getApellido() : cliente.getApellido(),
                cuenta ? resultado.getTurno().getTurnoId() : pago.getTurno().getTurnoId(),
                cuenta ? pago.getDeuda().getDeudaId() : null);
    }

    private record PageableSpec(Sort sort) {}
}
