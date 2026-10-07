package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.suscripcion.AdminSuscripcionDto;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class AdminSuscripcionService {

    // GESTION MANUAL DE DIAS QUE DURA LA SUSCRIPCION

    private static final int DIAS_PROXIMOS_A_VENCER = 30;
    private static final Set<SuscripcionStatus> ESTADOS_ACTIVOS = Set.of(
            SuscripcionStatus.ACTIVE,
            SuscripcionStatus.TRIAL
    );

    private final SuscripcionRepository suscripcionRepository;

    public AdminSuscripcionService(SuscripcionRepository suscripcionRepository) {
        this.suscripcionRepository = suscripcionRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminSuscripcionDto> listarSuscripciones() {
        return suscripcionRepository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminSuscripcionDto obtenerSuscripcion(Long id) {
        return suscripcionRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la suscripción con id " + id));
    }

    @Transactional(readOnly = true)
    public List<AdminSuscripcionDto> listarVencidas() {
        return suscripcionRepository.findByStatusOrFechaVencimientoBefore(
                        SuscripcionStatus.EXPIRED, LocalDate.now()
                ).stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminSuscripcionDto> listarProximasAVencer() {
        LocalDate hoy = LocalDate.now();
        return suscripcionRepository.findByFechaVencimientoBetweenAndStatusIn(
                        hoy, hoy.plusDays(DIAS_PROXIMOS_A_VENCER), ESTADOS_ACTIVOS
                ).stream().map(this::aDto).toList();
    }

    @Transactional
    public AdminSuscripcionDto actualizarEstado(Long id, SuscripcionStatus status) {
        Suscripcion suscripcion = suscripcionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la suscripción con id " + id));
        if (!transicionValida(suscripcion.getStatus(), status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el estado de la suscripción de " + suscripcion.getStatus() + " a " + status);
        }
        if (suscripcion.getStatus() == status) {
            return aDto(suscripcion);
        }
        suscripcion.setStatus(status);
        return aDto(suscripcionRepository.save(suscripcion));
    }

    private boolean transicionValida(SuscripcionStatus actual, SuscripcionStatus destino) {
        return switch (actual) {
            case TRIAL -> destino == SuscripcionStatus.ACTIVE || destino == SuscripcionStatus.SUSPENDED;
            case ACTIVE -> destino == SuscripcionStatus.SUSPENDED;
            case SUSPENDED -> destino == SuscripcionStatus.ACTIVE;
            case EXPIRED, CANCELLED -> false;
        };
    }

    private AdminSuscripcionDto aDto(Suscripcion suscripcion) {
        return new AdminSuscripcionDto(
                suscripcion.getSuscripcionId(),
                suscripcion.getStatus(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaVencimiento(),
                suscripcion.getPlan().getPlanId(),
                suscripcion.getPlan().getNombre(),
                suscripcion.getNegocio().getNegocioId(),
                suscripcion.getNegocio().getNombre()
        );
    }
}
