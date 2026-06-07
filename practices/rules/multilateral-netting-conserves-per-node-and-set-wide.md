---
title: Multilateral netting must conserve BOTH per-node and set-wide — each member's net = Σ owed-to − Σ owed-by (one currency), the sum of ALL members' nets == EXACTLY 0 per currency, computed by a sole-mutator single transaction with a DB-backstopped rollup — never a per-operation conservation (this is the SET-WIDE dual of balanced-posting)
impact: HIGH
impactDescription: "A reduction that drops or double-counts one gross obligation produces net positions that do NOT sum to zero — the clearing/settlement system then settles a non-conserving set, creating or destroying money across the membership; computing the N×N reduction without a row lock lets a concurrent re-net double-persist positions; summing across currencies offsets a USD payable against a EUR receivable and conserves nothing"
tags:
  - concurrency
  - bigdecimal
  - conservation
  - netting
  - settlement
spec_ref: "specs/collection-conservation-l0.yaml#NET-SETWIDE-ZERO-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/netting/NettingService.java + backend/src/main/java/com/ax/template/authblueprint/netting/NettingRunRepository.java"
  pattern: "The reduction reads the netting run under PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) in one @Transactional, loads every gross obligation of the run exactly once, computes per-member net = Σ(amount where member is creditor) − Σ(amount where member is debtor) in BigDecimal within ONE currency partition, CROSS-CHECKS each computed net against an INDEPENDENT derivation (repository SUM queries sumOwedTo − sumOwedBy, a different code path) and REJECTS (422 NETTING_NOT_CONSERVED) on a mismatch (this catches a from/to swap, sign error, or dropped row), persists one NetPosition per member, also asserts net_total = Σ(member nets) == 0 with a DB @Check(net_total = 0) as a structural belt-and-suspenders, and flips the run to a terminal NETTED state that rejects a second reduction (409 NETTING_ALREADY_NETTED); obligations are @Column(updatable=false) append-only and cannot be added once NETTED; no cross-currency summation occurs"
upstream:
  - "https://www.bis.org/cpmi/glossary.pdf"
  - "https://cwe.mitre.org/data/definitions/362.html"
  - "https://www.postgresql.org/docs/current/explicit-locking.html"
evidence:
  - source_type: external
    citation: "BIS CPMI — Glossary of terms used in payments and market infrastructures, definition of 'multilateral netting'"
    url: "https://www.bis.org/cpmi/glossary.pdf"
    quote: "The offsetting of obligations between or among multiple participants to result in a single net position per participant."
    quoted_at: "2026-06-08"
  - source_type: external
    citation: "BIS CPMI — Glossary of terms used in payments and market infrastructures, definition of 'net credit (or debit) position'"
    url: "https://www.bis.org/cpmi/glossary.pdf"
    quote: "A participant's net credit or net debit position in a netting system is the sum of the value of all the transfers it has received up to a particular point in time less the value of all transfers it has sent."
    quoted_at: "2026-06-08"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL Documentation — 'Explicit Locking' (row-level FOR UPDATE serializes the reduction)"
    url: "https://www.postgresql.org/docs/current/explicit-locking.html"
    quote: "FOR UPDATE causes the rows retrieved by the SELECT statement to be locked as though for update. This prevents them from being locked, modified or deleted by other transactions until the current transaction ends."
    quoted_at: "2026-06-01"
---

## Multilateral netting must conserve both per-node and set-wide

**Impact: HIGH — a reduction that drops/double-counts one gross obligation yields net positions that do not sum to zero, so the settlement system moves money that was created or destroyed across the membership; an unlocked reduction double-persists under a concurrent re-net; cross-currency summation conserves nothing.**

This rule is the **set-wide dual** of `value-transfer-must-be-balanced` (balanced-posting). Balanced-posting conserves ONE operation: the legs of a single `operation_id` net to zero, and a non-zero per-member net is a bug to be **rejected**. **Multilateral netting is the opposite shape**: it collapses an N×N set of directed gross obligations into one signed net **per member** — and the per-member net is the deliberate *output* (a net credit or net debit position). The conservation moves up a level: it is the whole **set** that must sum to zero.

BIS CPMI defines it exactly: multilateral netting is *"the offsetting of obligations between or among multiple participants to result in a single net position per participant,"* where a participant's net position is *"the sum of the value of all the transfers it has received … less the value of all transfers it has sent."* Two invariants follow:

```text
per-node:  net_p = Σ(amount owed TO p) − Σ(amount owed BY p)     // one currency partition
set-wide:  Σ over all members p of net_p == 0                     // every payable is someone's receivable
```

The set-wide identity is not an accident — it is double-entry at the population level: every gross obligation is simultaneously one node's payable (−) and another's receivable (+), so summing all signed nets cancels to zero. If the reduction ever produces a non-zero set total, an obligation was dropped, double-counted, or cross-currency-summed — and the downstream settlement would create or destroy money.

Three defects recur, and one rule closes them.

**Defect 1 — trusting set-wide Σ==0 to prove per-node correctness.** The set-wide sum is a *structural identity* of a two-legged reduction: every obligation contributes `+amount` to the creditor and `−amount` to the debtor, so `Σ` is **always** 0 — regardless of correctness. A creditor/debtor **swap** (`merge(from,+); merge(to,−)`), a **sign error**, or a **whole-row drop** (both legs vanish together) all keep `Σ == 0`, so a `net_total` rollup with `@Check (net_total = 0)` catches *only* a one-sided/asymmetric emit — it does **not** detect the per-node bugs that actually move money the wrong way. The real guard is an **independent per-node cross-check**: re-derive each member's net from a *different code path* — repository `SUM` queries `Σ(owed-to-member) − Σ(owed-by-member)` — and reject (422 `NETTING_NOT_CONSERVED`) if it disagrees with the reduction's output. Keep the set-wide `@Check` as belt-and-suspenders (it catches the asymmetric case), but do not claim it proves per-node correctness.

**Defect 2 — an unlocked reduction double-persists under concurrency (CWE-362).** Two concurrent `net()` calls on the same run both read it as un-netted and both compute+persist positions — doubling them. This is CWE-362: *"a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."* Take the run row under `FOR UPDATE` and make NETTED a terminal state, so the reduction runs exactly once and a second call gets `409 NETTING_ALREADY_NETTED`.

**Defect 3 — cross-currency summation.** Summing a USD payable against a EUR receivable offsets values that are not fungible. Net **only within one currency partition** (the run is single-currency; a mismatched-currency obligation is rejected) — the heterogeneous-unit discipline of the Money pattern, applied per partition.

**Incorrect — per-operation balance reflex + no lock + no set-wide check:**

```java
// ❌ trying to force each member's net to zero (balanced-posting reflex) is WRONG here —
// the per-member net is the OUTPUT; only the SET nets to zero.
public void net(UUID runId) {
    var obligations = repo.findByRunId(runId);                 // ❌ no run lock (DEFECT 2)
    for (var m : members) {
        BigDecimal net = sumOwedTo(m) - sumOwedBy(m);
        if (net.signum() != 0) throw new NotBalancedException(); // ❌ rejects the legitimate net (wrong invariant)
        save(new NetPosition(runId, m, net));                  // ❌ no Σ==0 backstop, no terminal guard
    }
}
```

**Correct — run lock + per-node nets + set-wide zero backstop + net-once terminal:**

```java
@Transactional
public NettingRun net(String runKey) {
    NettingRun run = runs.findByRunKeyForUpdate(runKey)        // ✅ SELECT ... FOR UPDATE, same tx
        .orElseThrow(NettingException::notFound);
    if (run.getStatus() != OPEN) throw NettingException.alreadyNetted();  // ✅ net-once terminal (DEFECT 2)
    List<GrossObligation> gross = obligations.findByRunId(run.getId());    // each row read once
    BigDecimal total = BigDecimal.ZERO.setScale(SCALE);
    for (String member : membersOf(gross)) {                  // one currency partition (DEFECT 3)
        BigDecimal net = owedTo(gross, member).subtract(owedBy(gross, member));  // ✅ net = received − sent
        BigDecimal indep = obligations.sumOwedTo(run.getId(), member)            // ✅ INDEPENDENT cross-check
            .subtract(obligations.sumOwedBy(run.getId(), member));               //    (different code path)
        if (net.compareTo(indep) != 0) throw NettingException.notConserved();    // ✅ catches swap/sign/drop (DEFECT 1)
        positions.save(new NetPosition(run.getId(), member, net));
        total = total.add(net);
    }
    if (total.signum() != 0) throw NettingException.notConserved();  // set-wide Σ==0 — structural belt-and-suspenders
    run.markNetted(total);                                    // sets net_total (DB @Check(net_total=0) backstop)
    return run;
}
```

`FOR UPDATE` serializes concurrent reductions (*"This prevents them from being … modified … by other transactions until the current transaction ends"*); NETTED is terminal so the reduction is one-shot. The per-member nets are signed and non-zero (the output). Per-node correctness is guarded by the **independent cross-check** (the `SUM`-query derivation is a different code path than the in-memory accumulation, so a from/to swap or dropped row is caught); the set-wide `Σ == 0` / `@Check` is a structural belt-and-suspenders (it catches an asymmetric one-sided emit, not a per-node sign/swap). A correction is a fresh run over corrected obligations, never an in-place edit (the inputs are `@Column(updatable=false)` append-only).

Verification: review-tier — confirm the reduction locks the run row, loads each obligation once, computes `net = Σreceived − Σsent` per member in `BigDecimal` within one currency, CROSS-CHECKS each net against an independent `SUM`-query derivation (rejecting 422 on mismatch — the real per-node guard), asserts `Σ member nets == 0` with a DB `@Check (net_total = 0)` structural backstop, flips the run to a terminal NETTED state rejecting a second reduction, and never sums across currencies. The canonical proof a fork-receiver writes is: build an N×N gross set, net it, assert each `net_p == Σreceived−Σsent` and `Σ all nets == 0`; plus a concurrency test that two simultaneous `net()` yield exactly one NETTED run.

Reference: [BIS CPMI — Glossary of terms used in payments and market infrastructures](https://www.bis.org/cpmi/glossary.pdf)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)

Reference: [PostgreSQL — Explicit Locking (FOR UPDATE)](https://www.postgresql.org/docs/current/explicit-locking.html)
