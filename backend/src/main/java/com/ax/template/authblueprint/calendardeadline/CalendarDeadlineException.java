package com.ax.template.authblueprint.calendardeadline;

import org.springframework.http.HttpStatus;

/** Domain exception for business-day-deadline-arithmetic. status + RFC 9457 type + machine code. */
public class CalendarDeadlineException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private CalendarDeadlineException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static CalendarDeadlineException calendarNotFound() {
        return new CalendarDeadlineException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Holiday calendar not found");
    }

    public static CalendarDeadlineException deadlineNotFound() {
        return new CalendarDeadlineException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Calendar deadline not found");
    }

    /** CALDLINE-BUSINESS-001 — N must be non-negative for the arithmetic to terminate. */
    public static CalendarDeadlineException invalidPeriod() {
        return new CalendarDeadlineException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:caldline-invalid-period", "CALDLINE_INVALID_PERIOD",
            "The period count must be a non-negative whole number of days");
    }
}
