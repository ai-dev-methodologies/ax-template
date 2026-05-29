package com.example.emr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FAIL fixture (the IDW4 adversarial probe): a log statement that interpolates
 * the raw @Phi getDiagnosis() getter straight into the message — a permanent PHI
 * leak to the log aggregator. phi_in_logs_guard MUST exit 1.
 */
public class EncounterLogger {

    private static final Logger log = LoggerFactory.getLogger(EncounterLogger.class);

    public void onRead(Encounter encounter) {
        // Raw PHI written to the log — the deviation.
        log.info("read encounter {} diagnosis={}", encounter.getId(), encounter.getDiagnosis());
    }
}
