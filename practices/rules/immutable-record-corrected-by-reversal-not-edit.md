---
title: A posted immutable record is corrected by APPENDING a reversing entry — never by editing or deleting the original
impact: HIGH
impactDescription: "Once a record is POSTED/committed (ledger entry, inventory adjustment, payroll line, finalized event), editing or deleting it to fix a mistake destroys the audit trail and silently rewrites history. The correction posture is append-only: keep the original visible and append a compensating reversal that nets it out, so original + reversal (+ fresh correction) all survive and reconcile."
tags:
  - audit
  - immutability
  - append-only
  - correction
  - reversal
  - value-conservation
spec_ref: "specs/soft-delete-l0.yaml#SOFTDELETE-REVERSAL-001"
verification:
  type: review
  source: "backend correction path for any POSTED immutable record (ledger/inventory/payroll/finalized-event service) + entity mapping"
  pattern: "The posted record has no edit/delete mutator and its value columns are @Column(updatable=false); a correction appends a NEW reversing record carrying reversal_of = original_id (and, when a different value is wanted, a fresh correct record), so a read of the chain shows original + reversal (+ correction) and the net is the intended value — the original row is never UPDATEd or physically DELETEd."
upstream:
  - "https://martinfowler.com/eaaDev/EventSourcing.html"
  - "https://www.accountingtools.com/articles/what-is-a-reversing-entry.html"
  - "https://www.patriotsoftware.com/blog/accounting/what-is-correcting-entries-journal-examples/"
evidence:
  - source_type: external
    citation: "Martin Fowler — Event Sourcing (eaaDev), §Reversing Events"
    url: "https://martinfowler.com/eaaDev/EventSourcing.html"
    quote: "Reversal is the most straightforward when the event is cast in the form of a difference. An example of this would be 'add $10 to Martin's account' as opposed to 'set Martin's account to $110'. In the former case I can reverse by just subtracting $10, but in the latter case I don't have enough information to recreate the past value of the account."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "AccountingTools — What is a reversing entry?"
    url: "https://www.accountingtools.com/articles/what-is-a-reversing-entry.html"
    quote: "A reversing entry is a journal entry made in an accounting period, which reverses selected entries made in the immediately preceding period."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Patriot Software — How to Make Correcting Entries in Accounting"
    url: "https://www.patriotsoftware.com/blog/accounting/what-is-correcting-entries-journal-examples/"
    quote: "A correcting entry in accounting fixes a mistake posted in your books."
    quoted_at: "2026-06-01"
---

## A posted immutable record is corrected by APPENDING a reversing entry — never by editing or deleting the original

**Impact: HIGH — Once a record is POSTED/committed, editing or deleting it to fix a mistake destroys the audit trail and silently rewrites history. The correction posture is append-only: keep the original visible and append a compensating reversal that nets it out, so original + reversal (+ fresh correction) all survive and reconcile.**

Some records are *posted*: a ledger entry, an inventory stock-take adjustment, a payroll line, a finalized domain event. After posting they represent a fact that *happened* — and facts are not edited away. The naive fix for a wrong posted amount is `UPDATE ledger SET amount = ... WHERE id = ...` or a `DELETE` of the bad row. Both are wrong for the same reason: they make the mistake disappear instead of recording that it was corrected. An auditor reading the table afterward sees a clean history that never contained the error — exactly the rewrite the audit trail exists to prevent. This is the centuries-old accounting rule: a correcting entry is *journalized* (a new entry is recorded); the erroneous posted entry is left in place and reversed, never erased.

The correct mechanism is **correction-by-reversal**. To fix a posted record you APPEND a NEW *reversing* record that compensates the original (equal and opposite — the difference form Fowler describes), carrying `reversal_of = original_id` so the two are linked. If a different value was actually intended, you ALSO append a fresh correct record. After the correction the chain reads: original (visible) + reversal (nets it to zero) + correction (the intended value). Value is conserved and every step is auditable. Nothing was hidden and nothing was destroyed — the read just sums to the right answer.

This is **distinct** from two neighbouring patterns and must not be conflated with either:

- **Soft-delete (tombstone)** *hides* a row (`deleted_at` set, default-excluded from queries). Reversal does the opposite: the original STAYS visible; a compensating entry nets it out. You do not tombstone a posted ledger entry — you reverse it.
- **Content versioning** produces a new *edition* of mutable content (a wiki page, a draft), superseding the prior text. A posted record is not content being re-edited; it is an immutable fact, and the reversal is a separate fact that offsets it, not a new version of the same fact.

The pattern is **generic and cross-cutting** — not fintech-only, not Korea-specific. It shows up wherever value or a finalized fact must be conserved: an accounting **reversal entry**; an inventory **stock-take adjustment** (you append a +N or −N adjustment, you don't retype the on-hand count); **loyalty points** reverse-and-regrant (reverse the wrong grant, grant the correct one); a **gradebook correction** (append a regrade that supersedes-by-offset while the original mark stays on the record). In every case the invariant is the same: original + reversal (+ correction) survive together and reconcile.

**Incorrect — posted record edited/deleted in place; the mistake vanishes and the audit trail is rewritten:**

```java
@Entity
public class LedgerEntry {
  @Id @GeneratedValue Long id;
  BigDecimal amount;        // mutable — wrong
}

// "Fix" the wrong amount by mutating or deleting the posted row:
ledgerRepository.findById(id).ifPresent(e -> {
  e.setAmount(correctedAmount);   // ❌ rewrites a posted fact; original error is now invisible
});
// or worse:
ledgerRepository.deleteById(id);  // ❌ posted fact physically erased — reconciliation can never explain it
```

**Correct — original stays immutable and visible; a reversal (and, if needed, a fresh correction) is appended:**

```java
@Entity
public class LedgerEntry {
  @Id @GeneratedValue Long id;
  @Column(updatable = false) BigDecimal amount;     // posted value is immutable
  @Column(updatable = false) Instant postedAt;
  @Column(updatable = false) Long reversalOf;       // null = original; set = this entry reverses #reversalOf
}

// LedgerService is the sole writer; it never UPDATEs or DELETEs a posted row.
public void correct(Long originalId, BigDecimal intendedAmount) {
  LedgerEntry original = repo.findById(originalId).orElseThrow();
  // 1. append the reversal (equal-and-opposite difference — Fowler's reversal form)
  repo.save(LedgerEntry.reversalOf(original, original.getAmount().negate()));
  // 2. append the fresh correct entry (only if a different value was intended)
  if (intendedAmount != null) {
    repo.save(LedgerEntry.posted(intendedAmount));
  }
  // chain now reads: original (visible) + reversal (nets to 0) + correction (intended) — value conserved, fully auditable
}
```

Keep this layered with the surrounding posture: the original carries the append-only / immutable-columns posture (`@Column(updatable=false)`, no edit/delete mutator) so the only way to change the net is to append another entry; pairs with `soft-delete-l0` (retain — never physically erase a record that must survive) and with `tamper-evident-log-l0` (the chain of entries stays intact and verifiable). The difference from soft-delete is precise: soft-delete hides; reversal compensates while the original stays visible.

Verification: review the posted entity and its correction path — confirm value columns are `@Column(updatable=false)`, the service exposes no edit/delete mutator for a posted record, and `correct()` appends a reversing record (`reversal_of = original_id`, equal-and-opposite) plus an optional fresh correct record, so a read of the chain yields original + reversal (+ correction) and the net equals the intended value while the original row is never UPDATEd or DELETEd.

Reference: [Martin Fowler — Event Sourcing (eaaDev), Reversing Events](https://martinfowler.com/eaaDev/EventSourcing.html)

Reference: [AccountingTools — What is a reversing entry?](https://www.accountingtools.com/articles/what-is-a-reversing-entry.html)

Reference: [Patriot Software — Correcting Entries in Accounting](https://www.patriotsoftware.com/blog/accounting/what-is-correcting-entries-journal-examples/)
