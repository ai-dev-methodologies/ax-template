package com.ax.template.authblueprint.geoquery;

import org.springframework.http.HttpStatus;

/** Domain exception for geo-bounded-query. status + RFC 9457 type + machine code. */
public class GeoQueryException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private GeoQueryException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** GEO-INPUT-001 — lat/lon out of ISO 6709 range, or radius non-positive / past the policy max. */
    public static GeoQueryException invalidInput(String detail) {
        return new GeoQueryException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:geo-invalid-input", "GEO_INVALID_INPUT", detail);
    }
}
