package com.dev.cutly.auditoria.repository;

import com.dev.cutly.auditoria.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<AuditLog,Long> {
}
