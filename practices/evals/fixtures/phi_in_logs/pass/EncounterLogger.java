package com.example.emr;

import com.ax.template.authblueprint.common.AuditPiiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PASS fixture: logs a NON-PHI field (getId) and a hashed correlation token,
 * never the raw @Phi getDiagnosis() getter. The hash is computed BEFORE the log
 * call. phi_in_logs_guard exits 0.
 */
public class EncounterLogger {

    private static final Logger log = LoggerFactory.getLogger(EncounterLogger.class);

    public void onRead(Encounter encounter) {
        String diagnosisHash = AuditPiiHelper.piiHash(encounter.getDiagnosis());
        // Non-PHI id is fine; PHI surfaces only as a non-recoverable hash token.
        log.info("verb=PHI_READ encounterId={} diagnosisHash={}",
                encounter.getId(), diagnosisHash);
    }
}
