package com.dev.cutly.suscripcion.service;

import com.dev.cutly.auditoria.entity.AuditLog;
import com.dev.cutly.auditoria.repository.AuditoriaRepository;
import com.dev.cutly.suscripcion.entity.HistorialSuscripcion;
import com.dev.cutly.suscripcion.entity.SnapshotSuscripcion;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.TipoEventoSuscripcion;
import com.dev.cutly.suscripcion.repository.HistorialSuscripcionRepository;
import com.dev.cutly.usuario.entity.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class HistorialSuscripcionService {

    private final HistorialSuscripcionRepository historialRepository;
    private final AuditoriaRepository auditoriaRepository;

    public HistorialSuscripcionService(
            HistorialSuscripcionRepository historialRepository,
            AuditoriaRepository auditoriaRepository
    ) {
        this.historialRepository = historialRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public HistorialSuscripcion registrar(
            Suscripcion suscripcion,
            TipoEventoSuscripcion tipoEvento,
            LocalDate fechaInicioPeriodo,
            SnapshotSuscripcion anterior,
            SnapshotSuscripcion nuevo,
            Usuario administrador,
            String identificacionSistema,
            String observacion,
            String descripcionAuditoria,
            String ipAddress
    ) {
        LocalDateTime fechaEvento = RelojSuscripcion.fechaHoraActual();
        HistorialSuscripcion historial = HistorialSuscripcion.registrar(
                suscripcion,
                tipoEvento,
                fechaInicioPeriodo,
                anterior,
                nuevo,
                administrador,
                identificacionSistema,
                observacion,
                fechaEvento
        );
        historialRepository.save(historial);

        AuditLog auditLog = new AuditLog();
        String eventoAuditado = tipoEvento.name().startsWith("SUBSCRIPTION_")
                ? tipoEvento.name().substring("SUBSCRIPTION_".length())
                : tipoEvento.name();
        auditLog.setAccion("SUBSCRIPTION_" + eventoAuditado);
        auditLog.setEntidad("Suscripcion");
        auditLog.setEntidadId(suscripcion.getSuscripcionId());
        String descripcion = descripcionAuditoria == null || descripcionAuditoria.isBlank()
                ? "Evento " + tipoEvento.name() + " registrado en el historial de suscripción"
                : descripcionAuditoria;
        auditLog.setDescripcion(descripcion
                + (observacion == null || observacion.isBlank() ? "" : ". Observación: " + observacion));
        auditLog.setFecha(fechaEvento);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUsuario(administrador);
        auditoriaRepository.save(auditLog);
        return historial;
    }
}
