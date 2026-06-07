package com.ax.template.authblueprint.reservation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * reserve-settle-balance-l0 sole orchestrator. A pooled balance is drawn in TWO phases: an
 * over-reserve-safe {@link #reserve} places a hold (under the balance's PESSIMISTIC_WRITE lock),
 * and a {@link #settle} commits the actual (≤ the hold) AND returns the unused remainder in ONE
 * transaction; {@link #release} returns a whole hold; {@link #expireOne} (timeout sweep) reclaims a
 * stranded hold and LOSES the race to a live settle. committed/reserved mutate only here (no public
 * setter). Lock order is ALWAYS balance-then-hold to avoid a lock-order deadlock.
 */
@Service
public class ReservationService {

    static final int MONEY_SCALE = 4;

    private final ReservableBalanceRepository balances;
    private final ReservationRepository holds;
    private final ReservationMetrics metrics;
    private final Clock clock;

    public ReservationService(ReservableBalanceRepository balances, ReservationRepository holds,
                              ReservationMetrics metrics, Clock clock) {
        this.balances = balances;
        this.holds = holds;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public ReservableBalance createBalance(String scopeKey, BigDecimal funded) {
        requireNonNegative(funded);
        if (balances.existsByScopeKey(scopeKey)) {
            metrics.record("create", "rejected");
            throw ReservationException.duplicateScope();
        }
        ReservableBalance b = new ReservableBalance(UUID.randomUUID(), scopeKey,
            funded.setScale(MONEY_SCALE), zero(), zero(), Instant.now(clock));
        try {
            ReservableBalance saved = balances.saveAndFlush(b);   // race → deterministic 409, not 500
            metrics.record("create", "ok");
            return saved;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw ReservationException.duplicateScope();
        }
    }

    /** RSV-RESERVE-001 — over-reserve-safe atomic hold under the balance row lock (rejecting dual). */
    @Transactional
    public Reservation reserve(String scopeKey, BigDecimal amount, long ttlSeconds) {
        requirePositive(amount);
        ReservableBalance b = balances.findByScopeKeyForUpdate(scopeKey).orElseThrow(ReservationException::notFound);
        BigDecimal amt = amount.setScale(MONEY_SCALE);
        if (amt.compareTo(b.available()) > 0) {              // reject — never clamp (unlike accumulator-consume)
            metrics.record("reserve", "insufficient");
            throw ReservationException.insufficientFunds();
        }
        b.increaseReserved(amt);
        Instant now = Instant.now(clock);
        Reservation h = holds.save(new Reservation(UUID.randomUUID(), b.getId(), amt,
            ReservationStatus.OUTSTANDING, null, now.plusSeconds(ttlSeconds), now));
        metrics.record("reserve", "ok");
        return h;
    }

    /** RSV-SETTLE-001 — commit actual (≤ hold) AND return the remainder, in one tx, conserving. */
    @Transactional
    public Reservation settle(UUID holdId, BigDecimal actual) {
        requireNonNegative(actual);
        ReservableBalance b = lockBalanceOf(holdId);             // balance FIRST (deterministic order)
        Reservation h = holds.findByIdForUpdate(holdId).orElseThrow(ReservationException::notFound);
        if (h.getStatus().isTerminal()) {                       // a live settle/release/sweep won the race
            metrics.record("settle", "already_terminal");
            throw ReservationException.notOutstanding();
        }
        BigDecimal act = actual.setScale(MONEY_SCALE);
        if (act.compareTo(h.getAmount()) > 0) {                 // the load-bearing overspend guard
            metrics.record("settle", "over_settle");
            throw ReservationException.overSettle();
        }
        b.advanceCommitted(act);                                // committed += actual
        b.decreaseReserved(h.getAmount());                      // reserved -= WHOLE hold → available += amount-actual
        h.settle(act);
        metrics.record("settle", "ok");
        return h;
    }

    /** RSV-RELEASE-001 — return the whole hold; one terminal transition. */
    @Transactional
    public Reservation release(UUID holdId) {
        ReservableBalance b = lockBalanceOf(holdId);
        Reservation h = holds.findByIdForUpdate(holdId).orElseThrow(ReservationException::notFound);
        if (h.getStatus().isTerminal()) {
            metrics.record("release", "already_terminal");
            throw ReservationException.notOutstanding();
        }
        b.decreaseReserved(h.getAmount());                      // committed unchanged; whole hold returned
        h.release();
        metrics.record("release", "ok");
        return h;
    }

    /**
     * RSV-SWEEP-001 — per-row REQUIRES_NEW reclaim of one expired hold (called CROSS-BEAN from the
     * sweeper so REQUIRES_NEW is honored). Re-reads under the balance lock and SKIPS a hold that is no
     * longer OUTSTANDING (a live settle/release won the race) — so a swept-then-settled hold never
     * double-returns value.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOne(UUID holdId) {
        UUID balanceId = holds.findBalanceId(holdId).orElse(null);
        if (balanceId == null) {
            return;                                            // hold vanished — nothing to reclaim
        }
        ReservableBalance b = balances.findByIdForUpdate(balanceId).orElseThrow(ReservationException::notFound);
        Reservation h = holds.findByIdForUpdate(holdId).orElseThrow(ReservationException::notFound);
        if (h.getStatus().isTerminal()) {
            return;                                            // live settle/release won the race — skip
        }
        if (h.getExpiresAt().isAfter(Instant.now(clock))) {
            return;                                            // not actually due (extended) — defensive
        }
        b.decreaseReserved(h.getAmount());
        h.expire();
        metrics.record("sweep", "expired");
    }

    @Transactional(readOnly = true)
    public List<UUID> dueHoldIds(int batch) {
        return holds.findDueIds(ReservationStatus.OUTSTANDING, Instant.now(clock), PageRequest.of(0, batch));
    }

    @Transactional(readOnly = true)
    public ReservableBalance getBalance(String scopeKey) {
        return balances.findByScopeKey(scopeKey).orElseThrow(ReservationException::notFound);
    }

    @Transactional(readOnly = true)
    public Reservation getHold(UUID holdId) {
        return holds.findById(holdId).orElseThrow(ReservationException::notFound);
    }

    @Transactional(readOnly = true)
    public Page<Reservation> listHolds(String scopeKey, int page, int size) {
        ReservableBalance b = balances.findByScopeKey(scopeKey).orElseThrow(ReservationException::notFound);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return holds.findByBalanceIdOrderByCreatedAtAsc(b.getId(), PageRequest.of(safePage, safeSize));
    }

    /** Discover the parent balance via a scalar projection (no stale L1 entity), then lock it. */
    private ReservableBalance lockBalanceOf(UUID holdId) {
        UUID balanceId = holds.findBalanceId(holdId).orElseThrow(ReservationException::notFound);
        return balances.findByIdForUpdate(balanceId).orElseThrow(ReservationException::notFound);
    }

    private static void requireNonNegative(BigDecimal v) {
        if (v == null || v.signum() < 0) throw ReservationException.invalidAmount();
    }

    private static void requirePositive(BigDecimal v) {
        if (v == null || v.signum() <= 0) throw ReservationException.invalidAmount();
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE);
    }
}
