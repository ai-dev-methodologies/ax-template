package com.ax.template.authblueprint.eventingest;

import org.springframework.http.HttpStatus;

/** Domain exception for monotonic-event-ingest. status + RFC 9457 type + machine-readable code. */
public class EventIngestException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private EventIngestException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static EventIngestException notFound() {
        return new EventIngestException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "No ingest state for that (stream, subject)");
    }

    /** INGEST-CAPTURE-001 — occurred_at <= captured_at <= recorded_at, fail-closed. */
    public static EventIngestException captureOrderInvalid() {
        return new EventIngestException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:ingest-capture-order-invalid", "INGEST_CAPTURE_ORDER_INVALID",
            "occurred_at must be <= captured_at (within clock-skew tolerance) and captured_at must be <= recorded_at");
    }
}
