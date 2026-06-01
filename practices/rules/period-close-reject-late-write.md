---
title: A write into a sealed/closed aggregation period MUST be rejected or rerouted — never silently mutate a finalized period
impact: HIGH
impactDescription: "Once a period is closed and a statement/report has been issued from it, a back-dated write that silently mutates the sealed window makes already-distributed reports irreproducible and breaks reconciliation — the corruption surfaces months later when the period total no longer matches the issued statement"
tags:
  - period-close
  - sealed-period
  - value-conservation
  - watermark
  - monotonic
  - audit
spec_ref: "specs/sealed-period-l0.yaml#SEALED-REJECT-LATE-001"
verification:
  type: review
  source: "specs/sealed-period-l0.yaml#SEALED-REJECT-LATE-001"
  pattern: "the write path reads a server-owned sealed_through watermark inside the same transaction that applies the write, compares the server-resolved effective date against it, and for effectiveDate <= sealed_through either rejects with 422/409 PERIOD_CLOSED (RFC 9457) or deterministically reroutes to the next open period — the sealed aggregate's persisted total is provably unchanged on a late write"
upstream:
  - "https://en.wikipedia.org/wiki/Trial_balance"
  - "https://cwe.mitre.org/data/definitions/367.html"
evidence:
  - source_type: external
    citation: "Wikipedia — Trial balance (closing the books / period close convention)"
    url: "https://en.wikipedia.org/wiki/Trial_balance"
    quote: "The act of \"closing the books\" refers to zeroing out all the revenue and expense amounts at the end of an accounting period (typically a fiscal year) and adding the difference to the retained earnings account."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Wikipedia — Closing entries (period-end finalization for the next period)"
    url: "https://en.wikipedia.org/wiki/Closing_entries"
    quote: "Closing entries are journal entries made at the end of an accounting period to transfer temporary accounts to permanent accounts."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-367 — Time-of-check Time-of-use (TOCTOU) Race Condition"
    url: "https://cwe.mitre.org/data/definitions/367.html"
    quote: "The product checks the state of a resource before using that resource, but the resource's state can change between the check and the use in a way that invalidates the results of the check."
    quoted_at: "2026-06-01"
---

## A write into a sealed/closed aggregation period MUST be rejected or rerouted — never silently mutate a finalized period

**Impact: HIGH — a back-dated write into a closed period invalidates already-issued statements and breaks reconciliation**

Periodic systems close a window and report on it: an accounting period is closed and a financial statement is issued; a billing cycle is closed and invoices are sent; a payroll run is finalized and pay slips are distributed; an end-of-day cutoff is taken and a daily settlement file is produced; an inventory count is finalized and a stock report is signed off. The double-entry convention calls this "closing the books" — at the end of a period the figures are zeroed out and rolled forward, and the closing entries finalize that period so the next one can start clean. Once a statement has been issued from a closed window, that window's total is a fact that downstream consumers have already trusted. A write whose effective date falls *inside* that closed window — a forgotten expense back-dated to last month, a correction stamped with an old value date, a clock-skewed event — that silently mutates the sealed aggregate makes the issued report irreproducible: re-running the period total no longer matches the statement that went out the door, and reconciliation breaks. The break is silent at write time and only surfaces in an audit months later.

The fix is a sealed-through watermark on the aggregate plus a transactional gate on every write. The aggregate persists a server-owned `sealed_through` instant — the inclusive upper bound of every period already closed. Every write carries a **server-resolved** effective/value date (never a client-supplied date the caller could back-date — see PRACTICES-TIME-001: server clock + server-stored window decide the outcome). Inside the same transaction that would apply the write, the service reads the watermark and compares: a write strictly *after* the watermark posts normally; a write *at or before* the watermark targets a sealed window and MUST NOT be applied in place. It is either **rejected** (422 / 409 + RFC 9457 `type=urn:problem:period-closed`, `title=PERIOD_CLOSED`, echoing the offending effective date and the watermark) or **deterministically rerouted** to the next open period (forward-dated correction) when the recipe opts into `sealed_late_write_policy=reroute-next-open`. Reading the watermark and applying the write under one snapshot is not optional: per CWE-367, a check-then-act split lets a close racing a write slip a late write into a just-sealed period (time-of-check ≠ time-of-use).

This is **distinct** from a per-record deadline (PRACTICES-TIME-001 guards *one row's* `expiresAt`), from optimistic locking (ETag/If-Match guards a concurrent read-modify-write on *one row*), and from monotonic event ingest (rejects a stale event that would roll *one projected row* backward). Here the gate is a **period boundary**, and the rejected write is one that would mutate an aggregate that has already been closed and reported on. The close itself is one-way monotonic and reopening is an explicit, audited action (SEALED-MONOTONIC-001) — there is no silent path that un-seals a period.

**Incorrect — last-write-wins by effective date; a back-dated entry silently mutates a closed, already-reported period:**

```java
@PostMapping("/ledger/entries")
public ResponseEntity<Void> postEntry(@RequestBody LedgerEntry in) {
    // effective date taken straight from the client body — forgeable, back-datable
    Period p = periods.findContaining(in.effectiveDate()).orElseThrow();
    p.addAmount(in.amount());        // ❌ no sealed check: posts into a CLOSED period
    periods.save(p);                 // ❌ the issued statement for p no longer reproduces
    return ResponseEntity.ok().build();
}
```

**Correct — server-resolved effective date compared to the sealed-through watermark inside one transaction; late write rejected:**

```java
@Transactional
public PostResult postEntry(LedgerCommand cmd) {
    // 1. effective date is SERVER-resolved (clock-injected), never the raw client value
    LocalDate effectiveDate = effectiveDateResolver.resolve(cmd, clock);

    // 2. read the server-owned watermark INSIDE this txn (same snapshot — CWE-367)
    LocalDate sealedThrough = ledgerAggregate.sealedThrough();   // inclusive close watermark

    // 3. at-or-before the watermark => targets a sealed, already-reported window
    if (!effectiveDate.isAfter(sealedThrough)) {
        lateWrites.increment("ledger", "rejected");
        throw new PeriodClosedException(effectiveDate, sealedThrough); // -> 422 PERIOD_CLOSED (RFC 9457)
        // (or, when sealed_late_write_policy=reroute-next-open:
        //   effectiveDate = ledgerAggregate.firstOpenDayAfter(sealedThrough);
        //   lateWrites.increment("ledger", "rerouted");  // forward-dated correction)
    }

    Period p = periods.findContaining(effectiveDate).orElseThrow();
    p.addAmount(cmd.amount());        // ✅ only ever mutates an OPEN period
    periods.save(p);
    return PostResult.applied(p.id());
}
```

The keystone invariant a fork-receiver proves: seal period P (watermark = end-of-P), POST a write with an effective date inside P, assert `422 PERIOD_CLOSED` (with the watermark echoed) **and** assert the sealed aggregate's persisted total is byte-for-byte unchanged. A closed period is a finalized fact; the only correct disposition for a late write is reject-or-reroute, never in-place mutation.

Verification: review the write path against `specs/sealed-period-l0.yaml#SEALED-REJECT-LATE-001` — confirm the server-resolved effective date is compared to a server-owned `sealed_through` watermark inside the apply transaction, and that an at-or-before-watermark write mutates nothing (rejected with PERIOD_CLOSED or forward-routed). Cross-check that the effective date is server-resolved per PRACTICES-TIME-001 (no client-supplied back-dating) and that close/reopen monotonicity holds per SEALED-MONOTONIC-001.

Reference: [Wikipedia — Trial balance (closing the books)](https://en.wikipedia.org/wiki/Trial_balance)

Reference: [Wikipedia — Closing entries](https://en.wikipedia.org/wiki/Closing_entries)

Reference: [CWE-367 — Time-of-check Time-of-use (TOCTOU) Race Condition](https://cwe.mitre.org/data/definitions/367.html)
