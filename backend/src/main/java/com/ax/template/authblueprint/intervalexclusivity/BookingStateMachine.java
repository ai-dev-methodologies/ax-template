package com.ax.template.authblueprint.intervalexclusivity;

import org.springframework.stereotype.Component;

/**
 * interval-exclusivity-l0 sole lifecycle mutator. The ONLY transition is the one-way cancel edge
 * ACTIVE → CANCELLED (IVX-MUTATE-003), invoked by {@link BookingService} under the resource's row
 * lock. CANCELLED has ZERO outgoing edges — this class deliberately defines no un-cancel/reactivate.
 * A cancelled booking's window is freed IMMEDIATELY: {@link BookingRepository}'s overlap queries
 * filter on {@code status = 'ACTIVE'}, so the moment this transition commits, a new booking for the
 * same window is admissible.
 */
@Component
public class BookingStateMachine {

    /** ACTIVE → CANCELLED. Cancelling an already-cancelled booking is a deterministic 409. */
    void cancel(Booking booking) {
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw IntervalExclusivityException.alreadyCancelled();
        }
        booking.markCancelled();
    }
}
