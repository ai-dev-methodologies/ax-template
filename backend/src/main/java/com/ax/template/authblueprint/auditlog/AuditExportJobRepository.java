package com.ax.template.authblueprint.auditlog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditExportJobRepository extends JpaRepository<AuditExportJob, UUID> {
}
