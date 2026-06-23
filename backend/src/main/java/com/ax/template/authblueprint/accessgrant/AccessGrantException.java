package com.ax.template.authblueprint.accessgrant;

import org.springframework.http.HttpStatus;

/** Domain exception for time-bounded-access-grant. status + RFC 9457 type + machine-readable code. */
public class AccessGrantException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private AccessGrantException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static AccessGrantException notFound() {
        return new AccessGrantException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Access grant not found");
    }

    /** AGRANT-WINDOW-001 — the check is before validFrom; the window has not opened (fail closed). */
    public static AccessGrantException notYetValid() {
        return new AccessGrantException(HttpStatus.FORBIDDEN,
            "urn:problem:agrant-not-yet-valid", "GRANT_NOT_YET_VALID",
            "The access grant's validity window has not opened yet");
    }

    /** AGRANT-WINDOW/BOUNDARY-001 — now is at or after validUntil; the window has elapsed (fail closed). */
    public static AccessGrantException expired() {
        return new AccessGrantException(HttpStatus.FORBIDDEN,
            "urn:problem:agrant-expired", "GRANT_EXPIRED",
            "The access grant has expired — now is at or after its validUntil");
    }

    /** AGRANT-REVOKE-001 — the grant was revoked; it fails closed regardless of the window. */
    public static AccessGrantException revoked() {
        return new AccessGrantException(HttpStatus.FORBIDDEN,
            "urn:problem:agrant-revoked", "GRANT_REVOKED",
            "The access grant has been revoked");
    }

    /** AGRANT-ELIGIBILITY-001 — a required credential class is missing or expired (names the class). */
    public static AccessGrantException credentialIneligible(String credentialClass) {
        return new AccessGrantException(HttpStatus.FORBIDDEN,
            "urn:problem:agrant-credential-ineligible", "CREDENTIAL_INELIGIBLE",
            "Ineligible — required credential class missing or expired: " + credentialClass);
    }
}
