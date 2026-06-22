package com.ax.template.authblueprint.dunning;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * dunning-collections-l0 sole orchestrator. The ladder is one-way and exactly-once
 * (DUNNING-LADDER-001): every advance takes the case's PESSIMISTIC_WRITE row lock and appends
 * a DunningStageTransition whose uq(case_id, stage) makes a re-emit a deterministic 409 even
 * under the concurrent-advance race (DUNNING-CONCURRENT-001). The aging bucket is computed
 * deterministically from days-overdue at a recorded as-of instant (DUNNING-AGING-001). A
 * payment opens a cure window; a full cure within it resets aging to CURRENT and halts the
 * ladder (recorded), a lapse releases the halt so the ladder resumes (DUNNING-CURE-001).
 * The reference cut-points (30/60/90 days) and stage set are a fork-receiver swap behind the
 * governance contract. Transition rows are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class DunningService {

    /** Reference cure-window length — a fork-receiver swap (the grace seam, not the contract). */
    static final long CURE_WINDOW_DAYS = 14L;
    static final String KIND_ADVANCE = "ADVANCE";
    static final String KIND_CURED = "CURED";

    private final DunningCaseRepository cases;
    private final MemberWriter members;
    private final DunningMetrics metrics;
    private final Clock clock;

    public DunningService(DunningCaseRepository cases, MemberWriter members,
                          DunningMetrics metrics, Clock clock) {
        this.cases = cases;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public DunningCase open(String receivableRef, LocalDate dueDate, BigDecimal overdueAmount) {
        DunningCase c = new DunningCase(UUID.randomUUID(), receivableRef, dueDate, overdueAmount,
            Instant.now(clock));
        reageInPlace(c, Instant.now(clock));            // record the initial bucket + basis
        DunningCase saved = cases.save(c);
        metrics.record("open", "ok");
        return saved;
    }

    /** DUNNING-LADDER/CONCURRENT-001 — advance from the rung the caller OBSERVED ({@code fromStage})
     *  to its single successor. The case row's PESSIMISTIC_WRITE lock serializes the
     *  read-stage / write-next-stage sequence so that across N concurrent calls all naming the
     *  SAME {@code fromStage} exactly one advances (the others find the case already moved past
     *  {@code fromStage} → 409); the uq(case_id, stage) backstop makes the loser deterministic
     *  even if a residual race slipped the lock (CWE-362). The ladder is one-way: a stale or
     *  out-of-order {@code fromStage} can never reach a rung twice or skip one. */
    @Transactional
    public DunningCase advance(UUID caseId, DunningStage fromStage, String actor) {
        DunningCase c = cases.findByIdForUpdate(caseId).orElseThrow(DunningException::notFound);
        if (c.getStage() == DunningStage.SUSPENDED) {
            metrics.record("advance", "terminal");
            throw DunningException.ladderTerminal();           // SUSPENDED is terminal — one-way
        }
        if (c.getStage() != fromStage) {
            metrics.record("advance", "already_reached");      // a faster advance already moved it
            throw DunningException.stageAlreadyReached();
        }
        DunningStage nextStage = c.getStage().next();          // never null here (not SUSPENDED)
        Instant now = Instant.now(clock);
        long days = daysOverdue(c.getDueDate(), now);
        try {
            // uq(case_id, stage) — appending the same rung twice is the exactly-once backstop.
            members.persistAndFlush(new DunningStageTransition(UUID.randomUUID(), c.getId(),
                nextStage, KIND_ADVANCE, days, actor, now));
        } catch (DataIntegrityViolationException dup) {
            metrics.record("advance", "already_reached");
            throw DunningException.stageAlreadyReached();       // loser of the concurrent advance
        }
        c.advanceTo(nextStage);
        reageInPlace(c, now);
        metrics.record("advance", "advanced");
        return c;
    }

    /** DUNNING-AGING-001 — recompute the deterministic bucket at a recorded as-of instant. */
    @Transactional
    public DunningCase reage(UUID caseId) {
        DunningCase c = cases.findByIdForUpdate(caseId).orElseThrow(DunningException::notFound);
        Instant now = Instant.now(clock);
        boolean lapsed = c.getCureDeadline() != null && !now.isBefore(c.getCureDeadline());
        if (lapsed && c.isLadderHalted()) {
            c.releaseHalt();                                     // a lapsed cure resumes the ladder
        }
        reageInPlace(c, now);
        metrics.record("reage", "ok");
        return c;
    }

    /** DUNNING-CURE-001 — record a payment; open/keep the cure window; full cure resets + halts. */
    @Transactional
    public DunningCase pay(UUID caseId, BigDecimal amount) {
        DunningCase c = cases.findByIdForUpdate(caseId).orElseThrow(DunningException::notFound);
        Instant now = Instant.now(clock);
        c.openCureWindow(now, now.plus(CURE_WINDOW_DAYS, ChronoUnit.DAYS));
        c.addPayment(amount);
        metrics.record("pay", c.isFullyPaid() ? "cured" : "ok");
        return c;
    }

    /** DUNNING-CURE-001 — full cure WITHIN the window resets to CURRENT + halts; idempotent. */
    @Transactional
    public DunningCase cure(UUID caseId, String actor) {
        DunningCase c = cases.findByIdForUpdate(caseId).orElseThrow(DunningException::notFound);
        if (c.isLadderHalted() && c.getAgingBucket() == AgingBucket.CURRENT) {
            metrics.record("cure", "cured");                     // already cured — idempotent no-op
            return c;
        }
        Instant now = Instant.now(clock);
        boolean windowOpen = c.getCureDeadline() != null && now.isBefore(c.getCureDeadline());
        if (!windowOpen || !c.isFullyPaid()) {
            metrics.record("cure", "no_window");
            throw DunningException.noCureWindow();
        }
        members.persistAndFlush(new DunningStageTransition(UUID.randomUUID(), c.getId(),
            c.getStage(), KIND_CURED, c.getDaysOverdue(), actor, now));   // recorded halt
        c.cure();
        metrics.record("cure", "cured");
        return c;
    }

    @Transactional(readOnly = true)
    public DunningCase get(UUID caseId) {
        return cases.findById(caseId).orElseThrow(DunningException::notFound);
    }

    @Transactional(readOnly = true)
    public List<DunningStageTransition> transitions(UUID caseId) {
        get(caseId);                                             // 404 before an empty list
        return cases.findTransitions(caseId);
    }

    private void reageInPlace(DunningCase c, Instant asOf) {
        long days = daysOverdue(c.getDueDate(), asOf);
        c.reage(AgingBucket.of(days), asOf, days);
    }

    /** Whole days from the due date to the as-of instant (negative ⇒ not yet due ⇒ CURRENT). */
    private static long daysOverdue(LocalDate dueDate, Instant asOf) {
        LocalDate asOfDate = asOf.atZone(ZoneOffset.UTC).toLocalDate();
        return ChronoUnit.DAYS.between(dueDate, asOfDate);
    }
}
