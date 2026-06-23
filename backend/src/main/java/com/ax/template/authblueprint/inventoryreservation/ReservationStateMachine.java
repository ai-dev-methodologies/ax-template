package com.ax.template.authblueprint.inventoryreservation;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link Reservation#getStatus()} (HG-STATE-SOLE-MUTATOR). A hold moves
 * HELD → (COMMITTED | RELEASED) EXACTLY ONCE (INVRES-COMMIT/RELEASE-001): both terminals have
 * NO outgoing edge, so a second commit/release — or a commit of a released hold (or vice versa)
 * — is an illegal edge that throws {@link InventoryReservationException#reservationNotHeld()}
 * → a deterministic 409. The state machine NEVER touches the item's quantity axes; the service
 * decrements onHand/reserved through {@link InventoryItem}'s sole-mutator hooks alongside the
 * status move, inside the item's PESSIMISTIC_WRITE lock.
 */
@Component
public class ReservationStateMachine {

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ReservationStatus.class);
        ALLOWED.put(ReservationStatus.HELD,
            EnumSet.of(ReservationStatus.COMMITTED, ReservationStatus.RELEASED));
        ALLOWED.put(ReservationStatus.COMMITTED, EnumSet.noneOf(ReservationStatus.class));
        ALLOWED.put(ReservationStatus.RELEASED, EnumSet.noneOf(ReservationStatus.class));
    }

    /** HELD → COMMITTED — the goods leave (caller decrements BOTH item axes). 409 if not HELD. */
    public void commit(InventoryReservation reservation) {
        assertTransition(reservation.getStatus(), ReservationStatus.COMMITTED);
        reservation.setStatus(ReservationStatus.COMMITTED);
    }

    /** HELD → RELEASED — the hold frees (caller decrements item reserved only). 409 if not HELD. */
    public void release(InventoryReservation reservation) {
        assertTransition(reservation.getStatus(), ReservationStatus.RELEASED);
        reservation.setStatus(ReservationStatus.RELEASED);
    }

    private static void assertTransition(ReservationStatus from, ReservationStatus to) {
        Set<ReservationStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(ReservationStatus.class));
        if (!allowed.contains(to)) {
            // a non-HELD reservation has no legal edge — exactly-once 409
            throw InventoryReservationException.reservationNotHeld();
        }
    }
}
