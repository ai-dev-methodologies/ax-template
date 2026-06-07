package com.ax.template.authblueprint.governedrecord;

import java.util.Set;

/**
 * ACR-VOCAB-001 — pure controlled-vocabulary check (no Spring). When the configured allowlist is
 * non-empty, a reason MUST be one of its members (the vocabulary is governed/closed — no
 * miscellaneous escape); when empty, any non-blank free-text reason is allowed (ACR-ENVELOPE-001
 * still enforces non-blank upstream).
 */
public final class ReasonVocabulary {

    private ReasonVocabulary() {}

    public static boolean isAllowed(String reason, Set<String> allowed) {
        return allowed == null || allowed.isEmpty() || allowed.contains(reason);
    }
}
