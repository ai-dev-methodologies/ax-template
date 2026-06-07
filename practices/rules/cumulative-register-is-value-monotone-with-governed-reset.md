---
title: A cumulative register (meter / odometer / counter) must be VALUE-monotone — an appended read is ≥ the anchor and consumption is delta = curr − prior computed under the row lock — and a decrease is rejected (422) unless it is a governed ROLLOVER (wrapped-delta) or EXCHANGE (baseline reset); never a silent negative delta
impact: HIGH
impactDescription: "Accepting a read below the anchor posts a NEGATIVE delta that silently erases billed consumption; computing delta against a stale anchor under concurrency double-counts or loses consumption (CWE-362); treating an odometer wrap as a normal decrease either rejects a legitimate read or fabricates a huge negative — each corrupts the measured quantity the whole system bills on"
tags:
  - concurrency
  - bigdecimal
  - conservation
  - monotonic
  - metering
  - state-machine
spec_ref: "specs/monotone-register-l0.yaml#REG-MONOTONE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/register/RegisterRepository.java + backend/src/main/java/com/ax/template/authblueprint/register/RegisterService.java"
  pattern: "An append reads the register row under PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) in the same transaction; a NORMAL read requires read >= anchor (else 422 REGISTER_NOT_MONOTONE) and records delta = read - anchor; a ROLLOVER read (governed, reason-required) records delta = (modulus - anchor) + read and never a negative; an EXCHANGE read (governed, reason-required) records delta = 0 and resets the anchor to the new opening read; the anchor advances to the read; appended reads are @Column(updatable=false) append-only; a @Check (delta >= 0) and (anchor >= 0 AND anchor < modulus) backstop the invariants under ddl-auto; no read-the-anchor-then-write-in-a-separate-statement appears on any append path"
upstream:
  - "https://www.rfc-editor.org/rfc/rfc2578.txt"
  - "https://www.postgresql.org/docs/current/explicit-locking.html"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "IETF RFC 2578 — Structure of Management Information Version 2 (SMIv2), §7.1.6 Counter32 (the canonical monotone-counter-with-wrap definition)"
    url: "https://www.rfc-editor.org/rfc/rfc2578.txt"
    quote: "The Counter32 type represents a non-negative integer which monotonically increases until it reaches a maximum value of 2^32-1 (4294967295 decimal), when it wraps around and starts increasing again from zero."
    quoted_at: "2026-06-08"
  - source_type: external
    citation: "PostgreSQL Documentation — 'Explicit Locking' (row-level FOR UPDATE serializes concurrent appenders on one register row)"
    url: "https://www.postgresql.org/docs/current/explicit-locking.html"
    quote: "FOR UPDATE causes the rows retrieved by the SELECT statement to be locked as though for update. This prevents them from being locked, modified or deleted by other transactions until the current transaction ends."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A cumulative register must be value-monotone with delta = curr − prior, and a decrease is a governed exception, never a silent negative

**Impact: HIGH — accepting a read below the anchor posts a negative delta that erases billed consumption; a stale-anchor delta under concurrency double-counts or loses it; mishandling an odometer wrap rejects a real read or fabricates a huge negative.**

A *cumulative register* — an electricity/gas/water meter, a vehicle odometer, a cumulative byte/request counter, an SNMP-style counter — is **value-monotone**: its reading only ever increases. The observed quantity between two reads is the **delta**, and that is what the system bills or charts:

```text
delta = read − anchor          // anchor = the register's latest reading
anchor = read                  // advance (NORMAL read), read MUST be ≥ anchor
// invariant: every NORMAL delta ≥ 0 ; Σ deltas (totalConsumption) is the billed quantity.
// Σ deltas == (final − initial) holds ONLY across a NORMAL-only run; a governed ROLLOVER/EXCHANGE
// resets the anchor, so NEVER reconcile or bill on (final − initial) — always on Σ deltas.
```

This is NOT the catalog's existing monotonicity. `monotonic-event-ingest` guards **event TIME** against a watermark and de-dups to one row; `accumulator-consume` **floors at zero** and is bidirectional (a balance); `shared-counter-claim` refuses past a **cap**. None encodes a value-monotone odometer with **wrapped-delta math** and a **governed reset**. RFC 2578 gives the canonical shape: a Counter *"monotonically increases until it reaches a maximum value … when it wraps around and starts increasing again from zero."*

Three defects recur, and one rule closes them.

**Defect 1 — accepting a decrease as a normal read (negative delta).** If a read below the anchor is stored with `delta = read − anchor` (a negative), the period's billed consumption is silently erased or reversed. A NORMAL read below the anchor must be **rejected (422)**, not posted.

**Defect 2 — a stale-anchor delta under concurrency (CWE-362).** Reading the anchor in one statement and writing the new reading in a later statement is a race: two interval blocks for the same meter both read `anchor = 1000`, both compute their delta against `1000`, and one increment is lost (or double-counted). This is CWE-362: *"a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."* The append must take the register row under `FOR UPDATE` so concurrent appends serialize and each delta is computed against the **freshly-committed** anchor.

**Defect 3 — treating an odometer wrap as a decrease.** When the physical register wraps past its modulus (a 5-digit meter going 99998 → 00001), the new read is *below* the anchor but consumption is *positive*. This is a **governed ROLLOVER**: `delta = (modulus − anchor) + read`, recorded with a reason — never a negative, never a rejection. A device swap is a **governed EXCHANGE**: the baseline resets to the new opening read with `delta = 0` (the seam fabricates no consumption). Both are reason-gated exceptions to the monotone check, not silent acceptances.

**Incorrect — read-then-write (races) + decrease stored as negative + no wrap concept:**

```java
public BigDecimal recordRead(String key, BigDecimal read) {
    Register r = repo.findByScopeKey(key).orElseThrow();   // ❌ plain read, no lock (DEFECT 2)
    BigDecimal delta = read.subtract(r.getAnchor());        // ❌ DEFECT 1/3: negative if read < anchor
    r.setAnchor(read);                                      // ❌ another tx moved the anchor in between
    repo.save(r);
    return delta;                                           // ❌ a wrapped odometer bills a huge negative
}
```

**Correct — row lock + monotone NORMAL + governed ROLLOVER/EXCHANGE, delta never negative:**

```java
@Transactional
public RegisterReading append(String key, ReadingKind kind, BigDecimal read, String reason) {
    Register r = repo.findByScopeKeyForUpdate(key)             // ✅ SELECT ... FOR UPDATE, same tx
        .orElseThrow(RegisterException::notFound);
    BigDecimal anchor = r.getAnchor();
    BigDecimal delta = switch (kind) {
        case NORMAL -> {
            if (read.compareTo(anchor) < 0)                    // ✅ DEFECT 1: reject, never negative
                throw RegisterException.notMonotone();
            yield read.subtract(anchor);                       // delta ≥ 0 (CHECK delta >= 0 backstop)
        }
        case ROLLOVER -> {                                     // ✅ DEFECT 3: wrapped-delta (RFC 2578)
            requireReason(reason);
            yield r.getModulus().subtract(anchor).add(read);   // (modulus − anchor) + read, ≥ 0
        }
        case EXCHANGE -> {                                     // ✅ governed baseline reset, downward only
            requireReason(reason);
            if (read.compareTo(anchor) >= 0)                   // an upward EXCHANGE would erase consumption
                throw RegisterException.invalidReading();
            yield BigDecimal.ZERO;                             // delta 0 — the seam fabricates no consumption
        }
    };
    long seq = readings.maxSequence(r.getId()) + 1;
    RegisterReading row = readings.save(new RegisterReading(/* immutable */ r.getId(), kind, read, anchor,
        delta, seq, reason, Instant.now(clock)));
    r.advanceAnchor(read);                                     // ✅ anchor := read, under the lock
    return row;
}
```

`FOR UPDATE` serializes concurrent appenders on the one register row (*"This prevents them from being … modified … by other transactions until the current transaction ends"*), so each delta is computed against the true prior anchor and, across a NORMAL-only run, `Σ deltas == final − initial` holds. The NORMAL path can never store a negative; the only decreases are the two reason-gated governed exceptions, each of which requires the new read to be **below** the anchor (a baseline reset is downward — an *upward* EXCHANGE with `delta = 0` would silently erase the consumption between the old anchor and the new read). Σ deltas (total consumption) is the billing quantity and is robust across resets; never reconcile on `final − initial` (a governed reset breaks it). Appended reads are `@Column(updatable=false)` append-only, so the history reconstructs the cumulative total and every interval.

Verification: review-tier — confirm every append locks the register row (`@Lock(PESSIMISTIC_WRITE)`), the NORMAL path rejects `read < anchor` (422) and stores `delta = read − anchor`, ROLLOVER stores `(modulus − anchor) + read` with a reason and requires `read < anchor`, EXCHANGE stores `0` with a reason and requires `read < anchor` (a downward baseline reset, so an upward swap cannot drop consumption), reads are immutable, and `@Check (delta >= 0)` + `(anchor >= 0 AND anchor < modulus)` are declared. The canonical proof a fork-receiver writes is a concurrency test over a NORMAL-only run: N concurrent appends of distinct increasing values, asserting `Σ committed deltas == final anchor − initial anchor`.

Reference: [IETF RFC 2578 — SMIv2 (Counter32)](https://www.rfc-editor.org/rfc/rfc2578.txt)

Reference: [PostgreSQL — Explicit Locking (FOR UPDATE)](https://www.postgresql.org/docs/current/explicit-locking.html)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
