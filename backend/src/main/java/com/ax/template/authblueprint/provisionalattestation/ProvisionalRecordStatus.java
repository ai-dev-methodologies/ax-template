package com.ax.template.authblueprint.provisionalattestation;

/**
 * provisional-attestation-l0 — PATT-LIFECYCLE-001. A 2-state lifecycle: PROVISIONAL (created by
 * the author, content still editable) -> ATTESTED (countersigned by a distinct party, content
 * frozen). ATTESTED is terminal — there is no transition back to PROVISIONAL.
 */
public enum ProvisionalRecordStatus {
    PROVISIONAL,
    ATTESTED
}
