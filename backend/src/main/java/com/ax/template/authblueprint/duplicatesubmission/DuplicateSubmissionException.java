package com.ax.template.authblueprint.duplicatesubmission;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/** Domain exception for duplicate-submission-key. status + RFC 9457 type + machine-readable code. */
public class DuplicateSubmissionException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;
    private final UUID conflictingSubmissionId;

    private DuplicateSubmissionException(HttpStatus status, String type, String code, String message,
                                         UUID conflictingSubmissionId) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
        this.conflictingSubmissionId = conflictingSubmissionId;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }
    public UUID conflictingSubmissionId() { return conflictingSubmissionId; }

    public static DuplicateSubmissionException notFound() {
        return new DuplicateSubmissionException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Duplicate-submission resource not found", null);
    }

    /** DUPKEY-NATURAL-001 — an exact natural-key match against an existing ACTIVE submission. */
    public static DuplicateSubmissionException duplicateSubmission(UUID existingId) {
        return new DuplicateSubmissionException(HttpStatus.CONFLICT,
            "urn:problem:duplicate-submission", "DUPLICATE_SUBMISSION",
            "A submission with the same natural same-loss key is already active", existingId);
    }

    /** DUPKEY-WITHDRAWN-003 — an illegal state transition (e.g. double-withdraw). */
    public static DuplicateSubmissionException illegalTransition(SubmissionStatus from, SubmissionStatus to) {
        return new DuplicateSubmissionException(HttpStatus.CONFLICT,
            "urn:problem:duplicate-submission-illegal-transition", "SUBMISSION_ILLEGAL_TRANSITION",
            "Illegal submission transition: " + from + " -> " + to, null);
    }

    /** A blank/invalid channel config submitted as a domain 422 (not a Bean-Validation 400). */
    public static DuplicateSubmissionException invalidChannel(String detail) {
        return new DuplicateSubmissionException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:duplicate-submission-invalid-channel", "DUPLICATE_KEY_CHANNEL_INVALID", detail, null);
    }
}
