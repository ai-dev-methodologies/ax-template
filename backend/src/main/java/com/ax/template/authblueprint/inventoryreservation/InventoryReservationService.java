package com.ax.template.authblueprint.inventoryreservation;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * two-axis-inventory-reservation-l0 sole orchestrator. AVAILABLE is a DERIVED projection
 * (onHand − reserved), never a stored column (INVRES-RESERVE-001). The two-phase hold:
 * (1) {@link #reserve} takes the item's PESSIMISTIC_WRITE row lock, refuses with 422 when
 * available < q (nothing mutated), else increments reserved and appends a HELD {@link Reservation}
 * leaving onHand untouched (INVRES-CONCURRENT-001 — the lock serializes the read-available /
 * write-reserved sequence so concurrent reserves cannot over-reserve, CWE-362); (2) {@link #commit}
 * moves the hold HELD → COMMITTED and decrements BOTH onHand and reserved (the goods leave);
 * (3) {@link #release} moves HELD → RELEASED and decrements reserved alone (the hold frees). The
 * commit/release are exactly-once via {@link ReservationStateMachine} (a non-HELD reservation → 409).
 * The @Check reserved >= 0 AND reserved <= on_hand backstops conservation (reserved == Σ HELD
 * quantities). Reservation rows are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class InventoryReservationService {

    private final InventoryItemRepository items;
    private final MemberWriter members;
    private final ReservationStateMachine stateMachine;
    private final InventoryReservationMetrics metrics;
    private final Clock clock;

    public InventoryReservationService(InventoryItemRepository items, MemberWriter members,
                                       ReservationStateMachine stateMachine,
                                       InventoryReservationMetrics metrics, Clock clock) {
        this.items = items;
        this.members = members;
        this.stateMachine = stateMachine;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public InventoryItem createItem(String sku, long onHand) {
        InventoryItem item = new InventoryItem(UUID.randomUUID(), sku, onHand, Instant.now(clock));
        InventoryItem saved = items.save(item);
        metrics.record("create", "ok");
        return saved;
    }

    /** INVRES-RESERVE/CONCURRENT-001 — reserve q against DERIVED available under the item row lock.
     *  The PESSIMISTIC_WRITE lock serializes the read-available / write-reserved sequence so that
     *  across N concurrent reserves against {@code available = k·q} exactly k pass the {@code
     *  available >= q} gate (the rest see the already-reduced headroom → 422); reserved can never
     *  exceed onHand (CWE-362). onHand is UNTOUCHED — the hold only shrinks available. */
    @Transactional
    public InventoryReservation reserve(UUID itemId, long quantity, String actor) {
        InventoryItem item = items.findByIdForUpdate(itemId).orElseThrow(InventoryReservationException::itemNotFound);
        if (item.available() < quantity) {
            metrics.record("reserve", "insufficient");
            throw InventoryReservationException.insufficientAvailable();     // 422 — nothing mutated
        }
        item.reserve(quantity);                                             // reserved += q; onHand untouched
        InventoryReservation r = new InventoryReservation(UUID.randomUUID(), item.getId(), quantity,
            ReservationStatus.HELD, actor, Instant.now(clock));
        members.persistAndFlush(r);                                          // immutable HELD row appended
        metrics.record("reserve", "reserved");
        return r;
    }

    /** INVRES-COMMIT-001 — commit a HELD hold: HELD → COMMITTED + onHand −= q AND reserved −= q
     *  (the goods leave). Exactly-once: a non-HELD reservation → 409. The item is row-locked so
     *  the axis decrement is serialized against any concurrent reserve. */
    @Transactional
    public InventoryReservation commit(UUID reservationId) {
        InventoryReservation r = items.findReservation(reservationId).orElseThrow(InventoryReservationException::reservationNotFound);
        InventoryItem item = items.findByIdForUpdate(r.getItemId()).orElseThrow(InventoryReservationException::itemNotFound);
        stateMachine.commit(r);                                             // HELD → COMMITTED (409 if not HELD)
        item.commitReservation(r.getQuantity());                            // onHand −= q AND reserved −= q
        metrics.record("commit", "committed");
        return r;
    }

    /** INVRES-RELEASE-001 — release a HELD hold: HELD → RELEASED + reserved −= q (onHand untouched,
     *  the hold frees so available grows back). Exactly-once: a non-HELD reservation → 409. */
    @Transactional
    public InventoryReservation release(UUID reservationId) {
        InventoryReservation r = items.findReservation(reservationId).orElseThrow(InventoryReservationException::reservationNotFound);
        InventoryItem item = items.findByIdForUpdate(r.getItemId()).orElseThrow(InventoryReservationException::itemNotFound);
        stateMachine.release(r);                                            // HELD → RELEASED (409 if not HELD)
        item.releaseReservation(r.getQuantity());                          // reserved −= q only
        metrics.record("release", "released");
        return r;
    }

    @Transactional(readOnly = true)
    public InventoryItem getItem(UUID itemId) {
        return items.findById(itemId).orElseThrow(InventoryReservationException::itemNotFound);
    }

    @Transactional(readOnly = true)
    public InventoryReservation getReservation(UUID reservationId) {
        return items.findReservation(reservationId).orElseThrow(InventoryReservationException::reservationNotFound);
    }

    /** INVRES-CONSERVE-001 — the live conservation check: reserved == Σ(HELD reservation quantities). */
    @Transactional(readOnly = true)
    public long sumHeldQuantity(UUID itemId) {
        getItem(itemId);                                                    // 404 before a zero sum
        return items.sumHeldQuantity(itemId);
    }

    @Transactional(readOnly = true)
    public List<InventoryReservation> reservations(UUID itemId, int page, int size) {
        getItem(itemId);                                                    // 404 before an empty list
        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 50 : Math.min(size, 200));
        return items.findReservations(itemId, pageable);
    }
}
