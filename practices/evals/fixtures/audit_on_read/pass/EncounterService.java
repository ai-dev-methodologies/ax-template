package com.example.emr;

import com.ax.template.authblueprint.auditlog.AuditLog;
import com.ax.template.authblueprint.auditlog.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PASS fixture: a read method that returns @Phi-tagged data AND records an
 * audit entry on that path → audit_on_read_guard exits 0.
 */
@Service
public class EncounterService {

    private final EncounterRepository repository;
    private final AuditLogService auditLogService;

    public EncounterService(EncounterRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public EncounterView findForPatient(Long id, String actorId) {
        EncounterView view = repository.loadView(id);
        // HIPAA §164.312(b): record the PHI read BEFORE returning it.
        auditLogService.record(AuditLog.of("PHI_READ", actorId, String.valueOf(id)));
        return view;
    }
}
