package com.ax.template.authblueprint.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * ordered-waterfall-l0 — pure conservation logic for partitioning ONE eligible amount across
 * priority-ordered, individually-capped, gating tiers, with residual carry-forward and a cross-tier
 * retroactive clamp. Cross-cutting primitive (no Spring, no DB): the caller loads each tier's current
 * headroom (under a row lock, in deterministic order — WF-LOCK-001), calls {@link #allocate}, then
 * advances each tier's accumulator by the returned absorb amount inside ONE transaction (WF-ATOMIC-001).
 *
 * <p>The member side is computed tier-by-tier; the counterparty (insurer) side is ALWAYS the exact
 * residual {@code eligible - memberPaid} — never independently rounded — so conservation
 * {@code memberPaid + counterpartyPaid == eligible} holds exactly (WF-CONSERVE-001, Martin Fowler
 * Money pattern penny hazard). Generalizes beyond insurance: tax-bracket marginal calc,
 * payment-application (fees -> interest -> principal), debt-payoff cascade.
 */
public final class WaterfallAllocator {

    private WaterfallAllocator() {}

    public enum TierKind {
        /** Member absorbs up to {@code amount} (a cap), e.g. a deductible. */
        ABSORB_TO_CAP,
        /** Member absorbs {@code amount} (a rate in [0,1]) of the running remainder, e.g. coinsurance. */
        COINSURANCE,
        /** Clamp the member's running total to {@code amount} (a cap), e.g. out-of-pocket maximum. */
        CLAMP_TOTAL
    }

    /** A tier in fixed order. {@code accumulatorKey} may be null for a tier with no persisted accumulator. */
    public record Tier(String accumulatorKey, TierKind kind, BigDecimal amount) {}

    /**
     * How much to advance a tier's accumulator. For an ABSORB_TO_CAP tier this is the member's
     * INCREMENTAL contribution to that bucket (post-clamp). For a CLAMP_TOTAL tier this is the
     * member's CUMULATIVE running total (e.g. an out-of-pocket-max accumulator legitimately includes
     * the deductible spend) — so CLAMP_TOTAL advances OVERLAP earlier ABSORB buckets and the
     * advances list MUST NOT be summed to recover memberPaid.
     */
    public record Absorb(String accumulatorKey, BigDecimal amount) {}

    public record Result(BigDecimal memberPaid, BigDecimal counterpartyPaid, List<Absorb> advances) {}

    /**
     * @param eligible the inbound amount to partition (non-negative)
     * @param tiers    fixed, ordered tiers (an immutable List — never an unordered set)
     * @param scale    money scale for rate-rounding (HALF_EVEN); the counterparty side is the exact residual
     */
    public static Result allocate(BigDecimal eligible, List<Tier> tiers, int scale) {
        BigDecimal remainder = eligible;                 // not-yet-allocated amount
        // member-side contributions in tier order (key nullable, e.g. coinsurance has no accumulator).
        // Mutated by the CLAMP_TOTAL pass so the emitted advances reflect the POST-clamp split.
        List<Absorb> memberContribs = new ArrayList<>();
        String clampKey = null;
        BigDecimal clampCap = null;

        for (Tier t : tiers) {
            switch (t.kind()) {
                case ABSORB_TO_CAP -> {
                    BigDecimal take = remainder.min(t.amount()).max(z(scale));
                    remainder = remainder.subtract(take);
                    memberContribs.add(new Absorb(t.accumulatorKey(), take));
                }
                case COINSURANCE -> {
                    BigDecimal memberShare = remainder.multiply(t.amount()).setScale(scale, RoundingMode.HALF_EVEN);
                    if (memberShare.compareTo(remainder) > 0) memberShare = remainder;   // never exceed remainder
                    remainder = remainder.subtract(memberShare);
                    memberContribs.add(new Absorb(t.accumulatorKey(), memberShare));
                }
                case CLAMP_TOTAL -> {                     // a clamp does not contribute; it caps the running total
                    clampKey = t.accumulatorKey();
                    clampCap = t.amount();
                }
            }
        }

        BigDecimal memberRunning = memberContribs.stream().map(Absorb::amount).reduce(z(scale), BigDecimal::add);

        // WF-CLAMP-001 — clamp the member total to the cap, clawing the excess back from the prior
        // member contributions in REVERSE tier order (later tiers, e.g. coinsurance, are reduced before
        // earlier tiers, e.g. the deductible — which is "locked in" first). This keeps each emitted
        // Absorb consistent with the member's ACTUAL post-clamp contribution to that bucket, so the
        // deductible accumulator is never advanced by money the OOP-max clamp moved to the insurer.
        if (clampCap != null && memberRunning.compareTo(clampCap) > 0) {
            BigDecimal excess = memberRunning.subtract(clampCap);
            for (int i = memberContribs.size() - 1; i >= 0 && excess.signum() > 0; i--) {
                Absorb c = memberContribs.get(i);
                BigDecimal reduce = excess.min(c.amount());
                memberContribs.set(i, new Absorb(c.accumulatorKey(), c.amount().subtract(reduce)));
                excess = excess.subtract(reduce);
            }
            memberRunning = clampCap;
        }

        BigDecimal memberPaid = memberRunning.min(eligible).max(z(scale));
        BigDecimal counterpartyPaid = eligible.subtract(memberPaid);   // EXACT residual — conservation

        List<Absorb> advances = new ArrayList<>();
        for (Absorb c : memberContribs) {                 // incremental member buckets (post-clamp)
            if (c.accumulatorKey() != null) advances.add(c);
        }
        if (clampKey != null) {                           // the clamp accumulator = CUMULATIVE member total
            advances.add(new Absorb(clampKey, memberPaid));
        }
        return new Result(memberPaid, counterpartyPaid, List.copyOf(advances));
    }

    private static BigDecimal z(int scale) {
        return BigDecimal.ZERO.setScale(scale);
    }
}
