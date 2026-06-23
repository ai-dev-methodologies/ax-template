---
title: An external-feed reconciliation must CLASSIFY each internal/external pair EXACTLY ONCE with its recorded basis (internal value, external value, delta — never a bare aggregate count), require EXPLICIT human DISPOSITION of every BREAK (who/when/reason) before the run can be RESOLVED (an undisposed break is 422), be IDEMPOTENT on the feed snapshot hash (same feed → same run, changed feed → new run, prior retained), and serialize concurrent disposes on one break so exactly one wins
impact: HIGH
impactDescription: "A reconciliation that reports a bare aggregate count (matched: 412, breaks: 3) with no per-item basis cannot be re-derived, audited, or reconciled to the allowance the receivable is stated net of; a run that can be marked RESOLVED while a break is unexplained defeats the entire control (a back office signs off on differences it never investigated); a non-idempotent re-run double-books the same statement; and an unsynchronized dispose lets two operators write conflicting dispositions on one break (CWE-362) — the break ends with two verdicts or a half-written one"
tags:
  - state-machine
  - audit
  - concurrency
  - billing
  - governance
spec_ref: "specs/external-reconciliation-l0.yaml#RECON-CLASSIFY-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/reconciliation/ReconciliationService.java + backend/src/main/java/com/ax/template/authblueprint/reconciliation/ReconciliationItem.java + backend/src/main/java/com/ax/template/authblueprint/reconciliation/ReconciliationRun.java"
  pattern: "A run matches an internal record set against an external feed snapshot, classifying every distinct key EXACTLY ONCE into MATCHED / BREAK / INTERNAL_ONLY / EXTERNAL_ONLY and PERSISTING each item's basis (internal amount + external amount + delta) so a bare aggregate count is unrepresentable; a BREAK requires an explicit human disposition (ACCEPT_INTERNAL / ACCEPT_EXTERNAL / ADJUST) recording who/when/reason, taken under the item's PESSIMISTIC_WRITE row lock so concurrent disposes converge to exactly one winner (the disposed-once precondition makes the loser a deterministic 409); a run cannot be RESOLVED while any break is undisposed (a service gate counting undisposed breaks + a @Check on the item making a disposed non-break / half-written disposition unrepresentable); the run is idempotent on (sourceKey, feedSnapshotHash) — a re-run on the SAME feed returns the existing run verbatim (uq(source_key, feed_snapshot_hash) + uq(run_id, item_key) backstops), a CHANGED feed appends a NEW run and the prior is retained; NO delete path exists"
upstream:
  - "https://pcaobus.org/oversight/standards/auditing-standards/details/AS2305"
  - "https://www.law.cornell.edu/cfr/text/17/210.5-02"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "PCAOB AS 2305 .02 (Substantive Analytical Procedures) — the evaluate-plausible-relationships discipline an external-feed reconciliation realizes (matching recorded data against an independent expectation)"
    url: "https://pcaobus.org/oversight/standards/auditing-standards/details/AS2305"
    quote: "Analytical procedures are an important part of the audit process and consist of evaluations of financial information made by a study of plausible relationships among both financial and nonfinancial data."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "PCAOB AS 2305 .21 (Investigation and Evaluation of Significant Differences) — the investigate-every-significant-difference requirement a BREAK disposition realizes (a difference is explained, not silently accepted)"
    url: "https://pcaobus.org/oversight/standards/auditing-standards/details/AS2305"
    quote: "The auditor should evaluate significant unexpected differences. Reconsidering the methods and factors used in developing the expectation and inquiry of management may assist the auditor in this regard."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "17 CFR § 210.5-02(4) (Regulation S-X, Cornell LII) — the allowance for doubtful accounts a receivable reconciliation feeds, stated separately on the balance sheet"
    url: "https://www.law.cornell.edu/cfr/text/17/210.5-02"
    quote: "Allowances for doubtful accounts and notes receivable. The amount is to be set forth separately in the balance sheet or in a note thereto."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent disposes racing one break)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-23"
---

## A reconciliation is classify-with-basis, dispose-every-break, resolve-only-when-clean, and idempotent-on-the-feed — not a pair of aggregate counts

**Impact: HIGH — a bare matched/break count cannot be re-derived or audited; a run resolved over an unexplained break defeats the control; a non-idempotent re-run double-books a statement; an unsynchronized dispose writes two verdicts on one break (CWE-362).**

An *external-feed reconciliation* is the periodic control a back office runs against every counterparty / custodian / bank statement: match the internal record set against an external feed snapshot, classify each pair, and explain every difference before signing off. The discipline is the audit standard generalized: PCAOB AS 2305 defines analytical procedures as *"evaluations of financial information made by a study of plausible relationships among both financial and nonfinancial data"* and requires that the auditor *"evaluate significant unexpected differences"* — a difference is **investigated and explained**, never silently accepted. The catalog governed decisions (`decision-governance`), netted obligations (`netting`), and matched identity records (`record-linkage`) but had no primitive for the classify-with-basis, dispose-every-break, idempotent-on-the-feed reconciliation:

```text
run(source, feedHash, internal, external):  classify EVERY key once into MATCHED / BREAK /
                                             INTERNAL_ONLY / EXTERNAL_ONLY; PERSIST each item's
                                             basis (internal amount + external amount + delta);
                                             idempotent on (source, feedHash) — same feed → same run
dispose(break):                              ACCEPT_INTERNAL / ACCEPT_EXTERNAL / ADJUST + who/when/
                                             reason, under the item's PESSIMISTIC_WRITE lock; only
                                             a BREAK; disposed-once → the loser of any race is 409
resolve(run):                                refused 422 while ANY break is undisposed; a @Check makes
                                             a disposed non-break / half-written disposition impossible
```

**1. Each pair is classified exactly once, with its basis (RECON-CLASSIFY-001).** The classification AND the internal amount, external amount, and delta are persisted on the item row; the items ARE the record, so a bare aggregate count is unrepresentable. `uq(run_id, item_key)` makes a duplicate key within one run impossible.

**2. Every break is explicitly disposed (RECON-DISPOSE-001).** Only a BREAK is disposed (a MATCHED / INTERNAL_ONLY / EXTERNAL_ONLY item is refused 422); the disposition records who (the authenticated caller), when (the injected clock), and a non-blank reason — the difference is explained, mirroring AS 2305 .21.

**3. A run resolves only when clean, and re-runs idempotently (RECON-RESOLVE/IDEMPOTENT-001).** A run cannot be RESOLVED while any break is undisposed (422); a re-run on the SAME feed snapshot hash returns the existing run verbatim, a CHANGED feed appends a new run and the prior is retained.

**Incorrect — a pair of aggregate counts, a resolve with no gate, an unsynchronized dispose:**

```java
public ReconResult reconcile(String source, List<Line> internal, List<Line> external) {
    int matched = 0, breaks = 0;
    for (Line in : internal) {                              // ❌ no per-item basis recorded
        if (external.stream().noneMatch(e -> e.key().equals(in.key()))) breaks++;
        else matched++;                                     // ❌ a bare count — un-re-derivable, un-auditable
    }
    return new ReconResult(matched, breaks);                // ❌ no break disposition, no run identity
}

public void resolve(UUID runId) {
    var run = repo.findById(runId).orElseThrow();           // ❌ no row lock; no undisposed-break gate
    run.setStatus("RESOLVED");                               // ❌ public setter; resolves over unexplained breaks
    repo.save(run);
}
```
<!-- catalog-example-ok: ReconResult — illustrative aggregate-count anti-pattern; the reference impl records per-item basis on ReconciliationItem -->
<!-- catalog-example-ok: Line — illustrative request line in the anti-pattern -->

**Correct — classify with basis, dispose under the item lock, resolve only when clean:**

```java
@Transactional
public ReconciliationRun run(String sourceKey, String feedSnapshotHash,
                             Map<String, BigDecimal> internal, Map<String, BigDecimal> external) {
    var existing = runs.findBySourceKeyAndFeedSnapshotHash(sourceKey, feedSnapshotHash);
    if (existing.isPresent()) return existing.get();        // ✅ idempotent — same feed → same run
    Instant now = Instant.now(clock);
    ReconciliationRun saved = runs.save(new ReconciliationRun(UUID.randomUUID(), sourceKey, feedSnapshotHash, now));
    TreeSet<String> keys = new TreeSet<>();
    keys.addAll(internal.keySet());
    keys.addAll(external.keySet());
    for (String key : keys) {
        members.persist(new ReconciliationItem(UUID.randomUUID(), saved.getId(), key,
            internal.get(key), external.get(key), now));     // ✅ classification + basis (amounts + delta)
    }
    return saved;
}

@Transactional
public ReconciliationItem dispose(UUID runId, UUID itemId, DispositionType type, String reason, String actor) {
    ReconciliationItem item = runs.findItemByIdForUpdate(itemId).orElseThrow(ReconciliationException::notFound); // ✅ PESSIMISTIC_WRITE
    if (!item.getRunId().equals(runId)) throw ReconciliationException.notFound();      // ✅ IDOR-safe 404
    if (!item.isBreak()) throw ReconciliationException.notABreak();                    // 422 — only a break disposes
    if (reason == null || reason.isBlank()) throw ReconciliationException.blankReason(); // 422
    if (item.isDisposed()) throw ReconciliationException.alreadyDisposed();            // ✅ disposed-once → 409 loser
    item.dispose(type, actor, Instant.now(clock), reason);                             // ✅ who / when / reason recorded
    return item;
}

@Transactional
public ReconciliationRun resolve(UUID runId) {
    ReconciliationRun run = runs.findByIdForUpdate(runId).orElseThrow(ReconciliationException::notFound);
    if (run.isResolved()) return run;                       // ✅ idempotent
    if (runs.countUndisposedBreaks(runId) > 0) throw ReconciliationException.undisposedBreak();  // 422 gate
    run.resolve(Instant.now(clock));                        // ✅ resolved only when every break is explained
    return run;
}
```

The item-row PESSIMISTIC_WRITE lock serializes the read-undisposed / write-disposition sequence; the disposed-once precondition makes N concurrent disposes on one break resolve to exactly one winner (the rest → 409). The resolve gate counts undisposed breaks and the item `@Check` (`disposed = FALSE OR classification = 'BREAK'` and a disposed row carries every disposition field) makes a disposed non-break or a half-written disposition unrepresentable (CWE-362). `ReconciliationItem` rows are `@AggregateMember` of `ReconciliationRun` — root-JPQL reads, `common/MemberWriter` writes; no delete path exists.

Verification: review-tier — confirm each pair is classified once with its basis persisted, only a break is disposed (with who/when/reason), the resolve path refuses an undisposed break, the re-run is idempotent on the feed hash, and the dispose takes the item's PESSIMISTIC_WRITE lock. The behavioural proof a fork-receiver keeps green: the N-thread dispose race on one break (exactly one 2xx + N-1 409).

Reference: [PCAOB AS 2305 (Substantive Analytical Procedures)](https://pcaobus.org/oversight/standards/auditing-standards/details/AS2305)

Reference: [17 CFR § 210.5-02 (Regulation S-X — allowance for doubtful accounts)](https://www.law.cornell.edu/cfr/text/17/210.5-02)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
