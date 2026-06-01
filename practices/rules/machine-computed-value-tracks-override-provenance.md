---
title: A machine-computed but human-overridable field MUST track override provenance — and recompute MUST skip human overrides
impact: HIGH
impactDescription: "Without a valueSource flag, a nightly recompute silently reverts every human correction (analyst fraud-review, instructor grade override, reviewer ML fix) — a lost human decision GDPR Art 22 explicitly protects"
tags:
  - provenance
  - audit
  - automated-decision
  - human-override
  - data-integrity
  - gdpr
spec_ref: "specs/value-provenance-l0.yaml#PROVENANCE-NO-RECLOBBER-001"
verification:
  type: review
  source: "specs/value-provenance-l0.yaml"
  pattern: "Entity field that is machine-computed AND human-overridable carries valueSource(SYSTEM|USER) + overriddenByActorId + overriddenAt + preserved machineValue; the recompute/batch path filters `where valueSource = SYSTEM` (or skips USER rows) so a human override is never clobbered"
upstream:
  - "https://gdpr-info.eu/art-22-gdpr/"
  - "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x15-V7-Error-Logging.md"
evidence:
  - source_type: external
    citation: "GDPR — Article 22 (Automated individual decision-making, including profiling) §1 and §3"
    url: "https://gdpr-info.eu/art-22-gdpr/"
    quote: "The data subject shall have the right not to be subject to a decision based solely on automated processing, including profiling, which produces legal effects concerning him or her or similarly significantly affects him or her."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "OWASP ASVS V7 — Error Handling and Logging, requirement 7.1.3 (log security relevant events)"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x15-V7-Error-Logging.md"
    quote: "Verify that the application logs security relevant events including successful and failed authentication events, access control failures, deserialization failures and input validation failures."
    quoted_at: "2026-06-01"
---

## A machine-computed but human-overridable field MUST track override provenance — and recompute MUST skip human overrides

**Impact: HIGH — a value the system computes and a human can correct is two values wearing one column; without provenance the machine wins every recompute and the human correction is silently lost**

Lots of fields are *both* machine-computed *and* human-overridable: an auto-assigned fraud score an analyst can downgrade, an ML-suggested tag a reviewer can fix, a predicted category a human approves or rewrites, an auto-computed grade an instructor overrides, a risk tier ops can adjust. The field looks like one column, but it carries two possible authorities — the model and the person. The moment you let a human override the model, you have created a question the row must be able to answer: *is this current value the machine's, or did a human deliberately set it?* If the column cannot answer that, two failures follow. First, you cannot audit or defend the value (who changed the fraud score, and when?). Second — and this is the silent one — the next time the recompute job runs (nightly batch, new model version, event-driven re-score), it overwrites the human's correction with a fresh machine value, because nothing told it to stop. The analyst's careful downgrade, the instructor's grade fix, the reviewer's tag correction — all reverted overnight, with no error and no trace. Under GDPR Art 22 that is precisely the harm regulated: an automated decision silently displacing a human intervention.

The fix is a small, generic provenance shape attached to the overridable field: a `valueSource` enum (`SYSTEM` | `USER`, default `SYSTEM`), an `overriddenByActorId` + `overriddenAt` stamped atomically when a human overrides, and the prior machine value preserved (so the baseline is never destroyed). Then one rule binds the recompute path: **a recompute MUST skip rows whose `valueSource == USER`.** It may refresh `SYSTEM` rows and may update the preserved `machineValue` baseline, but it MUST NOT touch the live value of a row a human owns. This is review-tier because it is a runtime/structural property of the write paths, not something a single compiled assertion can fully prove — the reviewer confirms (a) the four provenance members exist, (b) the override mutator stamps actor + timestamp + audit event in one write, and (c) every recompute query carries the `valueSource = SYSTEM` filter.

**Incorrect — one column, no provenance; the nightly recompute clobbers every human correction:**

```java
@Entity
class Transaction {
    @Id Long id;
    BigDecimal fraudScore;   // computed by the model AND editable by an analyst — but which is it now?
}

// analyst manually downgrades a false positive
tx.setFraudScore(new BigDecimal("0.05"));   // human correction, no marker left behind
repo.save(tx);

// nightly batch — re-scores EVERYTHING, including the row the analyst just fixed
@Scheduled(cron = "0 0 3 * * *")
void recompute() {
    for (Transaction t : repo.findAll()) {        // ❌ no valueSource filter
        t.setFraudScore(model.score(t));          // ❌ silently reverts the analyst's 0.05 back to 0.92
        repo.save(t);
    }
}
```

**Correct — provenance columns + an override mutator that stamps the actor + a recompute that SKIPS human-owned rows:**

```java
enum ValueSource { SYSTEM, USER }

@Entity
class Transaction {
    @Id Long id;
    BigDecimal fraudScore;                       // the live value
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    ValueSource fraudScoreSource = ValueSource.SYSTEM;
    Long fraudScoreOverriddenBy;                 // non-null iff source == USER
    Instant fraudScoreOverriddenAt;
    BigDecimal fraudScoreMachineValue;           // preserved model baseline — never destroyed by an override
}

// sole mutator for a human override — atomic stamp + audit
@Transactional
void override(Long txId, BigDecimal newValue, Long actorId) {
    Transaction t = repo.findById(txId).orElseThrow();
    t.fraudScoreMachineValue = t.fraudScoreSource == ValueSource.SYSTEM ? t.fraudScore : t.fraudScoreMachineValue;
    t.fraudScore = newValue;
    t.fraudScoreSource = ValueSource.USER;
    t.fraudScoreOverriddenBy = actorId;
    t.fraudScoreOverriddenAt = Instant.now();
    audit.record("FRAUD_SCORE_OVERRIDE", actorId, txId, t.fraudScoreMachineValue, newValue); // ASVS V7 7.1.3
}

@Scheduled(cron = "0 0 3 * * *")
@Transactional
void recompute() {
    for (Transaction t : repo.findByFraudScoreSource(ValueSource.SYSTEM)) {  // ✅ USER rows excluded by query
        t.fraudScore = model.score(t);
        t.fraudScoreMachineValue = t.fraudScore;
    }
    // USER rows: only the preserved machineValue baseline may refresh; the live value is untouched.
    meterRegistry.counter("provenance_reclobber_skipped_total",
        "resource", "transaction.fraud_score").increment(repo.countByFraudScoreSource(ValueSource.USER));
}
```

The shape is identical for every instance of the pattern: auto-tag (`tagSource`), ML label (`labelSource`), auto-grade (`gradeSource`), risk tier (`riskTierSource`). Rename the field, keep the four provenance members and the `valueSource = SYSTEM` recompute filter. The cheapest way to get this wrong is to add the override path first and the recompute filter never — the override works in the demo, and the data quietly reverts in production a day later.

Verification: review-tier. Confirm (1) the overridable field declares `valueSource` (`SYSTEM`|`USER`, default `SYSTEM`, non-null) plus `overriddenBy` / `overriddenAt` / preserved `machineValue`; (2) the override is performed by one atomic mutator that stamps actor + timestamp and emits an audit event (ASVS V7 §7.1.3); (3) every recompute / batch / re-score query carries the `valueSource = SYSTEM` filter (or an explicit per-row `if (source == USER) continue;` skip) so a human override is never overwritten, with the skip counted by `provenance_reclobber_skipped_total`. Spec: `specs/value-provenance-l0.yaml#PROVENANCE-NO-RECLOBBER-001`.

Reference: [GDPR Article 22 — Automated individual decision-making, including profiling](https://gdpr-info.eu/art-22-gdpr/)

Reference: [OWASP ASVS V7 — Error Handling and Logging (7.1.3)](https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x15-V7-Error-Logging.md)
