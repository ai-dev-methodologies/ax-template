package com.ax.template.authblueprint.costshare;

import com.ax.template.authblueprint.common.WaterfallAllocator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * accumulator-consume-l0 + ordered-waterfall-l0 sole orchestrator. Every mutation acquires the
 * accumulator row under PESSIMISTIC_WRITE (ACC-ATOMIC-001 form b); consume NEVER rejects an
 * over-limit valid draw (returns a residual); allocate threads one eligible amount through the
 * ordered tier cascade in ONE transaction (WF-ATOMIC-001), locking accumulators in deterministic
 * scope-key order (WF-LOCK-001) and conserving exactly (WF-CONSERVE-001).
 */
@Service
public class CostShareService {

    static final int MONEY_SCALE = 2;

    public record ConsumeResult(BigDecimal applied, BigDecimal residual) {}

    public record AllocationResult(BigDecimal eligible, BigDecimal memberPaid, BigDecimal insurerPaid,
                                   BigDecimal deductibleApplied, BigDecimal oopApplied) {}

    private final AccumulatorRepository repo;
    private final CostShareMetrics metrics;
    private final Clock clock;

    public CostShareService(AccumulatorRepository repo, CostShareMetrics metrics, Clock clock) {
        this.repo = repo;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public Accumulator create(String scopeKey, BigDecimal limit, BigDecimal initialUsed) {
        requireNonNegative(limit);
        requireNonNegative(initialUsed);
        if (initialUsed.compareTo(limit) > 0) {
            metrics.record("create", "rejected");
            throw CostShareException.invalidAmount();       // initial usage cannot start above the limit
        }
        if (repo.existsByScopeKey(scopeKey)) {
            metrics.record("create", "rejected");
            throw CostShareException.duplicateScope();
        }
        Accumulator a = new Accumulator(UUID.randomUUID(), scopeKey,
            limit.setScale(MONEY_SCALE), initialUsed.setScale(MONEY_SCALE), Instant.now(clock));
        return repo.save(a);
    }

    /** ACC-ATOMIC-001 / ACC-CONSERVE-001 — non-rejecting partial draw under a row lock. */
    @Transactional
    public ConsumeResult consume(String scopeKey, BigDecimal delta) {
        requireNonNegative(delta);
        Accumulator a = repo.findByScopeKeyForUpdate(scopeKey).orElseThrow(CostShareException::notFound);
        BigDecimal applied = delta.min(a.headroom()).max(zero());     // min(delta, headroom), never rejects
        BigDecimal residual = delta.subtract(applied);                 // exact difference -> applied+residual==delta
        a.advanceUsed(applied);
        metrics.record("consume", residual.signum() > 0 ? "partial" : "ok");
        return new ConsumeResult(applied.setScale(MONEY_SCALE), residual.setScale(MONEY_SCALE));
    }

    /** ACC-CLAWBACK-001 — decrement used (non-monotone); reject a draw below zero. */
    @Transactional
    public Accumulator release(String scopeKey, BigDecimal amount) {
        requireNonNegative(amount);
        Accumulator a = repo.findByScopeKeyForUpdate(scopeKey).orElseThrow(CostShareException::notFound);
        if (a.getUsed().subtract(amount).signum() < 0) {
            metrics.record("release", "over_release");
            throw CostShareException.overRelease();                    // CHECK(used>=0) backstops in prod
        }
        a.decrementUsed(amount);
        metrics.record("release", "ok");
        return a;
    }

    /**
     * ACC-RESET-001 — period-boundary reset (idempotent). The caller decides WHEN the boundary has
     * passed (the period clock it owns); this reset zeroes usage under the row lock and is a no-op if
     * already zero, so re-running it within a period cannot double-reset.
     */
    @Transactional
    public Accumulator reset(String scopeKey) {
        Accumulator a = repo.findByScopeKeyForUpdate(scopeKey).orElseThrow(CostShareException::notFound);
        if (a.getUsed().signum() != 0) {
            a.resetUsed();
        }
        metrics.record("reset", "ok");
        return a;
    }

    /**
     * WF-CONSERVE-001 / WF-ATOMIC-001 / WF-CLAMP-001 / WF-LOCK-001 — allocate one eligible amount
     * across the member-liability cascade (deductible -> coinsurance -> OOP-max clamp). Locks the
     * deductible + OOP accumulators in deterministic scope-key order, computes the split via the pure
     * {@link WaterfallAllocator}, advances both accumulators, and returns the conserving breakdown.
     */
    @Transactional
    public AllocationResult allocate(BigDecimal eligible, String deductibleKey, String oopMaxKey,
                                     BigDecimal coinsuranceRate) {
        requireNonNegative(eligible);
        requireNonNegative(coinsuranceRate);
        if (deductibleKey.equals(oopMaxKey)) {
            metrics.record("allocate", "rejected");
            throw CostShareException.sameScope();          // distinct accumulators or the advance double-counts
        }
        // WF-LOCK-001 — acquire both accumulator locks in deterministic (sorted) scope-key order, and
        // KEEP the locked rows (same transaction) instead of re-reading them.
        Map<String, Accumulator> locked = new HashMap<>();
        List<String> ordered = new ArrayList<>(List.of(deductibleKey, oopMaxKey));
        ordered.sort(String::compareTo);
        for (String k : ordered) {
            locked.put(k, repo.findByScopeKeyForUpdate(k).orElseThrow(CostShareException::notFound));
        }
        Accumulator deductible = locked.get(deductibleKey);
        Accumulator oop = locked.get(oopMaxKey);

        List<WaterfallAllocator.Tier> tiers = List.of(
            new WaterfallAllocator.Tier(deductibleKey, WaterfallAllocator.TierKind.ABSORB_TO_CAP, deductible.headroom()),
            new WaterfallAllocator.Tier(null, WaterfallAllocator.TierKind.COINSURANCE, coinsuranceRate),
            new WaterfallAllocator.Tier(oopMaxKey, WaterfallAllocator.TierKind.CLAMP_TOTAL, oop.headroom())
        );
        WaterfallAllocator.Result r = WaterfallAllocator.allocate(eligible.setScale(MONEY_SCALE), tiers, MONEY_SCALE);

        BigDecimal dedApplied = zero();
        BigDecimal oopApplied = zero();
        for (WaterfallAllocator.Absorb ab : r.advances()) {   // advances reflect the POST-clamp split
            Accumulator a = locked.get(ab.accumulatorKey());
            if (a != null) a.advanceUsed(ab.amount());
            if (ab.accumulatorKey().equals(deductibleKey)) dedApplied = ab.amount();
            else if (ab.accumulatorKey().equals(oopMaxKey)) oopApplied = ab.amount();
        }
        metrics.record("allocate", "ok");
        return new AllocationResult(eligible.setScale(MONEY_SCALE), r.memberPaid().setScale(MONEY_SCALE),
            r.counterpartyPaid().setScale(MONEY_SCALE), dedApplied.setScale(MONEY_SCALE), oopApplied.setScale(MONEY_SCALE));
    }

    @Transactional(readOnly = true)
    public Accumulator get(String scopeKey) {
        return repo.findByScopeKey(scopeKey).orElseThrow(CostShareException::notFound);
    }

    private static void requireNonNegative(BigDecimal v) {
        if (v == null || v.signum() < 0) throw CostShareException.invalidAmount();
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE);
    }
}
