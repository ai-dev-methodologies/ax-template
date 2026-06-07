package com.ax.template.authblueprint.dispatch;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for dispatch. Carries the HTTP status, the RFC 9457 problem {@code type} URI,
 * and a machine-readable {@code code} member. EXCL-409-004 requires the two race-loser codes to be
 * DISTINCT from each other (JOB_ALREADY_TAKEN vs DRIVER_ALREADY_BUSY) and from CAPACITY_EXHAUSTED
 * / 412 — a client branches on the code, so each factory pins its own.
 */
public class DispatchException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private DispatchException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static DispatchException notFound() {
        return new DispatchException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Resource not found");
    }

    public static DispatchException invalidTransition(String from, String to) {
        return new DispatchException(HttpStatus.CONFLICT,
            "urn:problem:dispatch-invalid-transition", "DISPATCH_INVALID_TRANSITION",
            "Illegal transition " + from + " -> " + to);
    }

    /** OFFER-TOCTOU-003 — accept arrived at/after the deadline (offer no longer acceptable). */
    public static DispatchException offerExpired() {
        return new DispatchException(HttpStatus.CONFLICT,
            "urn:problem:offer-expired", "OFFER_EXPIRED",
            "Offer is no longer acceptable (deadline passed)");
    }

    /** EXCL-409-004 — the request was already assigned to (or no longer available for) another provider. */
    public static DispatchException jobAlreadyTaken() {
        return new DispatchException(HttpStatus.CONFLICT,
            "urn:problem:job-already-taken", "JOB_ALREADY_TAKEN",
            "This request is no longer available for assignment");
    }

    /** EXCL-409-004 — the provider was already assigned to another request. */
    public static DispatchException driverAlreadyBusy() {
        return new DispatchException(HttpStatus.CONFLICT,
            "urn:problem:driver-already-busy", "DRIVER_ALREADY_BUSY",
            "This provider is already assigned to another request");
    }

    /** AVAIL-FRESH-002 — provider is OFFLINE/ASSIGNED or its heartbeat is stale. */
    public static DispatchException providerNotEligible() {
        return new DispatchException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:provider-not-eligible", "PROVIDER_NOT_ELIGIBLE",
            "Provider is not currently offerable (not AVAILABLE or heartbeat stale)");
    }

    /** OFFER-FSM-001 — a request may have at most one PENDING offer. */
    public static DispatchException duplicatePendingOffer() {
        return new DispatchException(HttpStatus.CONFLICT,
            "urn:problem:duplicate-pending-offer", "DUPLICATE_PENDING_OFFER",
            "This request already has an outstanding PENDING offer");
    }
}
