---
title: A contraindication / conflict gate must evaluate the candidate against the SET of the subject's other active members (set-intersection on a normalized concept), grade each finding ABSOLUTE vs RELATIVE, FAIL CLOSED on an unassessable candidate, and re-read the set in the same transaction — never a single-subject one-flag check, never a silent allow on an unknown concept
impact: HIGH
impactDescription: "A single-subject blocking flag cannot express 'this new medication conflicts with one of the patient's OTHER active meds/allergies' — the load-bearing safety check; silently allowing a candidate whose concept the knowledge base does not recognize lets an unrecognized drug bypass interaction checking (the exact harm the gate exists to prevent); evaluating a pre-read snapshot lets two mutually-conflicting members both be admitted under concurrency"
tags:
  - concurrency
  - state-machine
  - authorization
  - safety
  - fail-closed
spec_ref: "specs/negative-copresence-gate-l0.yaml#GATE-SET-EVAL-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/copresence/CopresenceService.java + backend/src/main/java/com/ax/template/authblueprint/copresence/SubjectRepository.java"
  pattern: "Activating a member takes the subject row under PESSIMISTIC_WRITE in the same @Transactional, then (a) FAILS CLOSED if the candidate concept is absent from the knowledge base (unmapped → 422 COPRESENCE_UNASSESSABLE, never silent allow), (b) loads the subject's ACTIVE members and intersects the candidate's NORMALIZED concept key against each via the conflict knowledge base, (c) grades findings ABSOLUTE vs RELATIVE, (d) rejects ABSOLUTE unconditionally (422, no override entrypoint), rejects RELATIVE unless a non-blank override reason is supplied atomically (recording the overridden findings on the new row), and (e) is the SOLE path that activates a member (no public setter / no bypass mutator); the active-set read is inside the locked transaction, not a stale pre-read snapshot"
upstream:
  - "https://web.mit.edu/Saltzer/www/publications/protection/Basic.html"
  - "https://www.postgresql.org/docs/current/explicit-locking.html"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "Saltzer & Schroeder, 'The Protection of Information in Computer Systems' (1975), design principle: Fail-safe defaults"
    url: "https://web.mit.edu/Saltzer/www/publications/protection/Basic.html"
    quote: "Base access decisions on permission rather than exclusion. This principle, suggested by E. Glaser in 1965, means that the default situation is lack of access, and the protection scheme identifies conditions under which access is permitted."
    quoted_at: "2026-06-08"
  - source_type: external
    citation: "PostgreSQL Documentation — 'Explicit Locking' (FOR UPDATE serializes the subject so the set re-read is current)"
    url: "https://www.postgresql.org/docs/current/explicit-locking.html"
    quote: "FOR UPDATE causes the rows retrieved by the SELECT statement to be locked as though for update. This prevents them from being locked, modified or deleted by other transactions until the current transaction ends."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A contraindication gate must be set-evaluated, graded, and fail-closed

**Impact: HIGH — a single-subject blocking flag cannot express "this new medication conflicts with one of the patient's OTHER active meds/allergies"; silently allowing an unrecognized concept bypasses the safety check entirely; a pre-read snapshot admits two mutually-conflicting members under concurrency.**

The catalog's gates are all **single-subject, one-flag**: `blocking-status-gate` re-reads one entity's own `BLOCKED` flag, `bounded-capacity-claim` is a scalar counter, `exclusive-assignment` is a cardinality-1 named pair, `consent`/`legal-hold`/`dsr-restriction` forbid on one present fact. None of them can answer the question medication safety (and separation-of-duties, double-booking, incompatible-config, incompatible-materials) actually asks: **does this candidate conflict with any of the subject's OTHER active members?** That is a *set-intersection on a normalized concept*, with a *graded* verdict, that must *fail closed*.

```text
gate(subject, candidate):
  lock(subject)                                              # serialize; in-tx set re-read
  if candidate.concept ∉ knowledgeBase:   BLOCK  # fail-safe default (unassessable → deny)
  findings = { rule(candidate.concept, m.concept)            # set-intersection on NORMALIZED concept
               for m in subject.activeMembers if conflicts }
  if any finding ABSOLUTE:                 BLOCK  # hard-stop, no override
  if any finding RELATIVE and no reason:   BLOCK  # soft-stop
  else: commit (record overridden findings if any)
```

Three defects recur, and one rule closes them.

**Defect 1 — a single-subject flag instead of a set check.** Reusing `blocking-status-gate` (is THIS record blocked?) cannot detect a conflict that only exists *relative to the subject's other rows*. The candidate must be intersected against the **set** of the subject's active members on a **normalized concept key** (a drug/allergen/role *class*, not the literal name — "Amoxil" and "amoxicillin" share the penicillin concept), via a knowledge base of conflicting concept pairs.

**Defect 2 — silent allow on an unknown concept.** If the knowledge base does not recognize the candidate's concept (unmapped) or is unreachable, returning "no conflict found → allow" lets an unrecognized drug skip interaction checking — the worst outcome. Saltzer & Schroeder's fail-safe defaults govern: *"Base access decisions on permission rather than exclusion … the default situation is lack of access."* An unassessable candidate **BLOCKS**.

**Defect 3 — evaluating a stale snapshot (CWE-362).** Reading the active set, then committing the new member in a later step, lets two concurrent adds of mutually-conflicting members both pass (each evaluated the set *before* the other). Take the **subject row lock** and read the active set **inside the locked transaction** so the second add sees the first.

A fourth concern — overriding a *RELATIVE* finding — is the `GATE-OVERRIDE-001` branch: proceed only with a non-blank reason captured atomically, the overridden findings recorded by reference on the new row; an `ABSOLUTE` finding has **no** override path.

**Incorrect — single-flag, silent-allow-on-unknown, stale snapshot:**

```java
public void addMember(UUID subjectId, String concept) {
    if (subject.isBlocked()) throw new BlockedException();   // ❌ DEFECT 1: one flag, not the set
    var conflicts = kb.find(concept);                        // ❌ DEFECT 2: unknown concept → empty → allow
    if (!conflicts.isEmpty()) throw new ConflictException();
    repo.save(new Member(subjectId, concept));               // ❌ DEFECT 3: no lock, stale set
}
```

**Correct — subject lock + fail-closed + set-intersection + graded:**

```java
@Transactional
public SubjectMember addMember(String subjectKey, String concept, String label, String overrideReason) {
    Subject s = subjects.findBySubjectKeyForUpdate(subjectKey)        // ✅ lock; in-tx set re-read (DEFECT 3)
        .orElseThrow(CopresenceException::subjectNotFound);
    if (!knownConcepts.existsByConcept(concept))                      // ✅ fail-safe default (DEFECT 2)
        throw CopresenceException.unassessable();
    List<Finding> findings = members.findActive(s.getId()).stream()   // ✅ set-intersection (DEFECT 1)
        .flatMap(m -> conflicts.find(concept, m.getConcept()).stream()
            .map(r -> new Finding(m.getConcept(), r.getSeverity())))
        .toList();
    if (findings.stream().anyMatch(f -> f.severity() == ABSOLUTE))    // ✅ hard-stop, no override
        throw CopresenceException.absolute();
    boolean relative = findings.stream().anyMatch(f -> f.severity() == RELATIVE);
    if (relative && (overrideReason == null || overrideReason.isBlank()))
        throw CopresenceException.relative();                         // ✅ soft-stop without a reason
    return members.save(SubjectMember.active(s.getId(), concept, label,
        relative ? overrideReason.strip() : null, renderOverridden(findings)));  // ✅ overridden findings by reference
}
```

`FOR UPDATE` serializes concurrent adds (*"This prevents them from being … modified … by other transactions until the current transaction ends"*) so the set re-read is current; an unassessable concept blocks (fail-safe default); the verdict is graded and lives on no member row. The gate is the sole activation path.

Verification: review-tier — confirm the activation path locks the subject, fails closed on an unmapped concept, intersects the candidate's normalized concept against the active-member set via the knowledge base, grades ABSOLUTE (no override) vs RELATIVE (override only with an atomic non-blank reason, overridden findings recorded by reference), reads the set inside the locked transaction, and is the only path that activates a member. The canonical proof a fork-receiver writes: an ABSOLUTE conflict blocks, a RELATIVE blocks-without/commits-with a reason, an unknown concept blocks, and two concurrent mutually-conflicting adds never both commit.

Reference: [Saltzer & Schroeder — Fail-safe defaults](https://web.mit.edu/Saltzer/www/publications/protection/Basic.html)

Reference: [PostgreSQL — Explicit Locking (FOR UPDATE)](https://www.postgresql.org/docs/current/explicit-locking.html)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
