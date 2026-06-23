package com.ax.template.authblueprint.mandate;

import org.springframework.http.HttpStatus;

/** Domain exception for mandate-fanout. status + RFC 9457 type + machine-readable code. */
public class MandateException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private MandateException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static MandateException notFound() {
        return new MandateException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Mandate or task not found");
    }

    /** MANDATE-FANOUT-001 — the fan-out target N must be positive (no empty directive). */
    public static MandateException emptyFanout() {
        return new MandateException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:mandate-empty-fanout", "MANDATE_EMPTY_FANOUT",
            "A mandate must fan out to a positive number of tasks (taskCount > 0)");
    }

    /** MANDATE-CONCURRENT-001 — a task reaches a terminal state exactly once (the loser's 409). */
    public static MandateException taskAlreadyResolved() {
        return new MandateException(HttpStatus.CONFLICT,
            "urn:problem:mandate-task-already-resolved", "MANDATE_TASK_ALREADY_RESOLVED",
            "That task has already reached a terminal state — each task is resolved exactly once");
    }

    /** MANDATE-BATTERY-001 — the mandate cannot be SATISFIED until every declared check is PASSED. */
    public static MandateException batteryIncomplete() {
        return new MandateException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:mandate-battery-incomplete", "MANDATE_BATTERY_INCOMPLETE",
            "Every declared check must be recorded PASSED before the mandate can be satisfied");
    }

    /** MANDATE-BATTERY-001 — a verdict may only be recorded for a check declared in the battery. */
    public static MandateException unknownCheck() {
        return new MandateException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:mandate-unknown-check", "MANDATE_UNKNOWN_CHECK",
            "That check key is not part of this mandate's declared battery");
    }

    /** MANDATE-BATTERY-001 — a recorded verdict must be PASSED or FAILED, never PENDING. */
    public static MandateException invalidVerdict() {
        return new MandateException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:mandate-invalid-verdict", "MANDATE_INVALID_VERDICT",
            "A recorded check verdict must be PASSED or FAILED");
    }

    /** MANDATE-CONCURRENT/FANOUT-001 — an explicit task response must be DONE or DECLINED. */
    public static MandateException invalidTaskTarget() {
        return new MandateException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:mandate-invalid-task-target", "MANDATE_INVALID_TASK_TARGET",
            "An explicit task response must be DONE or DECLINED");
    }
}
