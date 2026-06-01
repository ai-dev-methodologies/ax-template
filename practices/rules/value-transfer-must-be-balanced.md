---
title: A value-moving operation MUST post balanced legs that net to exactly zero — conserve, never mint or destroy
impact: CRITICAL
impactDescription: "A transfer / allocation that credits the receiver but skews, drops, or wrong-signs the debit MINTS value out of nothing (positive residual) or DESTROYS it (negative residual); idempotency, optimistic locking, and atomic-claim do NOT close this — only a net-zero-sum invariant across the legs of one operation does"
tags:
  - ledger
  - double-entry
  - conservation
  - bigdecimal
  - transactional
  - value-integrity
spec_ref: "specs/balanced-posting-l0.yaml#CONSERVATION-LEGS-NET-ZERO-001"
verification:
  type: review
  source: "specs/balanced-posting-l0.yaml#CONSERVATION-LEGS-NET-ZERO-001"
  pattern: "A value-moving handler (transfer, allocation, journal posting) MUST persist ALL legs of one operation inside ONE @Transactional unit through a single sole-mutator, every leg carrying the SAME operation id, and MUST assert before commit that the signed BigDecimal leg amounts sum to exactly zero via BigDecimal::compareTo(ZERO) (NEVER equals — scale-sensitive). Reject any handler that writes a credit leg without its mirror debit, splits the legs across independent REQUIRES_NEW sub-transactions, mixes amount scales, or skips the sum==0 check — an unbalanced operation MUST be rejected with 422 UNBALANCED_POSTING, never partially committed. A structural backstop (DB deferred-constraint trigger / per-operation rollup CHECK(net_amount=0), or a service-level pre-commit re-sum) MUST exist (CONSERVATION-BACKSTOP-001). This is a value-conservation correctness property with no compile-time signal, so it is verified by review against the spec, not by a static @Tag test."
upstream:
  - "https://martinfowler.com/eaaDev/AccountingTransaction.html"
  - "https://www.postgresql.org/docs/current/ddl-constraints.html"
evidence:
  - source_type: external
    citation: "Martin Fowler — Accounting Transaction (Patterns of Enterprise Application Architecture, eaaDev / Accounting Patterns)"
    url: "https://martinfowler.com/eaaDev/AccountingTransaction.html"
    quote: "A multi-legged transaction allows any number of entries, but with still the overall rule that all the entries must sum to zero, thus conserving money."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Martin Fowler — Accounting Transaction (intent / summary)"
    url: "https://martinfowler.com/eaaDev/AccountingTransaction.html"
    quote: "Link two (or more) entries together so that the total of all entries in a transaction is zero"
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL Documentation — 5.5.1 Check Constraints"
    url: "https://www.postgresql.org/docs/current/ddl-constraints.html"
    quote: "A check constraint is the most generic constraint type. It allows you to specify that the value in a certain column must satisfy a Boolean (truth-value) expression."
    quoted_at: "2026-06-01"
decided_at: "2026-06-01"
---

## A value-moving operation MUST post balanced legs that net to exactly zero — conserve, never mint or destroy

**Impact: CRITICAL — A value-moving operation (a transfer between accounts, an inventory move between warehouses, a points/voucher transfer between users, an allocation drawn from a pool) posts two or more signed legs. If the handler writes one leg but skews, drops, or wrong-signs its mirror — on a partial failure, a rounding split that does not re-add, a wrong sign, a forgotten leg under a refactor — the operation MINTS value out of nothing (positive residual) or DESTROYS it (negative residual). Reconciliation breaks, an audit invariant fails, and the corruption surfaces months later as a balance that does not foot. Idempotency does not catch it (the operation is not a retry), optimistic locking does not catch it (no concurrent writer), and atomic-claim does not catch it (no shared counter raced) — only a net-zero-sum invariant across the legs of one operation does.**

This is the double-entry / conservation-of-quantity principle, the same one physicists apply to energy: you cannot create value, you can only move it around. Martin Fowler's Accounting Transaction pattern states the rule directly — *all the entries must sum to zero, thus conserving money*. The catalog's neighbouring primitives do NOT cover this axis:

- **bounded-capacity-claim (`#CLAIM-ATOMIC-001`)** serializes claimants racing ONE one-sided shared counter — it prevents OVER-allocation of a single counter. It says nothing about whether a draw on the source equals the grant to the destination.
- **payment refund-cap** is a single-axis cumulative ceiling (`refunded <= captured`) — one monotone bound on one axis. Balanced-posting is a two-sided net-zero conservation across the legs of ONE operation, not a one-axis monotone bound.

There are three load-bearing requirements, and they compose with — but are distinct from — `persistence-state-machine-atomic` and `transaction-propagation-requires-new.md`.

**1. All legs in ONE transaction, ONE operation id.** Every leg of the operation is persisted inside a single `@Transactional` unit through one sole-mutator, each leg carrying the same `operation_id`. The legs MUST NOT be split across independent `REQUIRES_NEW` sub-transactions — a crash between them would leave a half-committed, unbalanced posting. This is exactly why `transaction-propagation-requires-new.md` matters here: the multi-leg write shares ONE transaction.

**2. Signed amounts sum to exactly zero, by `compareTo`.** All leg amounts share one signed `BigDecimal` representation at one fixed scale (the ISO-4217 currency scale, or the unit scale for non-money quantities). Debits are negative, credits positive (consistently). The pre-commit check is `sum.compareTo(BigDecimal.ZERO) == 0` — **never** `equals`, which is scale-sensitive (`new BigDecimal("0.00").equals(BigDecimal.ZERO)` is `false`, but `compareTo` is `0`).

**3. Reject unbalanced before commit.** An operation whose legs do not net to zero is rejected with `422 UNBALANCED_POSTING` (RFC 9457, `imbalance` carrying the residual) — never silently persisted, never partially committed.

**Incorrect — credits the receiver, skews the debit; the operation mints value out of nothing:**

```java
@Transactional
public Transfer transfer(long fromId, long toId, BigDecimal amount) {
    // VIOLATION 1: a fee is netted out of the debit but NOT mirrored by a
    // matching fee leg — the two legs no longer sum to zero.
    BigDecimal fee = amount.multiply(new BigDecimal("0.01"));
    ledger.save(new Leg(fromId, amount.add(fee).negate()));  // debit -(amount+fee)
    ledger.save(new Leg(toId,   amount));                     // credit +amount
    // residual = -fee  →  value DESTROYED; no balance check, commits anyway.
    // VIOLATION 2 (latent): had these two saves been split across two
    // REQUIRES_NEW methods, a crash between them commits ONE leg alone.
    return new Transfer(fromId, toId, amount);
}
```

**Correct — every leg shares one operation id; signed legs are asserted net-zero via compareTo before commit; unbalanced is rejected 422:**

```java
@Transactional
public Transfer post(PostingOperation op) {           // one sole-mutator, one txn
    List<Leg> legs = op.legs();                       // e.g. debit + credit + fee + fee-income
    BigDecimal residual = legs.stream()
        .map(Leg::signedAmount)                       // all at the ISO-4217 / unit scale
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (residual.compareTo(BigDecimal.ZERO) != 0) {   // compareTo, NOT equals
        postingMetrics.rejectedUnbalanced(op.type());  // CONSERVATION-OBSERVABILITY-001
        throw new UnbalancedPostingException(residual); // → 422 UNBALANCED_POSTING
    }
    long operationId = ledger.nextOperationId();
    legs.forEach(leg -> ledger.save(leg.withOperationId(operationId))); // SAME op id
    postingMetrics.balanced(op.type());
    return Transfer.of(op, operationId);
}
// Structural backstop (CONSERVATION-BACKSTOP-001), independent of this code:
//   a deferred-constraint trigger raising on  SUM(signed_amount) <> 0
//   GROUP BY operation_id,  OR a per-operation rollup row carrying
//   CHECK (net_amount = 0)  — so even a path that bypasses this service
//   check cannot persist an asymmetric posting.
```

**Generic across domains — this is value conservation, not accounting-only.** Inventory transfer: units leaving warehouse A (negative leg) equal units entering warehouse B (positive leg). Points / voucher / loyalty transfer: the sender's debit equals the receiver's credit. Energy / credit / quota allocation: the amount drawn from the pool equals the sum granted to consumers. Classic double-entry: debits equal credits. In every case the legs of one operation sum to zero or the system has minted or destroyed quantity it had no right to.

**Orthogonal to the catalog's other conservation-adjacent rules — ship the ones the operation needs.** A transfer that draws on a bounded source wants BOTH atomic-claim (`#CLAIM-ATOMIC-001`, so the source is not over-drawn under a race) AND balanced-posting (so the drawn amount equals the granted). A multi-currency posting wants `lang-bigdecimal-for-money.md` (exact decimal arithmetic) under it. None of these substitutes for the net-zero invariant.

Verification: review-tier. Value conservation is a correctness property of the posting logic with no compile-time signal — a single-path test on a happy transfer passes even on a handler that will skew a leg under a future refactor, and the corruption only shows up when a balance fails to foot. Verify by review against `specs/balanced-posting-l0.yaml#CONSERVATION-LEGS-NET-ZERO-001`, prove it with a positive sum-of-legs invariant test (every persisted operation: `sum(legs.signedAmount).compareTo(ZERO) == 0`) plus a negative test (a deliberately skewed leg → 422 `UNBALANCED_POSTING`, no rows committed), and require the structural backstop of `#CONSERVATION-BACKSTOP-001`. When a fork-receiver wires a real `@Tag("CONSERVATION-LEGS-NET-ZERO-001")` invariant IT, this rule's verification block may be upgraded from review to gradle_task+tag.

Reference: [Martin Fowler — Accounting Transaction (entries must sum to zero, conserving money)](https://martinfowler.com/eaaDev/AccountingTransaction.html)

Reference: [PostgreSQL — Constraints (CHECK constraint enforcing a Boolean condition on stored rows)](https://www.postgresql.org/docs/current/ddl-constraints.html)
