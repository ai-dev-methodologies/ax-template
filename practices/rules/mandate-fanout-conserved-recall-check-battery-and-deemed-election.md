---
title: A one-directive fan-out must create EXACTLY N child tasks atomically and report completion as a DERIVED conserved recall (Σ terminal == N, never a stored flag), gate the mandate behind a pass-ALL check battery (every declared check recorded passed, else 422), auto-resolve a child's silence past its deadline to a recorded DEEMED default election EXACTLY ONCE via a @Scheduled poller driving a proxied @Transactional worker, and serialize the explicit child-complete against the deemed sweep on the task row so each child reaches a terminal state exactly once
impact: HIGH
impactDescription: "A mandate whose completion is a stored boolean drifts from its children — it can report DONE while a child is still pending (the WCP-3 synchronization barrier broken), and partial fan-out (recorded N but fewer rows) silently under-delivers a directive; a battery gate that trusts a bare aggregate clears a mandate while a required safety/authz check is still failing or missing; a deemed default that is not recorded (or fires twice) either silently terminates a task with no audit trail or double-resolves it; and a deemed sweep that races an explicit complete without the task-row lock double-terminates one task (CWE-362) — two resolvers, two resolved_at, a corrupted terminal"
tags:
  - state-machine
  - audit
  - concurrency
  - governance
spec_ref: "specs/mandate-fanout-l0.yaml#MANDATE-FANOUT-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/mandate/MandateService.java + backend/src/main/java/com/ax/template/authblueprint/mandate/Mandate.java + backend/src/main/java/com/ax/template/authblueprint/mandate/MandateTask.java + backend/src/main/java/com/ax/template/authblueprint/mandate/MandateDeemedSweeper.java"
  pattern: "Issuing a mandate creates EXACTLY N MandateTask children in one transaction and records issuedCount = N (a @Check keeps issuedCount positive; uq(mandate_id, task_seq) makes partial fan-out unrepresentable); completion is a DERIVED recall — count terminal children and compare to issuedCount (Σ terminal == issuedCount), never a stored boolean; the mandate is SATISFIED only when EVERY declared check key has a recorded PASSED MandateCheck verdict, else 422; a MandateTask past its deadline with no explicit response is auto-resolved to DEEMED (resolver SYSTEM, reason DEEMED) exactly once via MandateService.resolveDeemed, a @Transactional worker the @Scheduled MandateDeemedSweeper drives through a CROSS-BEAN proxied call (never a bare same-bean self-invocation — the dunning/obligation lesson); the explicit complete path and the deemed worker BOTH take the task's PESSIMISTIC_WRITE row lock and resolve only a PENDING task, so across any interleaving the task reaches a terminal state exactly once (CWE-362); NO delete path exists on the mandate or its children"
upstream:
  - "https://en.wikipedia.org/wiki/Workflow_patterns"
  - "https://www.law.cornell.edu/cfr/text/16/310.2"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "van der Aalst et al., Workflow Patterns — WCP-2 Parallel Split and WCP-3 Synchronization (Wikipedia summary of the van der Aalst classification): the AND-split fan-out and the barrier-synchronization completion recall the mandate generalizes"
    url: "https://en.wikipedia.org/wiki/Workflow_patterns"
    quote: "synchronize two or more activities that may execute in any order or in parallel; do not proceed with the execution of subsequent activities until all preceding activities have completed"
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "16 CFR § 310.2(w) (Telemarketing Sales Rule, Cornell LII) — the negative-option / deemed-acceptance authority the deemed-default election generalizes: a customer's silence is interpreted as acceptance"
    url: "https://www.law.cornell.edu/cfr/text/16/310.2"
    quote: "a provision under which the customer's silence or failure to take an affirmative action to reject goods or services or to cancel the agreement is interpreted by the seller as acceptance of the offer"
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (the explicit child-complete racing the deemed sweep on one task)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A directive fan-out is exactly-N children, a derived conserved recall, a pass-all battery, and a deemed default — not a stored completion flag

**Impact: HIGH — a stored completion boolean drifts from its children (DONE while a child is still pending); a battery gate on a bare aggregate clears a mandate with a failing check; an unrecorded or double-firing deemed default loses the audit trail or double-terminates; an unsynchronized deemed sweep double-resolves a task (CWE-362).**

A *mandate* is one directive that fans out to N child tasks and is complete only when every child finishes — the van der Aalst WCP-2 Parallel Split (the AND-split fan-out) followed by the WCP-3 Synchronization barrier: *"do not proceed with the execution of subsequent activities until all preceding activities have completed."* The catalog governed obligations (`deadline-obligation`: ONE grounded deadline, ack-only terminal), sequential approvals (`approval-workflow`: ordered steps along one chain), and positive companion gates (`authorization-parity`), but had no primitive for ONE directive fanning out to N tasks with a CONSERVED completion recall, a pass-all check battery, and a deemed default on silence:

```text
issue(mandate, N, checks):  ONE transaction creates EXACTLY N MandateTask children (task_seq
                            unique per mandate); issuedCount = N recorded; @Check ties the count
                            to the created rows so partial fan-out is unrepresentable
complete:                   a DERIVED recall — count terminal children, compare to issuedCount
                            (Σ terminal == issuedCount); NEVER a stored boolean that can drift
battery:                    SATISFIED only when EVERY declared check key has a recorded PASSED
                            MandateCheck verdict; a missing/failing check → 422
deemed:                     a task past its deadline with no response → DEEMED (resolver SYSTEM,
                            reason DEEMED), exactly once, via @Scheduled poller → cross-bean
                            proxied @Transactional worker (MandateService.resolveDeemed)
locks:                      the task row, PESSIMISTIC_WRITE — explicit complete vs the deemed sweep
                            resolve a PENDING task → terminal exactly once
```

**1. Fan-out is exactly-N and completion is a derived recall (MANDATE-FANOUT-001).** Issue creates exactly N children in one transaction and records `issuedCount`; the `@Check issued_count >= 0` plus `uq(mandate_id, task_seq)` keep partial fan-out unrepresentable, and `complete` is computed by counting terminal children — never stored.

**2. The check battery is pass-all (MANDATE-BATTERY-001).** Every declared check key must have a recorded PASSED `MandateCheck` verdict before `satisfy` clears the mandate; a single FAILED or still-missing check is a 422. Verdicts are recorded per key (`uq(mandate_id, check_key)`), idempotent on the key, never a bare aggregate.

**3. Silence past the deadline is a recorded deemed default (MANDATE-DEEMED-001).** A `@Scheduled` poller (`MandateDeemedSweeper`) drives the `@Transactional` worker `MandateService.resolveDeemed` through a CROSS-BEAN proxied call — never a bare same-bean `this.resolveDeemed(...)` self-invocation, which would bypass the proxy and silently drop the row lock on the production path (the dunning/obligation lesson; an @Lazy self-reference would be required only if the worker lived on the poller itself). It resolves an overdue, unanswered task to DEEMED (resolver SYSTEM, reason DEEMED) exactly once.

**Incorrect — a stored completion flag, an aggregate battery gate, an unrecorded + unsynchronized deemed default:**

```java
public void completeTask(UUID taskId) {
    MandateTask t = repo.findById(taskId).orElseThrow();   // ❌ no row lock — deemed sweep races this
    t.setState("DONE");                                    // ❌ public setter; resolves a non-PENDING task too
    Mandate m = t.getMandate();
    m.setComplete(m.allTasksDoneFlag());                   // ❌ stored boolean — drifts from the children
    repo.save(t);                                          // ❌ sweep + this both write → double-terminal (CWE-362)
}
public boolean canSatisfy(Mandate m) { return m.isAllChecksOk(); }   // ❌ bare aggregate, not per-check verdicts
```

**Correct — exactly-N fan-out, derived recall, pass-all battery, locked deemed default:**

```java
@Transactional
public Mandate issue(String directive, int taskCount, List<String> checkKeys, String actor) {
    if (taskCount <= 0) throw MandateException.emptyFanout();                  // 422 — N must be positive
    Instant now = Instant.now(clock);
    Mandate m = new Mandate(UUID.randomUUID(), directive, taskCount, now);     // issuedCount = N
    mandates.saveAndFlush(m);
    Instant deemedDeadline = now.plus(deemedWindowDays, ChronoUnit.DAYS);      // each child's deemed deadline
    for (int seq = 0; seq < taskCount; seq++) {
        members.persist(new MandateTask(UUID.randomUUID(), m.getId(), seq,     // ✅ exactly N children, one tx
            deemedDeadline, now));
    }
    for (String key : Set.copyOf(checkKeys)) {
        members.persist(MandateCheck.declared(UUID.randomUUID(), m.getId(), key, now));  // declared, no verdict
    }
    return m;
}

@Transactional
public MandateTask completeTask(UUID taskId, MandateTaskState target, String actor) {
    MandateTask t = mandates.findTaskByIdForUpdate(taskId)                     // ✅ PESSIMISTIC_WRITE on the task
        .orElseThrow(MandateException::notFound);
    if (t.getState() != MandateTaskState.PENDING)                              // ✅ resolve only a PENDING task
        throw MandateException.taskAlreadyResolved();                          // 409 — loser of any race
    t.resolve(target, actor, MandateTask.REASON_EXPLICIT, Instant.now(clock));
    return t;
}

@Transactional
public Mandate satisfy(UUID mandateId, String actor) {
    Mandate m = mandates.findByIdForUpdate(mandateId).orElseThrow(MandateException::notFound);
    List<MandateCheck> battery = mandates.findChecks(m.getId());
    boolean allPassed = !battery.isEmpty()
        && battery.stream().allMatch(c -> c.getVerdict() == MandateCheckVerdict.PASSED);  // ✅ per-check, pass-ALL
    if (!allPassed) throw MandateException.batteryIncomplete();                // 422 — a missing/failing check blocks
    m.markSatisfied(actor, Instant.now(clock));
    return m;
}
```

The completion recall is computed, not stored: `countTerminalTasks(mandateId) == m.getIssuedCount()` — it cannot drift from the children (the WCP-3 barrier). The deemed sweep (`MandateDeemedSweeper`) drives the `@Transactional MandateService.resolveDeemed` worker through a cross-bean proxied call (never a same-bean self-invocation), which takes the same task-row `PESSIMISTIC_WRITE` lock and resolves only a PENDING-and-overdue task, so the explicit-complete path and the sweep converge to exactly one terminal resolution per task (CWE-362). `MandateTask` and `MandateCheck` rows are `@AggregateMember` of `Mandate` — root-JPQL reads, `common/MemberWriter` writes; no delete path exists.

Verification: review-tier — confirm issue creates exactly N children in one transaction with a recorded issuedCount, completion is a derived count of terminal children (never a stored flag), `satisfy` requires every declared check recorded PASSED, the deemed sweep resolves an overdue unanswered task to DEEMED exactly once through an `@Lazy`-self `@Transactional` worker under the task-row lock, and no delete path exists. The behavioural proof a fork-receiver keeps green: the N-thread explicit-complete race (exactly one 2xx + N-1 409, one resolver/resolved_at) and the derived-recall completion check.

Reference: [van der Aalst Workflow Patterns (WCP-2 Parallel Split / WCP-3 Synchronization)](https://en.wikipedia.org/wiki/Workflow_patterns)

Reference: [16 CFR § 310.2 (negative option feature — silence interpreted as acceptance)](https://www.law.cornell.edu/cfr/text/16/310.2)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
