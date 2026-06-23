package com.ax.template.authblueprint.recurringinterval;

import org.springframework.http.HttpStatus;

/** Domain exception for completion-reset-recurring-interval. status + RFC 9457 type + machine code. */
public class RecurringIntervalException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private RecurringIntervalException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static RecurringIntervalException notFound() {
        return new RecurringIntervalException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Recurring obligation not found");
    }

    public static RecurringIntervalException duplicateKey() {
        return new RecurringIntervalException(HttpStatus.CONFLICT,
            "urn:problem:recurring-interval-duplicate-key", "RECURRING_INTERVAL_DUPLICATE_KEY",
            "A recurring obligation with this key already exists");
    }

    /** CRI-RESET-001 — the interval must be a positive number of seconds. */
    public static RecurringIntervalException invalidInterval() {
        return new RecurringIntervalException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:recurring-interval-invalid-interval", "RECURRING_INTERVAL_INVALID_INTERVAL",
            "intervalSeconds must be a positive number of seconds");
    }

    /** CRI-ONCE-001 — each window carries at most one occurrence; the loser of the race is 409. */
    public static RecurringIntervalException windowAlreadyCompleted() {
        return new RecurringIntervalException(HttpStatus.CONFLICT,
            "urn:problem:recurring-interval-window-already-completed",
            "RECURRING_INTERVAL_WINDOW_ALREADY_COMPLETED",
            "The current window already carries its occurrence — exactly one completion per window");
    }

    /** CRI-AUTHZ-001 — who completed must be recorded (defensive; the API derives it from Authentication). */
    public static RecurringIntervalException completerRequired() {
        return new RecurringIntervalException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:recurring-interval-completer-required", "RECURRING_INTERVAL_COMPLETER_REQUIRED",
            "A completion must record a non-blank completer");
    }
}
