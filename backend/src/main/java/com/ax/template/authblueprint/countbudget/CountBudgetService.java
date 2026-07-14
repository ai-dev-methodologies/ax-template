package com.ax.template.authblueprint.countbudget;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.MemberWriter;

/**
 * periodic-count-budget-l0 sole orchestrator. Every consume acquires the POLICY row under
 * PESSIMISTIC_WRITE (PCB-CONSUME-001) so lazy period-creation AND consumption both serialize on ONE lock —
 * two concurrent consumes for the same subject can never both accept past the captured cap. The active
 * period is derived PURELY from the cadence + instant (PCB-RESET-001, never completion-triggered); its
 * {@code capAtPeriodStart} is captured once, at first touch, from the policy's cap AT THAT MOMENT
 * (PCB-CAP-001) — a later cap change never reshapes an already-touched period. The consumed count is
 * DERIVED (a COUNT of the append-only ledger), never a separately-stored total (PCB-AUDIT-001).
 */
@Service
public class CountBudgetService {

    private final CountBudgetPolicyRepository policies;
    private final MemberWriter members;
    private final CountBudgetMetrics metrics;
    private final Clock clock;

    public CountBudgetService(CountBudgetPolicyRepository policies, MemberWriter members,
                              CountBudgetMetrics metrics, Clock clock) {
        this.policies = policies;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    public record ConsumeResult(CountBudgetPeriod period, long consumedCount) {}

    @Transactional
    public CountBudgetPolicy createPolicy(String subjectKey, CountBudgetCadence cadence, int cap) {
        if (cadence == null || cap <= 0) {
            metrics.record("create", "invalid");
            throw CountBudgetException.invalidValue();
        }
        if (policies.existsBySubjectKey(subjectKey)) {
            metrics.record("create", "rejected");
            throw CountBudgetException.duplicateSubject();
        }
        try {
            CountBudgetPolicy p = policies.saveAndFlush(
                new CountBudgetPolicy(UUID.randomUUID(), subjectKey, cadence, cap, Instant.now(clock)));
            metrics.record("create", "ok");
            return p;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw CountBudgetException.duplicateSubject();
        }
    }

    /** PCB-CAP-001 — takes effect for periods NOT YET first-touched; an already-touched period keeps its
     *  own captured cap. */
    @Transactional
    public CountBudgetPolicy updateCap(String subjectKey, int newCap) {
        if (newCap <= 0) {
            metrics.record("update_cap", "invalid");
            throw CountBudgetException.invalidValue();
        }
        CountBudgetPolicy p = policies.findBySubjectKeyForUpdate(subjectKey)
            .orElseThrow(CountBudgetException::notFound);
        p.updateCap(newCap);
        metrics.record("update_cap", "ok");
        return p;
    }

    /** PCB-CONSUME-001 / PCB-RESET-001 / PCB-AUDIT-001 — consume under the policy row lock; {@code asOf}
     *  is optional (defaults to the injected clock) so a calendar boundary can be crossed deterministically
     *  in tests. */
    @Transactional
    public ConsumeResult consume(String subjectKey, Instant asOf) {
        CountBudgetPolicy policy = policies.findBySubjectKeyForUpdate(subjectKey)
            .orElseThrow(CountBudgetException::notFound);
        Instant at = asOf == null ? Instant.now(clock) : asOf;
        String periodKey = policy.getCadence().periodKeyFor(at);

        CountBudgetPeriod period = policies.findPeriod(policy.getId(), periodKey).orElseGet(() -> {
            // PCB-RESET-001 — lazy fresh start; PCB-CAP-001 — captures the cap AT THIS MOMENT.
            CountBudgetPeriod fresh = members.persist(new CountBudgetPeriod(UUID.randomUUID(), policy.getId(),
                periodKey, policy.getCap(), at));
            metrics.record("consume", "first_touch");
            return fresh;
        });

        long consumedSoFar = policies.countConsumptions(period.getId());
        if (consumedSoFar >= period.getCapAtPeriodStart()) {
            metrics.record("consume", "exhausted");
            throw CountBudgetException.budgetExhausted();
        }
        long seq = policies.maxConsumptionSequence(period.getId()) + 1;
        members.persist(new CountBudgetConsumption(UUID.randomUUID(), period.getId(), seq, at));
        metrics.record("consume", "ok");
        return new ConsumeResult(period, consumedSoFar + 1);
    }

    @Transactional(readOnly = true)
    public CountBudgetPolicy getPolicy(String subjectKey) {
        return policies.findBySubjectKey(subjectKey).orElseThrow(CountBudgetException::notFound);
    }

    @Transactional(readOnly = true)
    public ConsumeResult getPeriod(String subjectKey, String periodKey) {
        CountBudgetPolicy policy = getPolicy(subjectKey);
        CountBudgetPeriod period = policies.findPeriod(policy.getId(), periodKey)
            .orElseThrow(CountBudgetException::notFound);
        return new ConsumeResult(period, policies.countConsumptions(period.getId()));
    }

    @Transactional(readOnly = true)
    public Page<CountBudgetPeriod> listPeriods(String subjectKey, int page, int size) {
        CountBudgetPolicy policy = getPolicy(subjectKey);
        return policies.findPeriodsPage(policy.getId(), PageRequest.of(safePage(page), safeSize(size)));
    }

    @Transactional(readOnly = true)
    public Page<CountBudgetConsumption> listConsumptions(String subjectKey, String periodKey, int page, int size) {
        CountBudgetPolicy policy = getPolicy(subjectKey);
        CountBudgetPeriod period = policies.findPeriod(policy.getId(), periodKey)
            .orElseThrow(CountBudgetException::notFound);
        return policies.findConsumptionsPage(period.getId(), PageRequest.of(safePage(page), safeSize(size)));
    }

    private int safePage(int page) { return Math.max(page, 0); }

    private int safeSize(int size) { return Math.min(Math.max(size, 1), 200); }
}
