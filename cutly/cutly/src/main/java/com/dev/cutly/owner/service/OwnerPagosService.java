package com.dev.cutly.owner.service;

import com.dev.cutly.owner.dto.pago.CrearPagoRequestDto;
import com.dev.cutly.owner.dto.pago.OwnerPagoDto;
import com.dev.cutly.pago.entity.Pago;
import com.dev.cutly.pago.repository.PagoRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.repository.TurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OwnerPagosService {

    private final PagoRepository pagoRepository;
    private final TurnoRepository turnoRepository;
    private final OwnerNegocioService ownerNegocioService;

    public OwnerPagosService(
            PagoRepository pagoRepository,
            TurnoRepository turnoRepository,
            OwnerNegocioService ownerNegocioService
    ) {
        this.pagoRepository = pagoRepository;
        this.turnoRepository = turnoRepository;
        this.ownerNegocioService = ownerNegocioService;
    }

    public List<OwnerPagoDto> listarPagos(Long negocioId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return pagoRepository.findByTurno_Negocio_NegocioIdOrderByFechaPagoDescPagoIdDesc(negocioId)
                .stream().map(this::aDto).toList();
    }

    public OwnerPagoDto obtenerPago(Long negocioId, Long pagoId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Pago pago = pagoRepository.findByPagoIdAndTurno_Negocio_NegocioId(pagoId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el pago solicitado"));
        return aDto(pago);
    }

    public OwnerPagoDto registrarPago(
            Long negocioId,
            CrearPagoRequestDto request,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Turno turno = turnoRepository.findByTurnoIdAndNegocio_NegocioId(request.turnoId(), negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el turno solicitado en este negocio"));
        if (pagoRepository.existsByTurno_TurnoId(turno.getTurnoId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno ya tiene un pago registrado");
        }

        Pago pago = new Pago();
        pago.setTurno(turno);
        pago.setMonto(request.monto());
        pago.setMetodoPago(request.metodoPago());
        pago.setFechaPago(LocalDateTime.now());
        return aDto(pagoRepository.save(pago));
    }

    private OwnerPagoDto aDto(Pago pago) {
        return new OwnerPagoDto(
                pago.getPagoId(),
                pago.getTurno().getNegocio().getNegocioId(),
                pago.getTurno().getTurnoId(),
                pago.getMonto(),
                pago.getMetodoPago(),
                pago.getFechaPago()
        );
    }
}
