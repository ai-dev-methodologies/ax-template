package com.ax.template.authblueprint.copresence;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for negative-copresence-gate. status + RFC 9457 type + machine code.
 */
public class CopresenceException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private CopresenceException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static CopresenceException subjectNotFound() {
        return new CopresenceException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Subject not found");
    }

    public static CopresenceException duplicate(String what) {
        return new CopresenceException(HttpStatus.CONFLICT,
            "urn:problem:copresence-duplicate", "COPRESENCE_DUPLICATE", what + " already exists");
    }

    /** GATE-FAILCLOSED-001 — the candidate concept is not assessable by the knowledge base (fail closed). */
    public static CopresenceException unassessable() {
        return new CopresenceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:copresence-unassessable", "COPRESENCE_UNASSESSABLE",
            "Candidate concept is not in the knowledge base; the gate fails closed (no silent allow)");
    }

    /** GATE-GRADED-001 — an ABSOLUTE contraindication; no override path exists. */
    public static CopresenceException absolute(String detail) {
        return new CopresenceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:copresence-absolute", "COPRESENCE_ABSOLUTE",
            "Absolute contraindication with an active member: " + detail);
    }

    /** GATE-GRADED-001 / GATE-OVERRIDE-001 — a RELATIVE contraindication; requires an override reason. */
    public static CopresenceException relative(String detail) {
        return new CopresenceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:copresence-relative", "COPRESENCE_RELATIVE",
            "Relative contraindication (overridable with a reason): " + detail);
    }

    /** Referential integrity — a conflict rule must reference concepts the KB has registered, else a
     *  typo'd rule silently disables a real contraindication (fail-OPEN). */
    public static CopresenceException unknownConcept() {
        return new CopresenceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:copresence-unknown-concept", "COPRESENCE_UNKNOWN_CONCEPT",
            "A conflict rule must reference concepts already registered in the knowledge base");
    }

    public static CopresenceException invalidInput() {
        return new CopresenceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:copresence-invalid", "COPRESENCE_INVALID_INPUT",
            "Concept and label must be non-blank; a conflict pair must be two distinct concepts");
    }
}
