---
title: A period-seal watermark MUST be one-way monotonic — close only advances it, re-close is idempotent, reopen is privileged and audited
impact: HIGH
impactDescription: "If the sealed-through watermark can move backward or a period can be silently re-opened, a closed accounting/reporting period becomes mutable again — late writes slip into a period already reported to a regulator or to finance, and the books no longer foot to the filed numbers. Monotonic-only advance plus an audited reopen is what makes 'closed' actually mean closed."
tags:
  - sealed-period
  - watermark
  - monotonic
  - period-close
  - audit
spec_ref: "specs/sealed-period-l0.yaml#SEALED-MONOTONIC-001"
verification:
  type: review
  source: "specs/sealed-period-l0.yaml#SEALED-MONOTONIC-001"
  pattern: "The `sealed_through` watermark MUST be a one-way monotonic gate: a close operation may only ADVANCE it forward (`new_sealed_through > old_sealed_through`); a close that would move it backward or leave it unchanged MUST be rejected (no-op / 409). Re-closing an already-sealed period MUST be idempotent on the watermark value (same result, no double effect). Re-opening a sealed period MUST be a privileged, explicitly-audited action (who, when, justification) — never a silent un-seal. A structural backstop (a CHECK / guarded UPDATE asserting the new value strictly exceeds the old) enforces the monotonicity independent of the service path. Reject a watermark that can regress, a close that is not idempotent, and a reopen with no audited justification."
upstream:
  - "https://www.postgresql.org/docs/current/ddl-constraints.html"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — Constraints (CHECK enforces a Boolean condition on stored rows)"
    url: "https://www.postgresql.org/docs/current/ddl-constraints.html"
    quote: "A check constraint is the most generic constraint type. It allows you to specify that the value in a certain column must satisfy a Boolean (truth-value) expression."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A period-seal watermark MUST be one-way monotonic — close only advances, re-close idempotent, reopen privileged and audited

**Impact: HIGH — A sealed period (an accounting close, a reporting cutoff) is a promise: nothing in that range changes anymore. The promise rests on a `sealed_through` watermark that may move in only one direction. If a close could move it backward, or a period could be silently re-opened, a window already reported to finance or a regulator becomes writable again — a late posting slips in and the books no longer foot to the filed numbers, discovered months later in an audit. The companion `period-close-reject-late-write` rule rejects writes into a sealed range; THIS rule guarantees the seal itself only ever tightens. PostgreSQL's CHECK constraint — *the most generic constraint type ... specify that the value in a certain column must satisfy a Boolean (truth-value) expression* — is the structural backstop for the monotonic invariant.**

There are three load-bearing requirements for `SEALED-MONOTONIC-001`.

**1. One-way advance.** A close sets `sealed_through` to a value STRICTLY GREATER than the current one. A close whose target is `<= old` is rejected (no-op / 409). The watermark never regresses.

**2. Idempotent re-close.** Re-closing an already-sealed period (same target watermark) is idempotent — it returns the existing sealed state with no second effect, not a duplicate close event or an error.

**3. Privileged, audited reopen.** Re-opening a sealed period is an explicit, privileged action recorded with who/when/justification — there is NO silent path that un-seals. Reopen is the rare, accountable exception, not a routine mutation.

**Incorrect — a plain setter that accepts any value; reopen by nulling the watermark with no audit:**

```java
public void seal(LocalDate through) {
    period.setSealedThrough(through);   // VIOLATION: accepts a backward/unchanged value → watermark can regress
    periodRepo.save(period);            // (SEALED-MONOTONIC-001)
}
public void reopen() { period.setSealedThrough(null); }  // VIOLATION: silent un-seal, no privilege check, no audit
```

**Correct — guarded monotonic advance, idempotent re-close, audited reopen, DB CHECK backstop:**

```java
@Transactional
public SealResult seal(LocalDate through) {
    LocalDate current = period.getSealedThrough();
    if (current != null && !through.isAfter(current))      // strictly advancing only
        return SealResult.alreadySealed(current);          // idempotent / 409 on regress (SEALED-MONOTONIC-001)
    period.advanceSealedThrough(through);                   // sole mutator; DB CHECK (new > old) backstops
    return SealResult.sealed(through);
}
@Transactional
public void reopen(LocalDate to, Principal actor, String justification) {
    authz.requirePrivilege(actor, "PERIOD_REOPEN");
    auditLog.record(new PeriodReopened(period.id(), to, actor.id(), justification, clock.now())); // audited
    period.reopenTo(to);
}
// DB backstop:  ALTER TABLE period ADD CONSTRAINT seal_monotonic CHECK (...) / a guarded UPDATE ... WHERE sealed_through < :through
```

Verification: review-tier. Monotonicity is a state-invariant property with no compile-time signal — a plain setter compiles and works until a backward close or silent reopen lets a late write into a filed period. Verify by review against `specs/sealed-period-l0.yaml#SEALED-MONOTONIC-001`: the watermark only advances (backward/equal rejected); re-close is idempotent; reopen is privileged and audited; a DB CHECK / guarded UPDATE backstops the monotonicity. When a fork-receiver wires a real IT (a backward close → 409; reopen writes an audit row), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [PostgreSQL — Constraints](https://www.postgresql.org/docs/current/ddl-constraints.html)
