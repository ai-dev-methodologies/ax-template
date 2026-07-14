package com.ax.template.authblueprint.intervalexclusivity;

import org.springframework.http.HttpStatus;

/** Domain exception for interval-exclusivity-l0. status + RFC 9457 type + machine-readable code. */
public class IntervalExclusivityException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private IntervalExclusivityException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static IntervalExclusivityException resourceNotFound() {
        return new IntervalExclusivityException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "IVX_RESOURCE_NOT_FOUND", "No booking resource found for that key");
    }

    public static IntervalExclusivityException duplicateResource() {
        return new IntervalExclusivityException(HttpStatus.CONFLICT,
            "urn:problem:ivx-duplicate-resource", "IVX_DUPLICATE_RESOURCE", "A resource with that key already exists");
    }

    public static IntervalExclusivityException invalidInterval() {
        return new IntervalExclusivityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:ivx-invalid-interval", "IVX_INVALID_INTERVAL", "start must be strictly before end");
    }

    public static IntervalExclusivityException bookingNotFound() {
        return new IntervalExclusivityException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "IVX_BOOKING_NOT_FOUND", "No booking found for that id");
    }

    public static IntervalExclusivityException overlap() {
        return new IntervalExclusivityException(HttpStatus.CONFLICT,
            "urn:problem:ivx-overlap", "IVX_OVERLAP", "The requested interval overlaps an existing active booking");
    }

    public static IntervalExclusivityException alreadyCancelled() {
        return new IntervalExclusivityException(HttpStatus.CONFLICT,
            "urn:problem:ivx-already-cancelled", "IVX_ALREADY_CANCELLED", "That booking is already cancelled");
    }
}
