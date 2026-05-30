package com.ax.template.authblueprint.dsr;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fail-closed processing gate for DSR-RESTRICT-001 (GDPR Art 18).
 *
 * <p>A processing path consults {@link #checkProcessingAllowed(String)} before any
 * non-storage processing of a subject's data. While a subject is restricted the
 * gate throws {@link DsrException#processingRestricted()} (→ {@code 423 Locked}) —
 * default-deny: the restriction set is the source of truth, so a path that forgets
 * to consult the gate is the bug, not a silent allow.
 *
 * <p>The restriction set is mutated ONLY by {@link DsrService} (register on
 * {@code POST /restrict}, clear on lift). In-memory here for the reference
 * workload; a fork-receiver backs it with the {@link DsrRequest} store or a cache.
 */
@Component
public class DsrRestrictionGate {

    private final Set<String> restricted = ConcurrentHashMap.newKeySet();

    /** Mark the subject restricted (idempotent). Sole caller: {@link DsrService}. */
    void restrict(String subjectId) {
        restricted.add(subjectId);
    }

    /** Lift the restriction (idempotent). Sole caller: {@link DsrService}. */
    void lift(String subjectId) {
        restricted.remove(subjectId);
    }

    public boolean isRestricted(String subjectId) {
        return restricted.contains(subjectId);
    }

    /**
     * Gate a processing attempt. Returns silently when processing is allowed;
     * throws {@link DsrException#processingRestricted()} (423) while the subject
     * is restricted.
     */
    public void checkProcessingAllowed(String subjectId) {
        if (isRestricted(subjectId)) {
            throw DsrException.processingRestricted();
        }
    }
}
