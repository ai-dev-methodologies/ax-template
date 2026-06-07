package com.ax.template.authblueprint.announcement;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for announcement-l0. Carries the RFC 9457 status + a stable problem
 * {@code type} urn; {@link AnnouncementController}'s single {@code @ExceptionHandler} maps it
 * to a {@code ProblemDetail} without a per-code switch. Mirrors {@code DsrException}.
 */
public class AnnouncementException extends RuntimeException {

    private final HttpStatus status;
    private final String type;

    private AnnouncementException(HttpStatus status, String type, String detail) {
        super(detail);
        this.status = status;
        this.type = type;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }

    /** ANN-LIFECYCLE-001 — illegal lifecycle edge. */
    public static AnnouncementException invalidTransition(AnnouncementState from, AnnouncementState to) {
        return new AnnouncementException(HttpStatus.CONFLICT,
            "urn:problem:announcement-invalid-transition",
            "Illegal announcement transition " + from + " -> " + to);
    }

    /** ANN-VALIDATION-001 — non-positive window. */
    public static AnnouncementException invalidWindow() {
        return new AnnouncementException(HttpStatus.BAD_REQUEST,
            "urn:problem:announcement-invalid-window",
            "startsAt must be strictly before endsAt");
    }

    /** ANN-AUTHZ-001 — unknown id (IDOR-safe 404, never existence-leak). */
    public static AnnouncementException notFound() {
        return new AnnouncementException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found",
            "Announcement not found");
    }
}
