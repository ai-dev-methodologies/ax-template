package com.ax.template.authblueprint.decisiongov;

import org.springframework.http.HttpStatus;

/** Domain exception for decision-governance. status + RFC 9457 type + machine-readable code. */
public class DecisionException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private DecisionException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static DecisionException notFound() {
        return new DecisionException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Decision scope not found");
    }

    public static DecisionException duplicateScope() {
        return new DecisionException(HttpStatus.CONFLICT,
            "urn:problem:decision-duplicate-scope", "DECISION_DUPLICATE_SCOPE",
            "A decision scope with this key already exists — re-determine via recompute or override");
    }

    /** DG-BASIS-001 — the determination must carry an appraisal-sufficient basis snapshot. */
    public static DecisionException basisRequired() {
        return new DecisionException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:decision-basis-required", "DECISION_BASIS_REQUIRED",
            "A computed decision must carry a non-blank basis snapshot (inputs/assumptions/method version)");
    }

    /** DG-RECOMPUTE-001 / DG-OVERRIDE-001 — re-determinations carry a mandatory reason. */
    public static DecisionException reasonRequired() {
        return new DecisionException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:decision-reason-required", "DECISION_REASON_REQUIRED",
            "A recompute or override requires a non-blank reason");
    }

    /** DG-OVERRIDE-001 — four-eyes: the approver is required and must differ from the requester. */
    public static DecisionException fourEyesRequired() {
        return new DecisionException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:decision-four-eyes-required", "DECISION_FOUR_EYES_REQUIRED",
            "An override requires an approver distinct from the requesting actor (separation of duty)");
    }
}
