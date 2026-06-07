package com.ax.template.authblueprint.reservation;

/**
 * reserve-settle-balance-l0 hold lifecycle. A hold is created OUTSTANDING and has exactly ONE
 * terminal transition: SETTLED (committed actual ≤ held, remainder refunded), RELEASED (explicit
 * cancel, whole hold returned), or EXPIRED (timeout sweep reclaimed a stranded hold).
 */
public enum ReservationStatus {
    OUTSTANDING,
    SETTLED,
    RELEASED,
    EXPIRED;

    boolean isTerminal() {
        return this != OUTSTANDING;
    }
}
