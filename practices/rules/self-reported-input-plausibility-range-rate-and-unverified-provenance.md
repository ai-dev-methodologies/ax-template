---
title: A self-reported, server-unverifiable value (claimed location, self-meter-read, odometer entry, attestation) must NOT be trusted as authoritative — it must pass a RANGE bound and a RATE-OF-CHANGE limit vs the prior accepted reading, be persisted as SELF_REPORTED_UNVERIFIED with its plausibility basis (never CONFIRMED), and an implausible submission must be rejected (422) AND recorded as an auditable attempt — never silently dropped
impact: HIGH
impactDescription: "A self-reported value the server cannot verify, if trusted as authoritative, propagates fraud or error straight into billing/settlement/safety (a spoofed GPS, a rolled-back odometer, an impossible meter spike); if silently dropped, the implausible attempt — a fraud/calibration signal — vanishes with no audit. The two real failures are (a) accept it as server-confirmed and (b) reject it without a record. The correct posture is a deterministic RANGE + RATE-OF-CHANGE plausibility gate that admits the value ONLY as explicitly SELF_REPORTED_UNVERIFIED with its basis, and records every rejected attempt — CWE-20 / CWE-1284: input whose required quantity properties (range, rate) are not validated"
tags:
  - input-validation
  - audit
  - concurrency
  - data-quality
  - governance
spec_ref: "specs/self-reported-input-plausibility-l0.yaml#PLAUSIBILITY-RANGE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/inputplausibility/PlausibilityService.java + backend/src/main/java/com/ax/template/authblueprint/inputplausibility/PlausibilityChannel.java + backend/src/main/java/com/ax/template/authblueprint/inputplausibility/PlausibilityReading.java"
  pattern: "A submission against a PlausibilityChannel is plausibility-gated under the channel's PESSIMISTIC_WRITE row lock: the RANGE gate (min <= value <= max) runs first and a breach is 422 IMPLAUSIBLE_RANGE; when a prior accepted reading exists the RATE gate compares |value - priorValue| against maxDeltaPerSecond * elapsedSeconds (a zero-elapsed non-zero jump is infinite rate) and a breach is 422 IMPLAUSIBLE_RATE; a passing submission appends an immutable PlausibilityReading marked SELF_REPORTED_UNVERIFIED (the enum has no CONFIRMED constant) carrying its basis (checksRan, hadPrior/priorValue, elapsedSeconds, computedRate) and advances the channel's prior pointer; a failing gate records an immutable RejectedAttempt BEFORE the 422 and leaves the accepted state untouched; NO delete path exists"
upstream:
  - "https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html"
  - "https://cwe.mitre.org/data/definitions/20.html"
  - "https://cwe.mitre.org/data/definitions/1284.html"
evidence:
  - source_type: external
    citation: "OWASP Input Validation Cheat Sheet — syntactic vs SEMANTIC validation; a plausibility bound on a self-reported value is semantic validation (correctness in the specific business context), not a format check"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html"
    quote: "Input validation should be applied at both syntactic and semantic levels: Syntactic validation should enforce correct syntax of structured fields (e.g. SSN, date, currency symbol). Semantic validation should enforce correctness of their values in the specific business context."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-20: Improper Input Validation — MITRE; why a self-reported value the server cannot verify must not be trusted as processed safely"
    url: "https://cwe.mitre.org/data/definitions/20.html"
    quote: "The product receives input or data, but it does not validate or incorrectly validates that the input has the properties that are required to process the data safely and correctly."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-1284: Improper Validation of Specified Quantity in Input — MITRE; a self-reported quantity/reading whose range or rate of change is not validated"
    url: "https://cwe.mitre.org/data/definitions/1284.html"
    quote: "The product receives input that is expected to specify a quantity (such as size or length), but it does not validate or incorrectly validates that the quantity has the required properties."
    quoted_at: "2026-06-23"
---

## A self-reported, server-unverifiable value is plausibility-gated and recorded as unverified — never trusted as authoritative, never silently dropped

**Impact: HIGH — a trusted-as-authoritative self-report propagates fraud/error into billing/settlement/safety; a silently-dropped implausible attempt loses a fraud/calibration signal (CWE-20 / CWE-1284).**

A *self-reported* value is one the server **cannot independently verify** against an authoritative source: a claimed GPS location, a self-declared utility meter reading, a self-reported odometer or quantity, a self-attested task completion. Every self-service intake faces it, and the two intuitive answers are both wrong: *trusting* it as authoritative lets a spoofed coordinate or a rolled-back odometer flow straight into a charge or a safety decision; *silently discarding* an implausible one throws away exactly the signal a fraud/calibration system wants. OWASP frames the right move as *semantic* validation — *"Semantic validation should enforce correctness of their values in the specific business context"* — which is precisely what a plausibility bound is. The value is admitted, but only as explicitly unverified, with its basis recorded:

```text
submit(channel, value):  RANGE gate   — min <= value <= max               else 422 IMPLAUSIBLE_RANGE
                         RATE gate    — |value - prior| <= maxRate * elapsed  (prior only) else 422 IMPLAUSIBLE_RATE
                         accept       — append PlausibilityReading SELF_REPORTED_UNVERIFIED + basis;
                                        advance the channel prior pointer
                         reject       — record an immutable RejectedAttempt (reason + basis) BEFORE the 422
locks:                   the channel row, PESSIMISTIC_WRITE — concurrent submits serialize the read-prior/append
```

**1. RANGE — a semantic plausibility bound (PLAUSIBILITY-RANGE-001).** The channel pins a configured `[min, max]`; a value outside it is `422 IMPLAUSIBLE_RANGE`. A syntactically valid number that is physically implausible is still rejected.

**2. RATE-OF-CHANGE — the jump vs the prior accepted value over elapsed time (PLAUSIBILITY-RATE-001).** When a prior accepted reading exists, `|value - prior| / elapsedSeconds` must not exceed the channel's `maxDeltaPerSecond` — this is what catches a teleport, an odometer rollback, an impossible spike. A zero-elapsed change of a non-zero amount is an infinite rate and is rejected. The first reading (no prior) is range-checked only.

**3. UNVERIFIED provenance + recorded rejection (PLAUSIBILITY-PROVENANCE/REJECT-001).** An accepted reading is persisted as `SELF_REPORTED_UNVERIFIED` (the enum has *no* CONFIRMED constant) with its basis, so a downstream consumer always knows the origin is a self-report. An implausible submission is recorded as an immutable `RejectedAttempt` before the 422 — never silently dropped.

**Incorrect — trust the self-report as authoritative, or drop the implausible one silently:**

```java
public void recordReading(UUID channelId, BigDecimal value) {
    Channel c = repo.findById(channelId).orElseThrow();    // ❌ no row lock — two submits race the rate gate
    if (value.compareTo(c.getMax()) > 0) return;           // ❌ silent drop — the implausible attempt vanishes
    c.setLastReading(value);                               // ❌ stored as fact — no UNVERIFIED status, no basis,
    repo.save(c);                                          //    no rate-of-change check vs the prior
}
```

**Correct — plausibility-gate under the channel lock, persist as SELF_REPORTED_UNVERIFIED with basis, record rejections:**

```java
@Transactional
public PlausibilityReading submit(UUID channelId, BigDecimal reportedValue, String actor) {
    PlausibilityChannel c = channels.findByIdForUpdate(channelId)        // ✅ PESSIMISTIC_WRITE row lock
        .orElseThrow(PlausibilityException::notFound);
    Instant now = Instant.now(clock);
    boolean hadPrior = c.hasPrior();
    long elapsedSeconds = hadPrior ? elapsedSeconds(c.getPriorAt(), now) : 0L;

    if (!c.inRange(reportedValue)) {                                     // ✅ RANGE gate (semantic plausibility)
        recordRejection(c, reportedValue, RejectReason.IMPLAUSIBLE_RANGE,
            hadPrior ? c.getPriorValue() : null, elapsedSeconds, null, actor, now);  // ✅ recorded BEFORE 422
        throw PlausibilityException.implausibleRange();
    }
    BigDecimal computedRate = null;
    String checksRan = CHECKS_RANGE;
    if (hadPrior) {                                                      // ✅ RATE gate only with a prior basis
        checksRan = CHECKS_RANGE_RATE;
        BigDecimal delta = reportedValue.subtract(c.getPriorValue()).abs();
        boolean rateExceeded = exceedsRate(delta, elapsedSeconds, c.getMaxDeltaPerSecond());
        computedRate = (elapsedSeconds == 0L) ? null
            : delta.divide(BigDecimal.valueOf(elapsedSeconds), RATE_MC);
        if (rateExceeded) {
            recordRejection(c, reportedValue, RejectReason.IMPLAUSIBLE_RATE,
                c.getPriorValue(), elapsedSeconds, computedRate, actor, now);
            throw PlausibilityException.implausibleRate();
        }
    }
    PlausibilityReading reading = members.persist(new PlausibilityReading(UUID.randomUUID(),
        c.getId(), reportedValue, checksRan, hadPrior, hadPrior ? c.getPriorValue() : null,
        elapsedSeconds, computedRate, actor, now));     // ✅ SELF_REPORTED_UNVERIFIED + basis
    c.recordAccepted(reportedValue, now);               // ✅ advance the prior pointer (accepted only)
    return reading;
}
```

The channel-row PESSIMISTIC_WRITE lock serializes the read-prior / evaluate-rate / append-reading sequence so two concurrent submits cannot both read the same prior and both append (CWE-362). The rate gate is compared as `|delta| > max * elapsed` rather than dividing, so a zero-elapsed jump is handled without a divide-by-zero. `PlausibilityReading` and `RejectedAttempt` rows are `@AggregateMember` of `PlausibilityChannel` — root-JPQL reads, `common/MemberWriter` writes; no delete path exists.

Verification: review-tier — confirm the RANGE gate runs first and a breach is 422 IMPLAUSIBLE_RANGE, the RATE gate runs only with a prior and a breach is 422 IMPLAUSIBLE_RATE, an accepted reading is SELF_REPORTED_UNVERIFIED with its basis (and the enum has no CONFIRMED constant), every rejection is recorded as an immutable attempt before the 422, and the submit path takes the channel's PESSIMISTIC_WRITE lock.

Reference: [OWASP Input Validation Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)

Reference: [CWE-20: Improper Input Validation](https://cwe.mitre.org/data/definitions/20.html)

Reference: [CWE-1284: Improper Validation of Specified Quantity in Input](https://cwe.mitre.org/data/definitions/1284.html)
