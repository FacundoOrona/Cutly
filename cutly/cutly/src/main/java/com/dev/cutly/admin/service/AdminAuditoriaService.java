package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.auditoria.AdminAuditoriaDto;
import com.dev.cutly.auditoria.entity.AuditLog;
import com.dev.cutly.auditoria.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AdminAuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AdminAuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminAuditoriaDto> listarAuditorias() {
        return auditoriaRepository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminAuditoriaDto obtenerAuditoria(Long id) {
        return auditoriaRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la auditoría con id " + id));
    }

    private AdminAuditoriaDto aDto(AuditLog auditoria) {
        return new AdminAuditoriaDto(
                auditoria.getAuditLogId(),
                auditoria.getAccion(),
                auditoria.getEntidad(),
                auditoria.getEntidadId(),
                auditoria.getDescripcion(),
                auditoria.getFecha(),
                auditoria.getIpAddress(),
                auditoria.getUsuario() == null ? null : auditoria.getUsuario().getUsuarioId(),
                auditoria.getUsuario() == null ? null : auditoria.getUsuario().getEmail()
        );
    }
}
