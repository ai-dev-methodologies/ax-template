package com.ax.template.authblueprint.statemutation;

import org.springframework.http.HttpStatus;

/** Domain exception for state-conditional-mutability. status + RFC 9457 type + machine-readable code. */
public class StateMutationException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private StateMutationException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static StateMutationException notFound() {
        return new StateMutationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Governed form not found");
    }

    /** STATEMUTATION-AUTHORITY/TOCTOU-001 — the field is not mutable in the form's CURRENT state (CWE-367).
     *  The detail NAMES the offending field AND the state that locked it. */
    public static StateMutationException fieldLocked(FormField field, FormState state) {
        return new StateMutationException(HttpStatus.CONFLICT,
            "urn:problem:statemutation-field-locked-in-state", "FIELD_LOCKED_IN_STATE",
            "Field " + field + " is not mutable in state " + state
                + " — only " + StateFieldPolicy.mutableFields(state) + " may be edited now");
    }

    /** STATEMUTATION-MONOTONE-001 — an illegal lifecycle edge (skip, undeclared reverse, advance past LOCKED). */
    public static StateMutationException illegalTransition(FormState from, FormState to) {
        return new StateMutationException(HttpStatus.CONFLICT,
            "urn:problem:statemutation-illegal-transition", "ILLEGAL_FORM_TRANSITION",
            "Illegal form transition " + from + " → " + to);
    }

    /** STATEMUTATION-MONOTONE-001 — a re-open (widening) requires a recorded reason; a blank reason is refused. */
    public static StateMutationException reopenReasonRequired() {
        return new StateMutationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:statemutation-reopen-reason-required", "REOPEN_REASON_REQUIRED",
            "Re-opening a form widens its mutable-set — a non-blank reason MUST be recorded");
    }
}
