package com.ax.template.authblueprint.provisionalattestation;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.ax.template.authblueprint.provisionalattestation.ProvisionalRecordStatus.ATTESTED;
import static com.ax.template.authblueprint.provisionalattestation.ProvisionalRecordStatus.PROVISIONAL;

/**
 * PATT-LIFECYCLE-001 — sole mutator of {@link ProvisionalRecord#getStatus()}. The 2-state ALLOWED
 * graph: PROVISIONAL -> ATTESTED only; ATTESTED is terminal (no transition out, including no
 * reverse to PROVISIONAL). Any other attempted transition throws (409).
 */
@Component
public class ProvisionalRecordStateMachine {

    private static final Map<ProvisionalRecordStatus, Set<ProvisionalRecordStatus>> ALLOWED =
        new EnumMap<>(ProvisionalRecordStatus.class);

    static {
        ALLOWED.put(PROVISIONAL, EnumSet.of(ATTESTED));
        ALLOWED.put(ATTESTED, EnumSet.noneOf(ProvisionalRecordStatus.class));
    }

    /** PATT-DISTINCT-002 is checked by the CALLER before invoking this — this method only
     *  enforces the 2-state transition graph and performs the mutation atomically. */
    public void attest(ProvisionalRecord record, String attestedBy, String contentHash, Instant now) {
        Set<ProvisionalRecordStatus> targets = ALLOWED.getOrDefault(record.getStatus(),
            EnumSet.noneOf(ProvisionalRecordStatus.class));
        if (!targets.contains(ATTESTED)) {
            throw ProvisionalAttestationException.illegalTransition();
        }
        record.markAttested(attestedBy, contentHash, now);
    }
}
