package com.ax.template.authblueprint.reconciliation;

import org.springframework.http.HttpStatus;

/** Domain exception for external-reconciliation. status + RFC 9457 type + machine-readable code. */
public class ReconciliationException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private ReconciliationException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static ReconciliationException notFound() {
        return new ReconciliationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Reconciliation run or item not found");
    }

    /** RECON-DISPOSE-001 — only a BREAK can be disposed. */
    public static ReconciliationException notABreak() {
        return new ReconciliationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:recon-not-a-break", "RECON_NOT_A_BREAK",
            "Only a BREAK item requires disposition — a MATCHED / INTERNAL_ONLY / EXTERNAL_ONLY item cannot be disposed");
    }

    /** RECON-DISPOSE-001 — a disposition must carry a non-blank reason. */
    public static ReconciliationException blankReason() {
        return new ReconciliationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:recon-blank-reason", "RECON_BLANK_REASON",
            "A disposition must record a non-blank reason");
    }

    /** RECON-CONCURRENT-001 — the break was already disposed (the loser of the concurrent dispose). */
    public static ReconciliationException alreadyDisposed() {
        return new ReconciliationException(HttpStatus.CONFLICT,
            "urn:problem:recon-already-disposed", "RECON_ALREADY_DISPOSED",
            "That break was already disposed — a break is disposed exactly once");
    }

    /** RECON-RESOLVE-001 — a run cannot be RESOLVED while any break is undisposed. */
    public static ReconciliationException undisposedBreak() {
        return new ReconciliationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:recon-undisposed-break", "RECON_UNDISPOSED_BREAK",
            "The run has an undisposed break — every break must be disposed before the run can be resolved");
    }
}
