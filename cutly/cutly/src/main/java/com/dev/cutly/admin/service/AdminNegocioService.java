package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.negocio.AdminNegocioDto;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.suscripcion.service.ControlAccesoSuscripcion;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.dev.cutly.suscripcion.service.RelojSuscripcion;
import java.util.NoSuchElementException;

@Service
public class AdminNegocioService {

    private final NegocioRepository negocioRepository;
    private final SuscripcionRepository suscripcionRepository;

    public AdminNegocioService(
            NegocioRepository negocioRepository,
            SuscripcionRepository suscripcionRepository
    ) {
        this.negocioRepository = negocioRepository;
        this.suscripcionRepository = suscripcionRepository;
    }

    public List<AdminNegocioDto> listarNegocios() {
        return aDtos(negocioRepository.findAll());
    }

    public List<AdminNegocioDto> listarNegociosDeOwner(Long ownerId) {
        return aDtos(negocioRepository.findByOwner_UsuarioId(ownerId));
    }

    public AdminNegocioDto obtenerNegocio(Long id) {
        return negocioRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el negocio con id " + id));
    }

    @Transactional
    public AdminNegocioDto actualizarEstado(Long id, NegocioStatus status) {
        Negocio negocio = negocioRepository.findWithLockingByNegocioId(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el negocio con id " + id));
        if (!transicionValida(negocio.getStatus(), status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el estado del negocio de " + negocio.getStatus() + " a " + status);
        }
        if (status == NegocioStatus.ACTIVE) {
            Suscripcion suscripcion = suscripcionRepository.findByNegocio_NegocioId(negocio.getNegocioId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "No se puede habilitar un negocio sin una suscripción asociada"));
            if (!ControlAccesoSuscripcion.suscripcionHabilitada(suscripcion)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede habilitar un negocio sin una suscripción vigente");
            }
        }
        if (negocio.getStatus() == status) {
            return aDto(negocio);
        }
        negocio.setStatus(status);
        return aDto(negocioRepository.save(negocio));
    }

    private boolean transicionValida(NegocioStatus actual, NegocioStatus destino) {
        return switch (actual) {
            case ACTIVE -> destino == NegocioStatus.SUSPENDED || destino == NegocioStatus.INACTIVE;
            case SUSPENDED, INACTIVE -> destino == NegocioStatus.ACTIVE
                    || (actual == NegocioStatus.SUSPENDED && destino == NegocioStatus.INACTIVE);
        };
    }

    private AdminNegocioDto aDto(Negocio negocio) {
        return aDtos(List.of(negocio)).get(0);
    }

    private List<AdminNegocioDto> aDtos(List<Negocio> negocios) {
        if (negocios.isEmpty()) return List.of();
        Map<Long, Suscripcion> suscripciones = suscripcionRepository.findAllByNegocio_NegocioIdIn(
                        negocios.stream().map(Negocio::getNegocioId).toList()).stream()
                .collect(Collectors.toMap(s -> s.getNegocio().getNegocioId(), Function.identity()));
        return negocios.stream().map(negocio -> {
            Suscripcion suscripcion = suscripciones.get(negocio.getNegocioId());
            Long restantes = null;
            Long atraso = null;
            if (suscripcion != null && (suscripcion.getStatus() == com.dev.cutly.suscripcion.enums.SuscripcionStatus.ACTIVE
                    || suscripcion.getStatus() == com.dev.cutly.suscripcion.enums.SuscripcionStatus.TRIAL
                    || suscripcion.getStatus() == com.dev.cutly.suscripcion.enums.SuscripcionStatus.EXPIRED)) {
                long diferencia = java.time.temporal.ChronoUnit.DAYS.between(
                        RelojSuscripcion.fechaActual(), suscripcion.getFechaVencimiento());
                if (diferencia >= 0 && (suscripcion.getStatus() == com.dev.cutly.suscripcion.enums.SuscripcionStatus.ACTIVE
                        || suscripcion.getStatus() == com.dev.cutly.suscripcion.enums.SuscripcionStatus.TRIAL)) {
                    restantes = diferencia;
                } else if (diferencia < 0) {
                    atraso = -diferencia;
                }
            }
            return new AdminNegocioDto(
                    negocio.getNegocioId(), negocio.getNombre(), negocio.getDescripcion(), negocio.getTelefono(),
                    negocio.getEmail(), negocio.getStatus(), negocio.getOwner().getNombre(),
                    negocio.getOwner().getApellido(), negocio.getOwner().getEmail(),
                    suscripcion == null ? null : suscripcion.getSuscripcionId(),
                    suscripcion == null ? null : suscripcion.getStatus(),
                    suscripcion == null ? null : suscripcion.getPlan().getNombre(),
                    suscripcion == null ? null : suscripcion.getPlan().getTipo(),
                    suscripcion == null ? null : suscripcion.getPrecioContratado(),
                    suscripcion == null ? null : suscripcion.getFechaInicio(),
                    suscripcion == null ? null : suscripcion.getFechaVencimiento(),
                    restantes, atraso, suscripcion != null && ControlAccesoSuscripcion.suscripcionHabilitada(suscripcion));
        }).toList();
    }
}
