package com.ax.template.authblueprint.withholdingsplit;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * withholding-split-l0 sole orchestrator for {@link WithholdingPosting}. Every posting persists
 * EXACTLY two legs (WITHHOLDING + NET) in this one transaction (WHT-SPLIT-001): the withholding leg
 * is {@code gross * rate} rounded HALF_UP at currency scale, and the net leg is derived as
 * {@code gross - withholding} so the sum is exact BY CONSTRUCTION (WHT-RATE-002) — the pre-commit
 * re-sum below is a defense-in-depth backstop, not the primary correctness mechanism. A correction
 * ({@link #reverse}) creates a NEW posting with the negated gross, never edits the original
 * (WHT-IMMUTABLE-004).
 */
@Service
public class WithholdingPostingService {

    static final int LEG_SCALE = 2;
    static final int MAX_LEGS = 10;
    private static final Pattern PERIOD = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final WithholdingPostingRepository postings;
    private final MemberWriter members;
    private final Clock clock;

    public WithholdingPostingService(WithholdingPostingRepository postings, MemberWriter members, Clock clock) {
        this.postings = postings;
        this.members = members;
        this.clock = clock;
    }

    /**
     * @param period the business period this posting counts toward (e.g. a payroll month,
     *               {@code YYYY-MM}) — declared by the caller, never derived from the clock, so a
     *               posting created today can correctly belong to a prior settlement period.
     */
    @Transactional
    public WithholdingPosting post(BigDecimal gross, BigDecimal rate, String period) {
        return postInternal(gross, rate, period, null);
    }

    /** WHT-IMMUTABLE-004 — a correction is a NEW reversing posting, never an edit of the original;
     *  it inherits the ORIGINAL's period and rate so the reversal nets out in the same scope. */
    @Transactional
    public WithholdingPosting reverse(UUID originalPostingId) {
        WithholdingPosting original = postings.findById(originalPostingId)
            .orElseThrow(WithholdingSplitException::postingNotFound);
        return postInternal(original.getGrossAmount().negate(), original.getRate(), original.getPeriod(), original.getId());
    }

    private WithholdingPosting postInternal(BigDecimal gross, BigDecimal rate, String period, UUID correctionOf) {
        if (gross == null || gross.compareTo(BigDecimal.ZERO) == 0) {
            throw WithholdingSplitException.invalidGrossAmount();
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            throw WithholdingSplitException.invalidRate();
        }
        if (period == null || !PERIOD.matcher(period).matches()) {
            throw WithholdingSplitException.invalidPeriod();
        }
        UUID postingId = UUID.randomUUID();
        Instant now = Instant.now(clock);

        WithholdingPosting saved = postings.save(
            new WithholdingPosting(postingId, gross, rate, period, correctionOf, now));

        // WHT-RATE-002 — withholding rounds HALF_UP at currency scale; net absorbs the remainder by
        // construction (gross - withholding), so the two legs sum to gross exactly.
        BigDecimal withholding = gross.multiply(rate).setScale(LEG_SCALE, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(withholding);
        members.persist(new WithholdingLeg(UUID.randomUUID(), postingId, LegType.WITHHOLDING, withholding, now));
        members.persist(new WithholdingLeg(UUID.randomUUID(), postingId, LegType.NET, net, now));

        // WHT-SPLIT-001 backstop — re-read the just-written legs and re-assert the sum before commit.
        BigDecimal sum = postings.findLegs(postingId, PageRequest.of(0, MAX_LEGS)).stream()
            .map(WithholdingLeg::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(gross) != 0) {
            throw WithholdingSplitException.unbalancedSplit(sum.subtract(gross).toPlainString());
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public WithholdingPosting getPosting(UUID id) {
        return postings.findById(id).orElseThrow(WithholdingSplitException::postingNotFound);
    }

    @Transactional(readOnly = true)
    public List<WithholdingLeg> getLegs(UUID postingId) {
        getPosting(postingId);                                     // 404 before an empty list
        return postings.findLegs(postingId, PageRequest.of(0, MAX_LEGS));
    }
}
