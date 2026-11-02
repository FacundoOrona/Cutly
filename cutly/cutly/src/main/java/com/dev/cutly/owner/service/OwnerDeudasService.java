package com.dev.cutly.owner.service;

import com.dev.cutly.owner.dto.pago.*;
import com.dev.cutly.pago.entity.*;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.pago.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service @Transactional
public class OwnerDeudasService {
    private final DeudaClienteRepository deudaRepository;
    private final PagoRepository pagoRepository;
    private final OwnerNegocioService ownerNegocioService;
    private final IdempotenciaAbonoRepository idempotenciaAbonoRepository;
    public OwnerDeudasService(DeudaClienteRepository deudaRepository, PagoRepository pagoRepository, OwnerNegocioService ownerNegocioService, IdempotenciaAbonoRepository idempotenciaAbonoRepository) {
        this.deudaRepository = deudaRepository; this.pagoRepository = pagoRepository; this.ownerNegocioService = ownerNegocioService; this.idempotenciaAbonoRepository = idempotenciaAbonoRepository;
    }
    public Page<OwnerDeudaListadoDto> listar(Long negocioId, String texto, String estado, int page, int size, String email) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, email);
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación inválida; size debe estar entre 1 y 100");
        if (estado != null && !List.of("PENDIENTE", "PARCIAL", "PAGADA").contains(estado)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de deuda inválido");
        String busqueda = texto == null || texto.isBlank() ? null : texto.trim();
        return deudaRepository.listarResumenPorNegocio(negocioId, busqueda, estado,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaOrigen").and(Sort.by(Sort.Direction.DESC, "deudaId"))));
    }
    public Page<OwnerCuentaCorrienteClienteDto> listarPorCliente(Long negocioId, String texto, int page, int size, String email) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, email);
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación inválida; size debe estar entre 1 y 100");
        String busqueda = texto == null || texto.isBlank() ? null : texto.trim();
        return deudaRepository.listarCuentasCorrientesPorCliente(negocioId, busqueda,
                PageRequest.of(page, size));
    }
    public OwnerDeudaDto obtener(Long negocioId, Long deudaId, String email) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, email);
        return dto(deudaRepository.findByDeudaIdAndNegocio_NegocioId(deudaId, negocioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la deuda solicitada")));
    }
    public OwnerDeudaDto abonar(Long negocioId, Long deudaId, CrearAbonoRequestDto req, String key, String email) {
        if (key == null || key.isBlank() || key.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key es obligatorio y admite hasta 100 caracteres");
        var negocio = ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, email);
        DeudaCliente deuda = deudaRepository.findWithLockingByDeudaIdAndNegocio_NegocioId(deudaId, negocioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la deuda solicitada"));
        if (deuda.getResultadoFinanciero().getTipoResultado() != DecisionFinanciera.CUENTA_CORRIENTE) throw new ResponseStatusException(HttpStatus.CONFLICT, "La deuda no corresponde a cuenta corriente");
        String hash = hashSolicitud(req);
        var previa = idempotenciaAbonoRepository.findByKey(key);
        if (previa.isPresent()) {
            if (!previa.get().getDeuda().getDeudaId().equals(deudaId) || !previa.get().getDeuda().getNegocio().getNegocioId().equals(negocioId) || !previa.get().getRequestHash().equals(hash)) throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya se usó con otra operación");
            return dto(deuda);
        }
        BigDecimal abonado = pagoRepository.sumarAbonos(deudaId);
        BigDecimal saldo = deuda.getMontoOriginal().subtract(abonado);
        if (req.monto() == null || req.monto().signum() <= 0 || req.monto().compareTo(saldo) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "El abono supera el saldo pendiente");
        Pago pago = new Pago(); pago.setDeuda(deuda); pago.setMonto(req.monto()); pago.setMetodoPago(req.metodoPago()); pago.setDescripcionOtro(req.descripcionOtro()); pago.setFechaPago(LocalDateTime.now()); pago.setOwnerRegistrante(negocio.getOwner()); pagoRepository.save(pago);
        IdempotenciaAbono idem = new IdempotenciaAbono(); idem.setKey(key); idem.setRequestHash(hash); idem.setDeuda(deuda); idem.setPago(pago);
        try { idempotenciaAbonoRepository.saveAndFlush(idem); }
        catch (DataIntegrityViolationException conflict) { throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya se usó en otra solicitud"); }
        return dto(deuda);
    }
    private OwnerDeudaDto dto(DeudaCliente d) {
        BigDecimal abonado = pagoRepository.sumarAbonos(d.getDeudaId());
        var movimientos = pagoRepository.findByDeuda_DeudaIdOrderByFechaPagoAscPagoIdAsc(d.getDeudaId()).stream().map(p -> new OwnerPagoDto(p.getPagoId(), d.getNegocio().getNegocioId(), null, d.getDeudaId(), p.getMonto(), p.getMetodoPago(), p.getFechaPago(), p.getDescripcionOtro())).toList();
        BigDecimal saldo = d.getMontoOriginal().subtract(abonado);
        String estado = abonado.signum() == 0 ? "PENDIENTE" : saldo.signum() == 0 ? "PAGADA" : "PARCIAL";
        return new OwnerDeudaDto(d.getDeudaId(), d.getNegocio().getNegocioId(), d.getResultadoFinanciero().getTurno().getTurnoId(), d.getCliente() == null ? null : d.getCliente().getUsuarioId(), d.getClienteLocal() == null ? null : d.getClienteLocal().getClienteLocalId(), d.getCliente() == null ? d.getClienteLocal().getNombre() : d.getCliente().getNombre(), d.getCliente() == null ? d.getClienteLocal().getApellido() : d.getCliente().getApellido(), d.getMontoOriginal(), abonado, saldo, estado, d.getFechaOrigen(), movimientos, d.getMontoOriginal());
    }
    private String hashSolicitud(CrearAbonoRequestDto r) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((r.monto().stripTrailingZeros().toPlainString()+"|"+r.metodoPago()+"|"+(r.descripcionOtro()==null?"":r.descripcionOtro())).getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
