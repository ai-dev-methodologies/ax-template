package com.ax.template.authblueprint.obligation;

import org.springframework.http.HttpStatus;

/** Domain exception for deadline-obligation. status + RFC 9457 type + machine-readable code. */
public class ObligationException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private ObligationException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static ObligationException notFound() {
        return new ObligationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Obligation not found");
    }

    public static ObligationException duplicateKey() {
        return new ObligationException(HttpStatus.CONFLICT,
            "urn:problem:obligation-duplicate-key", "OBLIGATION_DUPLICATE_KEY",
            "An obligation with this key already exists");
    }

    /** OBL-GROUND/AXIS-001 — axis definitions must be derivable (positive intervals/limits/rates). */
    public static ObligationException invalidAxis() {
        return new ObligationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:obligation-invalid-axis", "OBLIGATION_INVALID_AXIS",
            "Axis definitions must be derivable: CALENDAR needs intervalDays > 0; USAGE needs limitUnits > 0 and unitsPerDay > 0 (at least one axis)");
    }

    /** OBL-ACK-001 — the loop closes exactly once. */
    public static ObligationException alreadyAcknowledged() {
        return new ObligationException(HttpStatus.CONFLICT,
            "urn:problem:obligation-already-acknowledged", "OBLIGATION_ALREADY_ACKNOWLEDGED",
            "The obligation is already acknowledged — the loop closes once");
    }

    /** OBL-ACK-001 — who closed the loop must be recorded (defensive; the API derives it from Authentication). */
    public static ObligationException acknowledgerRequired() {
        return new ObligationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:obligation-acknowledger-required", "OBLIGATION_ACKNOWLEDGER_REQUIRED",
            "An acknowledgment must record a non-blank acknowledger");
    }
}
