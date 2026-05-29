package com.example.share;

import java.util.List;

/**
 * Minimal stub of common/ConsentGate so the PASS fixture's gate call resolves
 * visually. The guard does pure text scanning; this stub documents the shape.
 */
public final class ConsentGate {

    private ConsentGate() {
    }

    public static void requireConsent(String subjectId, String purpose, List<ConsentRecord> ledger) {
        // real impl lives in common/ConsentGate
    }
}
