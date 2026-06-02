---
title: Erasure AND soft-delete purge MUST consult a fail-closed legal-hold gate before deleting
impact: HIGH
impactDescription: "A legal hold or records-retention obligation that lives only as prose inside the erasure handler is silently bypassed by the scheduled purge job — destroying records the controller was legally required to preserve (spoliation), or conversely treating a hold as advisory and deleting on a registry timeout"
tags:
  - dsr
  - gdpr
  - legal-hold
  - data-retention
  - erasure
  - soft-delete
  - fail-closed
  - spoliation
spec_ref: "specs/data-subject-rights-l0.yaml#DSR-LEGALHOLD-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/dsr/DsrRestrictionGate.java"
  pattern: "A standalone LegalHoldGate component (mirroring the existing fail-closed DsrRestrictionGate) is consulted by BOTH the erasure path and the soft-delete retention purge job before any delete; isHeld(subject, category) defaults to BLOCK on lookup failure (try/catch returns held=true), and every deletion entrypoint calls it (no delete path reaches removeFromStore without a preceding gate consultation)"
upstream:
  - "https://gdpr-info.eu/art-17-gdpr/"
  - "https://www.law.cornell.edu/rules/frcp/rule_37"
evidence:
  - source_type: external
    citation: "GDPR Article 17(3) — Exceptions to the right to erasure (legal obligation / legal claims)"
    url: "https://gdpr-info.eu/art-17-gdpr/"
    quote: "Paragraphs 1 and 2 shall not apply to the extent that processing is necessary: ... (b) for compliance with a legal obligation which requires processing by Union or Member State law to which the controller is subject or for the performance of a task carried out in the public interest or in the exercise of official authority vested in the controller; ... (e) for the establishment, exercise or defence of legal claims."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "US Federal Rules of Civil Procedure, Rule 37(e) — Failure to Preserve Electronically Stored Information"
    url: "https://www.law.cornell.edu/rules/frcp/rule_37"
    quote: "If electronically stored information that should have been preserved in the anticipation or conduct of litigation is lost because a party failed to take reasonable steps to preserve it, and it cannot be restored or replaced through additional discovery, the court: (1) upon finding prejudice to another party from loss of the information, may order measures no greater than necessary to cure the prejudice; or (2) only upon finding that the party acted with the intent to deprive another party of the information's use in the litigation may: (A) presume that the lost information was unfavorable to the party; (B) instruct the jury that it may or must presume the information was unfavorable to the party; or (C) dismiss the action or enter a default judgment."
    quoted_at: "2026-06-01"
---

## Erasure AND soft-delete purge MUST consult a fail-closed legal-hold gate before deleting

**Impact: HIGH — a retention obligation buried in prose is enforced on one delete path and silently bypassed on the other**

The right to erasure is not unconditional. GDPR Art 17(3) carves out the cases where the controller is *required* to keep the data: (b) compliance with a legal obligation (e.g. a tax / accounting / records-retention schedule) and (e) the establishment, exercise, or defence of legal claims (a litigation hold). US FRCP Rule 37(e) makes the same point from the other side: if information that *should have been preserved* in anticipation of litigation is destroyed, the court may impose sanctions up to dismissal or default judgment — spoliation. So a delete that ignores a hold is not merely a data-protection miss; it is a destruction of evidence the controller was legally bound to keep.

The failure mode this rule closes is structural, not behavioural. Today the hold obligation lives as a sentence inside the DSR erasure handler ("records under an active legal-hold yield a partial-erasure manifest"). That prose is invisible to the SECOND delete path: the scheduled soft-delete retention purge (`SOFTDELETE-PURGE-001`) that physically removes tombstoned rows after the retention window. Two independent code paths reach the same `removeFromStore`, and only one of them knows about the hold. The fix is to make the hold a **first-class, independent, fail-closed gate** — a component, not a comment — that BOTH paths consult before deleting, exactly the shape the reference workload already ships for Art 18 restriction (`DsrRestrictionGate`: default-deny, a path that forgets to consult is the bug).

Two properties are mandatory:

1. **Both delete paths consult it.** Erasure (`DsrService.erase`) and the retention purge job (`SoftDeletePurgeWorker`) and any admin force-purge MUST call `legalHoldGate.checkClearToDelete(subjectId, category)` before removal. A deletion entrypoint that reaches the store without a preceding gate call is the defect.
2. **Fail-closed on lookup failure.** When the hold registry is unreachable or throws, the gate MUST return *held* (block the delete), never *clear*. "I could not prove a hold exists" must resolve to "assume held" — an unreachable registry that defaults to delete is the worst outcome, because it destroys held records precisely when the system is degraded.

When a hold covers the subject/category the record is **retained**, the DSR request is **parked** (a `HELD` status with `{category, hold_reason, earliest_release_at}` in the partial-erasure manifest already specified by `DSR-ERASURE-001`), and erasure resumes automatically only when the hold lifts. The request is not failed and not silently completed-minus-the-held-rows without a manifest.

**Incorrect — the hold is prose in the erasure handler; the purge job deletes held rows, and a registry timeout deletes anyway:**

```java
// DsrService.erase(...) — knows about the hold (as an if), but only here
void erase(String subjectId) {
    // "records under legal hold are excluded" — encoded ad hoc, nowhere reusable
    if (holdRepo.findActive(subjectId).isEmpty()) {
        personalDataProviders.forEach(p -> p.eraseFor(subjectId));   // ❌ purge path never sees this guard
    }
}

// SoftDeletePurgeWorker — the OTHER delete path: no hold awareness at all
@Scheduled(cron = "0 0 3 * * *")
void purgeExpiredTombstones() {
    repo.findTombstonedOlderThan(retentionCutoff())
        .forEach(row -> repo.hardDelete(row.id()));                  // ❌ deletes rows under litigation hold
}

// And when the registry call throws, callers that DID guard often fail OPEN:
boolean held = false;
try { held = holdRepo.isHeld(subjectId); } catch (Exception e) { /* held stays false */ }  // ❌ delete proceeds on a timeout
```

**Correct — one fail-closed gate (sibling of `DsrRestrictionGate`) consulted by every delete path:**

```java
@Component
public class LegalHoldGate {

    private final LegalHoldRegistry registry;   // litigation holds + retention schedules

    public LegalHoldGate(LegalHoldRegistry registry) {
        this.registry = registry;
    }

    /** True when an active hold covers the target, and (fail-closed) when the registry lookup fails. */
    public boolean isHeld(String subjectId, String category) {
        try {
            return registry.activeHold(subjectId, category).isPresent();
        } catch (RuntimeException lookupFailed) {
            // fail-closed: an unreachable registry means "assume held", never "assume clear"
            return true;
        }
    }

    /** Sole guard both delete paths call. Throws → caller parks/skips; never deletes. */
    public void checkClearToDelete(String subjectId, String category) {
        if (isHeld(subjectId, category)) {
            throw LegalHoldException.held(subjectId, category);   // → park DSR request / skip purge row
        }
    }
}

// Erasure path:
void erase(String subjectId, String category) {
    legalHoldGate.checkClearToDelete(subjectId, category);   // ✅ blocks + parks under hold
    personalDataProviders.forEach(p -> p.eraseFor(subjectId, category));
}

// Retention purge path — the SAME gate, no second source of truth:
@Scheduled(cron = "0 0 3 * * *")
void purgeExpiredTombstones() {
    for (var row : repo.findTombstonedOlderThan(retentionCutoff())) {
        try {
            legalHoldGate.checkClearToDelete(row.subjectId(), row.category());  // ✅ skip held rows
            repo.hardDelete(row.id());
        } catch (LegalHoldException held) {
            log.info("purge skipped under legal hold: category={} reason={}", row.category(), held.reason());
        }
    }
}
```

The gate is the single place the obligation lives; both delete paths are physically unable to remove a held record because the call is on the only route to the store. Fail-closed on registry error means a degraded system over-retains (recoverable) rather than over-deletes (irreversible + sanctionable). This mirrors `DsrRestrictionGate` (Art 18) and `rbac-stub-default-fail-closed` (least-privilege default): when in doubt, the safe default is *do not act*.

Verification (review-tier): confirm a standalone `LegalHoldGate` exists, that BOTH the erasure service and the retention purge worker consult it before any removal (no `hardDelete` / `eraseFor` reachable without a preceding `checkClearToDelete`), and that the hold lookup defaults to *held* on exception. A fork-receiver with a concrete `LegalHoldRegistry` adds a RestAssured negative test: erasure of a held subject → 200 with a parked partial-erasure manifest (record still present), and a registry-down simulation → purge skips the row.

Reference: [GDPR Article 17(3) — Exceptions to the right to erasure](https://gdpr-info.eu/art-17-gdpr/)

Reference: [US FRCP Rule 37(e) — Failure to Preserve Electronically Stored Information](https://www.law.cornell.edu/rules/frcp/rule_37)
