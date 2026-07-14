package com.ax.template.authblueprint.additivefacts;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * additive-fact-ledger-l0 sole orchestrator. Distinct from remeasurement-trueup (a single
 * REPLACE-by-supersession value per slot): here MANY small facts ACCUMULATE per period
 * (FACT-ADDITIVE-ACCUM-001). A late fact for a CLOSED period never mutates that period's
 * frozen aggregate — it posts forward as a {@link LateDeltaPosting} into a caller-designated
 * OPEN period (FACT-LATE-DELTA-POST-002). Fact ingestion is idempotent on
 * {@code (source, external_fact_id)} — a duplicate delivery accumulates once (FACT-IDEMPOTENT-004).
 */
@Service
public class AdditiveFactService {

    private final FactPeriodRepository periods;
    private final MemberWriter members;
    private final Clock clock;

    public AdditiveFactService(FactPeriodRepository periods, MemberWriter members, Clock clock) {
        this.periods = periods;
        this.members = members;
        this.clock = clock;
    }

    @Transactional
    public FactPeriod createPeriod(String subject, String label) {
        return periods.save(new FactPeriod(UUID.randomUUID(), subject, label, Instant.now(clock)));
    }

    /**
     * FACT-IDEMPOTENT-004 — dedup FIRST: a redelivered (source, externalFactId) is a no-op
     * regardless of which periodId the retry names. Only a genuinely new fact reaches the
     * period lock and, for a CLOSED origin, the forward-posting path (FACT-LATE-DELTA-POST-002).
     */
    @Transactional
    public Fact addFact(UUID periodId, String source, String externalFactId, BigDecimal amount,
                        UUID currentOpenPeriodId) {
        Fact existing = periods.findFactBySourceAndExternalId(source, externalFactId).orElse(null);
        if (existing != null) {
            return existing;
        }

        FactPeriod period = periods.findByIdForUpdate(periodId).orElseThrow(AdditiveFactException::notFound);
        Fact fact = members.persist(new Fact(UUID.randomUUID(), period.getId(), source, externalFactId,
            amount, Instant.now(clock)));

        if (period.getStatus() == FactPeriodStatus.CLOSED) {
            postLateDelta(period, fact, currentOpenPeriodId);
        }
        return fact;
    }

    /** FACT-CLOSED-PERIOD-ADD-003 — closing FREEZES Σ facts; the column is then immutable. */
    @Transactional
    public FactPeriod close(UUID periodId) {
        FactPeriod period = periods.findByIdForUpdate(periodId).orElseThrow(AdditiveFactException::notFound);
        if (period.getStatus() != FactPeriodStatus.OPEN) {
            throw AdditiveFactException.invalidState();
        }
        BigDecimal total = periods.sumFactsForPeriod(periodId);
        period.close(total, Instant.now(clock));
        return period;
    }

    @Transactional(readOnly = true)
    public FactPeriod getPeriod(UUID id) {
        return periods.findById(id).orElseThrow(AdditiveFactException::notFound);
    }

    /** OPEN → derive-on-read Σ facts; CLOSED → the frozen aggregate, never recomputed. */
    @Transactional(readOnly = true)
    public BigDecimal total(UUID periodId) {
        FactPeriod period = getPeriod(periodId);
        return period.getStatus() == FactPeriodStatus.CLOSED
            ? period.getFrozenAggregate()
            : periods.sumFactsForPeriod(periodId);
    }

    @Transactional(readOnly = true)
    public List<Fact> factsOf(UUID periodId) {
        getPeriod(periodId);                                          // 404 before an empty list
        return periods.findFactsByPeriodId(periodId);
    }

    /** Postings CORRECTING this period (origin = id), wherever they were posted. */
    @Transactional(readOnly = true)
    public List<LateDeltaPosting> postingsFor(UUID originPeriodId) {
        getPeriod(originPeriodId);
        return periods.findPostingsByOrigin(originPeriodId);
    }

    private void postLateDelta(FactPeriod origin, Fact fact, UUID currentOpenPeriodId) {
        if (currentOpenPeriodId == null) {
            throw AdditiveFactException.currentPeriodRequired();
        }
        FactPeriod current = periods.findByIdForUpdate(currentOpenPeriodId)
            .orElseThrow(AdditiveFactException::notFound);
        if (current.getStatus() != FactPeriodStatus.OPEN) {
            throw AdditiveFactException.currentPeriodNotOpen();
        }
        members.persist(new LateDeltaPosting(UUID.randomUUID(), current.getId(), origin.getId(),
            fact.getId(), fact.getAmount(), Instant.now(clock)));
    }
}
