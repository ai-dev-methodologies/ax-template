package com.example.emr;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FAIL fixture (the IDW4 adversarial probe): a read method that returns
 * @Phi-tagged data but does NOT record an audit entry on that path → it ships an
 * un-audited PHI read. audit_on_read_guard MUST exit 1.
 */
@Service
public class EncounterService {

    private final EncounterRepository repository;

    public EncounterService(EncounterRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public EncounterView findForPatient(Long id) {
        // No AuditLogService.record on this PHI-read path — the deviation.
        return repository.loadView(id);
    }
}
