package com.ax.template.authblueprint.piecewisedeadband;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for piecewise-deadband. status + RFC 9457 type + machine-readable code.
 */
public class DeadbandException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private DeadbandException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static DeadbandException notFound() {
        return new DeadbandException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Deadband config not found");
    }

    public static DeadbandException duplicateConfig() {
        return new DeadbandException(HttpStatus.CONFLICT,
            "urn:problem:piecewise-deadband-duplicate", "PWDB_DUPLICATE_CONFIG",
            "A deadband config with this config key already exists");
    }

    /** PWDB-SEGMENT-001 — the segments do not tile the domain exactly (gap, overlap, or short span). */
    public static DeadbandException invalidSegments() {
        return new DeadbandException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:piecewise-deadband-invalid-segments", "PWDB_INVALID_SEGMENTS",
            "Segments must tile [domainStart, domainEnd) exactly: no gap, no overlap, full span");
    }

    /** PWDB-EVAL-001 — a domain point outside [domainStart, domainEnd). */
    public static DeadbandException pointOutOfDomain() {
        return new DeadbandException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:piecewise-deadband-point-out-of-domain", "PWDB_POINT_OUT_OF_DOMAIN",
            "The evaluated point must fall within [domainStart, domainEnd)");
    }

    public static DeadbandException invalidValue() {
        return new DeadbandException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:piecewise-deadband-invalid-value", "PWDB_INVALID_VALUE",
            "domainStart must be < domainEnd and every value must be an exact NUMERIC(19,4)");
    }
}
