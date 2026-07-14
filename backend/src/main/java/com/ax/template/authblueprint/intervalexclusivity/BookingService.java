package com.ax.template.authblueprint.intervalexclusivity;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * interval-exclusivity-l0 sole orchestrator for {@link Booking}. Every create/resize/cancel FIRST
 * acquires the resource's {@code PESSIMISTIC_WRITE} row lock (IVX-CONCURRENT-002), so the
 * overlap-check-then-write sequence for one resource can never interleave across concurrent
 * requests. Shrinking an interval (the new window is a subset of the current one) is allowed
 * unconditionally; extending re-validates overlap against every OTHER active booking
 * (IVX-MUTATE-003). Cancelling transitions through {@link BookingStateMachine}; a cancelled booking
 * is immediately excluded from every subsequent overlap check (the repository's overlap queries
 * filter {@code status = ACTIVE}).
 */
@Service
public class BookingService {

    static final int MAX_BOOKINGS = 500;

    private final BookingResourceRepository resources;
    private final MemberWriter members;
    private final BookingStateMachine stateMachine;
    private final Clock clock;

    public BookingService(BookingResourceRepository resources, MemberWriter members,
                          BookingStateMachine stateMachine, Clock clock) {
        this.resources = resources;
        this.members = members;
        this.stateMachine = stateMachine;
        this.clock = clock;
    }

    /** IVX-OVERLAP-001 / IVX-CONCURRENT-002 — lock the resource, THEN check overlap, THEN write. */
    @Transactional
    public Booking book(String resourceKey, Instant start, Instant end) {
        resources.findByResourceKeyForUpdate(resourceKey).orElseThrow(IntervalExclusivityException::resourceNotFound);
        validateInterval(start, end);
        if (resources.countOverlappingAny(resourceKey, start, end) > 0) {
            throw IntervalExclusivityException.overlap();
        }
        return members.persistAndFlush(new Booking(UUID.randomUUID(), resourceKey, start, end, Instant.now(clock)));
    }

    /** IVX-MUTATE-003 — shrink is unconditional; extend re-validates overlap under the same lock. */
    @Transactional
    public Booking resize(UUID bookingId, Instant newStart, Instant newEnd) {
        Booking booking = members.find(Booking.class, bookingId)
            .orElseThrow(IntervalExclusivityException::bookingNotFound);
        resources.findByResourceKeyForUpdate(booking.getResourceKey())
            .orElseThrow(IntervalExclusivityException::resourceNotFound);
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw IntervalExclusivityException.alreadyCancelled();
        }
        validateInterval(newStart, newEnd);

        boolean isShrink = !newStart.isBefore(booking.getStartAt()) && !newEnd.isAfter(booking.getEndAt());
        if (!isShrink && resources.countOverlappingExcluding(booking.getResourceKey(), bookingId, newStart, newEnd) > 0) {
            throw IntervalExclusivityException.overlap();
        }
        booking.resize(newStart, newEnd);
        return booking;
    }

    /** IVX-MUTATE-003 — cancelling frees the window immediately for new bookings. */
    @Transactional
    public Booking cancel(UUID bookingId) {
        Booking booking = members.find(Booking.class, bookingId)
            .orElseThrow(IntervalExclusivityException::bookingNotFound);
        resources.findByResourceKeyForUpdate(booking.getResourceKey())
            .orElseThrow(IntervalExclusivityException::resourceNotFound);
        stateMachine.cancel(booking);
        return booking;
    }

    @Transactional(readOnly = true)
    public Booking get(UUID bookingId) {
        return members.find(Booking.class, bookingId).orElseThrow(IntervalExclusivityException::bookingNotFound);
    }

    @Transactional(readOnly = true)
    public List<Booking> list(String resourceKey) {
        if (!resources.existsByResourceKey(resourceKey)) {
            throw IntervalExclusivityException.resourceNotFound();
        }
        return resources.findBookings(resourceKey, PageRequest.of(0, MAX_BOOKINGS));
    }

    private void validateInterval(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw IntervalExclusivityException.invalidInterval();
        }
    }
}
