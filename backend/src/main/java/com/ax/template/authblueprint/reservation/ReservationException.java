package com.ax.template.authblueprint.reservation;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for reserve-settle-balance. status + RFC 9457 type + machine-readable code.
 */
public class ReservationException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private ReservationException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static ReservationException notFound() {
        return new ReservationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Balance or reservation not found");
    }

    public static ReservationException duplicateScope() {
        return new ReservationException(HttpStatus.CONFLICT,
            "urn:problem:reservation-duplicate-scope", "RESERVATION_DUPLICATE_SCOPE",
            "A balance with this scope key already exists");
    }

    /** RSV-RESERVE-001 — reserve is the REJECTING dual: a hold beyond available is refused, not clamped. */
    public static ReservationException insufficientFunds() {
        return new ReservationException(HttpStatus.CONFLICT,
            "urn:problem:reservation-insufficient-funds", "RESERVATION_INSUFFICIENT_FUNDS",
            "Requested reservation exceeds the available balance");
    }

    /** RSV-SETTLE-001 — the load-bearing overspend guard: actual must not exceed the held amount. */
    public static ReservationException overSettle() {
        return new ReservationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:reservation-over-settle", "RESERVATION_OVER_SETTLE",
            "Settled amount exceeds the reserved hold");
    }

    /** RSV-RELEASE-001 — a hold has one terminal transition; a second terminal attempt is rejected. */
    public static ReservationException notOutstanding() {
        return new ReservationException(HttpStatus.CONFLICT,
            "urn:problem:reservation-not-outstanding", "RESERVATION_NOT_OUTSTANDING",
            "Reservation is already in a terminal state (settled/released/expired)");
    }

    public static ReservationException invalidAmount() {
        return new ReservationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:reservation-invalid-amount", "RESERVATION_INVALID_AMOUNT",
            "Amount must be a non-negative exact decimal");
    }
}
