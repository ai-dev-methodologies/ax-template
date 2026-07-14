package com.ax.template.authblueprint.duplicatesubmission;

/**
 * duplicate-submission-key-l0 lifecycle status. ACTIVE is the only status whose natural key is
 * considered by the DUPKEY-NATURAL-001 uniqueness gate; WITHDRAWN and REJECTED are terminal and
 * RELEASE the key (DUPKEY-WITHDRAWN-003) — there is no edge back out of either.
 */
public enum SubmissionStatus {
    ACTIVE,
    WITHDRAWN,
    REJECTED
}
