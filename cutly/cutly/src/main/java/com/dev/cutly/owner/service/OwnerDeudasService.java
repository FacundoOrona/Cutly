package com.dev.cutly.owner.service;

import com.dev.cutly.owner.dto.pago.*;
import com.dev.cutly.pago.entity.*;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.pago.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service @Transactional
public class OwnerDeudasService {
    private final DeudaClienteRepository deudaRepository;
    private final PagoRepository pagoRepository;
    private final OwnerNegocioService ownerNegocioService;
    public OwnerDeudasService(DeudaClienteRepository deudaRepository, PagoRepository pagoRepository, OwnerNegocioService ownerNegocioService) {
        this.deudaRepository = deudaRepository; this.pagoRepository = pagoRepository; this.ownerNegocioService = ownerNegocioService;
    }
    public List<OwnerDeudaDto> listar(Long negocioId, String email) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, email);
        return deudaRepository.findByNegocio_NegocioIdOrderByFechaOrigenDesc(negocioId).stream().map(this::dto).toList();
    }
    public OwnerDeudaDto obtener(Long negocioId, Long deudaId, String email) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, email);
        return dto(deudaRepository.findByDeudaIdAndNegocio_NegocioId(deudaId, negocioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la deuda solicitada")));
    }
    public OwnerDeudaDto abonar(Long negocioId, Long deudaId, CrearAbonoRequestDto req, String email) {
        var negocio = ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, email);
        DeudaCliente deuda = deudaRepository.findWithLockingByDeudaIdAndNegocio_NegocioId(deudaId, negocioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la deuda solicitada"));
        if (deuda.getResultadoFinanciero().getTipoResultado() != DecisionFinanciera.CUENTA_CORRIENTE) throw new ResponseStatusException(HttpStatus.CONFLICT, "La deuda no corresponde a cuenta corriente");
        if (req.metodoPago() != com.dev.cutly.pago.enums.MetodoPago.OTRO && req.descripcionOtro() != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "descripcionOtro solo se admite con OTRO");
        BigDecimal abonado = pagoRepository.sumarAbonos(deudaId);
        BigDecimal saldo = deuda.getMontoOriginal().subtract(abonado);
        if (req.monto() == null || req.monto().signum() <= 0 || req.monto().compareTo(saldo) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "El abono supera el saldo pendiente");
        Pago pago = new Pago(); pago.setDeuda(deuda); pago.setMonto(req.monto()); pago.setMetodoPago(req.metodoPago()); pago.setDescripcionOtro(req.descripcionOtro()); pago.setFechaPago(LocalDateTime.now()); pago.setOwnerRegistrante(negocio.getOwner()); pagoRepository.save(pago);
        return dto(deuda);
    }
    private OwnerDeudaDto dto(DeudaCliente d) {
        BigDecimal abonado = pagoRepository.sumarAbonos(d.getDeudaId());
        var movimientos = pagoRepository.findByDeuda_DeudaIdOrderByFechaPagoAscPagoIdAsc(d.getDeudaId()).stream().map(p -> new OwnerPagoDto(p.getPagoId(), d.getNegocio().getNegocioId(), null, d.getDeudaId(), p.getMonto(), p.getMetodoPago(), p.getFechaPago())).toList();
        BigDecimal saldo = d.getMontoOriginal().subtract(abonado);
        String estado = abonado.signum() == 0 ? "PENDIENTE" : saldo.signum() == 0 ? "PAGADA" : "PARCIAL";
        return new OwnerDeudaDto(d.getDeudaId(), d.getNegocio().getNegocioId(), d.getResultadoFinanciero().getTurno().getTurnoId(), d.getCliente() == null ? null : d.getCliente().getUsuarioId(), d.getClienteLocal() == null ? null : d.getClienteLocal().getClienteLocalId(), d.getMontoOriginal(), abonado, saldo, estado, d.getFechaOrigen(), movimientos);
    }
}
