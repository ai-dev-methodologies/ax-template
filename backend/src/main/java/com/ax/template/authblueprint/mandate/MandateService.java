package com.ax.template.authblueprint.mandate;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * mandate-fanout-l0 sole orchestrator. A directive fans out to EXACTLY N child tasks in one
 * transaction (MANDATE-FANOUT-001) and completion is a DERIVED recall — terminal children counted
 * against the immutable issuedCount (Σ terminal == issuedCount), never a stored flag. The mandate
 * is SATISFIED only when EVERY declared check has a recorded PASSED verdict (MANDATE-BATTERY-001).
 * The explicit-complete path and the deemed sweep BOTH take the task's PESSIMISTIC_WRITE row lock
 * and resolve only a PENDING task, so each task reaches a terminal state exactly once
 * (MANDATE-CONCURRENT-001 / CWE-362). The deemed-default election (MANDATE-DEEMED-001) lives in
 * {@link #resolveDeemed} — driven by {@link MandateDeemedSweeper} through an @Lazy self-reference.
 * Members are written via {@link MemberWriter} and read via JPQL on {@link MandateRepository}.
 */
@Service
public class MandateService {

    /** Reference deemed-default window in days — a fork-receiver swap (the negative-option seam,
     *  not the contract). A negative value lets a deterministic test make a task immediately
     *  overdue (deadline = now + windowDays, in the past) without moving the injected Clock. */
    static final long DEFAULT_DEEMED_WINDOW_DAYS = 7L;

    private final long deemedWindowDays;
    private final MandateRepository mandates;
    private final MemberWriter members;
    private final MandateMetrics metrics;
    private final Clock clock;

    public MandateService(MandateRepository mandates, MemberWriter members,
                          MandateMetrics metrics, Clock clock,
                          @Value("${mandate.deemed-window-days:7}") long deemedWindowDays) {
        this.mandates = mandates;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
        this.deemedWindowDays = deemedWindowDays;
    }

    /** A derived projection of a mandate + its conserved completion recall — never a stored flag. */
    public record MandateView(Mandate mandate, long terminalCount, boolean complete) {}

    /** MANDATE-FANOUT-001 — issue atomically creates EXACTLY N children + the declared check battery. */
    @Transactional
    public Mandate issue(String directive, int taskCount, List<String> checkKeys, String actor) {
        if (taskCount <= 0) {
            metrics.record("issue", "empty_fanout");
            throw MandateException.emptyFanout();
        }
        Instant now = Instant.now(clock);
        Mandate m = new Mandate(UUID.randomUUID(), directive, taskCount, now);
        mandates.saveAndFlush(m);                                  // the recorded issuedCount = N
        Instant deemedDeadline = now.plus(deemedWindowDays, ChronoUnit.DAYS);
        for (int seq = 0; seq < taskCount; seq++) {
            members.persist(new MandateTask(UUID.randomUUID(), m.getId(), seq, deemedDeadline, now));
        }
        // de-dup the declared battery (a check key is unique per mandate); preserve declaration order
        Set<String> distinct = new LinkedHashSet<>(checkKeys == null ? List.of() : checkKeys);
        for (String key : distinct) {
            members.persist(MandateCheck.declared(UUID.randomUUID(), m.getId(), key, now));
        }
        metrics.record("issue", "ok");
        return m;
    }

    /** MANDATE-FANOUT/CONCURRENT-001 — an explicit response (DONE/DECLINED) on ONE child, exactly once. */
    @Transactional
    public MandateTask completeTask(UUID taskId, MandateTaskState target, String actor) {
        if (target != MandateTaskState.DONE && target != MandateTaskState.DECLINED) {
            metrics.record("complete_task", "invalid");
            throw MandateException.invalidTaskTarget();
        }
        MandateTask t = mandates.findTaskByIdForUpdate(taskId)   // PESSIMISTIC_WRITE on the task row
            .orElseThrow(MandateException::notFound);
        if (!t.isPending()) {
            metrics.record("complete_task", "already_resolved");  // loser of the explicit/deemed race
            throw MandateException.taskAlreadyResolved();
        }
        t.resolve(target, actor, MandateTask.REASON_EXPLICIT, Instant.now(clock));
        metrics.record("complete_task", "resolved");
        return t;
    }

    /** MANDATE-DEEMED-001 — the deemed-default election worker. Idempotent + lock-serialized: it
     *  resolves ONLY a still-PENDING, deadline-passed task to DEEMED (resolver SYSTEM, reason
     *  DEEMED); a non-PENDING task (an explicit response already won) or a not-yet-overdue task is
     *  a no-op. Called by {@link MandateDeemedSweeper} through its @Lazy self-reference so the
     *  @Transactional proxy + the row lock are NOT bypassed. Returns true iff it resolved. */
    @Transactional
    public boolean resolveDeemed(UUID taskId) {
        MandateTask t = mandates.findTaskByIdForUpdate(taskId).orElse(null);
        if (t == null) {
            return false;
        }
        if (!t.isPending()) {
            metrics.record("deemed", "skipped");                 // an explicit response already terminal
            return false;
        }
        Instant now = Instant.now(clock);
        if (now.isBefore(t.getDeemedDeadline())) {
            metrics.record("deemed", "skipped");                 // not yet overdue — never deem early
            return false;
        }
        t.resolve(MandateTaskState.DEEMED, MandateTask.SYSTEM_RESOLVER, MandateTask.REASON_DEEMED, now);
        metrics.record("deemed", "deemed");
        return true;
    }

    /** MANDATE-BATTERY-001 — record/supersede a single check's verdict (idempotent on the key). */
    @Transactional
    public MandateCheck recordCheck(UUID mandateId, String checkKey, MandateCheckVerdict verdict, String actor) {
        if (verdict != MandateCheckVerdict.PASSED && verdict != MandateCheckVerdict.FAILED) {
            metrics.record("record_check", "invalid");
            throw MandateException.invalidVerdict();
        }
        mandates.findById(mandateId).orElseThrow(MandateException::notFound);
        MandateCheck c = mandates.findCheck(mandateId, checkKey).orElseThrow(() -> {
            metrics.record("record_check", "unknown_check");
            return MandateException.unknownCheck();              // a verdict only for a declared check
        });
        c.record(verdict, actor, Instant.now(clock));            // same row — supersession, no duplicate
        metrics.record("record_check", "ok");
        return c;
    }

    /** MANDATE-BATTERY-001 — SATISFIED only when EVERY declared check is recorded PASSED, else 422. */
    @Transactional
    public Mandate satisfy(UUID mandateId, String actor) {
        Mandate m = mandates.findByIdForUpdate(mandateId).orElseThrow(MandateException::notFound);
        if (m.isSatisfied()) {
            metrics.record("satisfy", "satisfied");              // idempotent — already cleared
            return m;
        }
        List<MandateCheck> battery = mandates.findChecks(m.getId());
        boolean allPassed = !battery.isEmpty() && battery.stream().allMatch(MandateCheck::isPassed);
        if (!allPassed) {
            metrics.record("satisfy", "battery_incomplete");
            throw MandateException.batteryIncomplete();          // a missing/failing check blocks
        }
        m.markSatisfied(actor, Instant.now(clock));
        metrics.record("satisfy", "satisfied");
        return m;
    }

    @Transactional(readOnly = true)
    public MandateView get(UUID mandateId) {
        Mandate m = mandates.findById(mandateId).orElseThrow(MandateException::notFound);
        long terminal = mandates.countTerminalTasks(m.getId());  // the DERIVED completion recall
        return new MandateView(m, terminal, terminal == m.getIssuedCount());
    }

    @Transactional(readOnly = true)
    public List<MandateTask> tasks(UUID mandateId) {
        mandates.findById(mandateId).orElseThrow(MandateException::notFound);   // 404 before an empty list
        return mandates.findTasks(mandateId);
    }

    @Transactional(readOnly = true)
    public List<MandateCheck> checks(UUID mandateId) {
        mandates.findById(mandateId).orElseThrow(MandateException::notFound);
        return mandates.findChecks(mandateId);
    }
}
