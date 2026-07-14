package com.ax.template.authblueprint.geofence;

import org.springframework.http.HttpStatus;

/** Domain exception for geofence-transition. status + RFC 9457 type + machine code. */
public class GeofenceException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private GeofenceException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static GeofenceException notFound() {
        return new GeofenceException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Geofence tracker not found");
    }

    /** A registration request for an already-tracked (subjectId, zoneId) pair, or a malformed observation. */
    public static GeofenceException invalidInput(String detail) {
        return new GeofenceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:geofence-invalid-input", "GEOFENCE_INVALID_INPUT", detail);
    }
}
