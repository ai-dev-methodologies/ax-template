---
title: Record linkage must band its verdicts Fellegi-Sunter-style with the score, per-field feature breakdown, and thresholds RECORDED on the proposal; the REVIEW band decides only by an explicit human confirm/reject; and a merge records per-field survivorship while TOMBSTONING the loser with a forward pointer — never deleting it
impact: HIGH
impactDescription: "A bare unexplained match verdict cannot be appraised or contested after the fact; auto-merging the uncertain band is how wrong-patient/wrong-customer consolidations happen (the band where the algorithm abstains belongs to a human); and deleting the losing record orphans every reference that pointed at it and destroys the audit trail the merge governance exists to keep — a mis-merge becomes irreversible AND invisible"
tags:
  - audit
  - state-machine
  - matching
  - governance
  - concurrency
spec_ref: "specs/record-linkage-l0.yaml#LINK-BAND-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/recordlinkage/LinkageService.java + backend/src/main/java/com/ax/template/authblueprint/recordlinkage/MatchProposal.java"
  pattern: "Every proposal row persists score + per-field feature breakdown + the threshold pair in force (a bare verdict is unrepresentable); the verdict bands AUTO_MATCH / REVIEW / NO_MATCH; a REVIEW proposal mutates ONLY via explicit confirm/reject recording who/when (AUTO_MATCH merges with decidedBy=AUTO and the same trail; NO_MATCH cannot be confirmed — 422); the merge appends one immutable SurvivorshipDecision per identity field (field, winning value, source record, rule) and TOMBSTONES the loser (status MERGED + mergedIntoId, values retained verbatim — no delete path exists); resolve follows the pointer chain cycle-safely; confirm/merge take the proposal's and BOTH records' PESSIMISTIC_WRITE locks in ascending-id order, double-confirm and merged-participant are deterministic 409s"
upstream:
  - "https://textbook.coleridgeinitiative.org/chap-link.html"
  - "https://pmc.ncbi.nlm.nih.gov/articles/PMC2815491/"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "Big Data and Social Science (Coleridge Initiative textbook), ch. 'Record Linkage' — the Fellegi-Sunter banding as canonically taught: above-threshold matches, and a between-thresholds class sent to humans"
    url: "https://textbook.coleridgeinitiative.org/chap-link.html"
    quote: "Record pairs with a match score greater than T₁ are marked as matches and removed from further consideration. The set of record pairs with a match score between T₁ and T₂ are believed to contain significant numbers of matches and nonmatches. These are sent to clerical review, meaning that research assistants will make a final determination of match status."
    quoted_at: "2026-06-11"
  - source_type: external
    citation: "Duplicate Medical Records: A Survey of Twin Cities Healthcare Organizations (PMC2815491) — the merge-governance anchor"
    url: "https://pmc.ncbi.nlm.nih.gov/articles/PMC2815491/"
    quote: "Ensure that there is a complete auditing trail to track the process of duplicate merges."
    quoted_at: "2026-06-11"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent confirms racing one proposal / one shared participant record)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## Linkage verdicts are banded and explained; the uncertain band belongs to a human; merges tombstone, never delete

**Impact: HIGH — an unexplained verdict cannot be contested; auto-merging the uncertain band is how wrong-entity consolidations happen; deleting the loser orphans its references and erases the trail.**

*Record linkage* — deciding whether two records denote the SAME real-world entity (two patient registrations, two customer accounts, two counterparty masters) — is a determination with consequences on both error sides: a missed match fragments history; a wrong merge contaminates it. The canonical Fellegi-Sunter discipline, as taught: pairs *"with a match score greater than T₁ are marked as matches"*, and the pairs between the thresholds *"are sent to clerical review, meaning that research assistants will make a final determination of match status."* The catalog's existing primitives cover none of this (`negative-copresence-gate` intersects a candidate against a knowledge base — a write gate, not pair similarity; `decision-governance` versions one scope's computed value — no cross-record identity):

```text
propose(a, b):  score + PER-FIELD breakdown + thresholds RECORDED on the proposal row
                band: score ≥ upper → AUTO_MATCH · between → REVIEW · below → NO_MATCH
REVIEW:         decides ONLY via explicit human confirm/reject (who/when);
                NO_MATCH cannot be confirmed (422); double-decide → 409
merge:          one immutable SurvivorshipDecision PER FIELD (value, source record, rule);
                loser → status MERGED + mergedIntoId (values retained) — NO delete path
resolve(id):    follows mergedIntoId chains cycle-safely to the living survivor
locks:          proposal + BOTH records, PESSIMISTIC_WRITE, ascending-id order
```

**1. Explained, banded verdicts (LINK-BAND-001).** The proposal row carries everything an auditor needs to re-appraise it: the score, which fields agreed and what each contributed, and the thresholds in force at proposal time (thresholds drift — the row pins what governed THIS verdict). A bare yes/no is unrepresentable.

**2. The uncertain band belongs to a human (LINK-REVIEW-001).** AUTO_MATCH may merge unattended — with the identical recorded trail (`decidedBy = AUTO`). The REVIEW band is the algorithm ABSTAINING: only an explicit confirm/reject with who/when decides it. Confirming a NO_MATCH is refused — if the records changed, re-propose and let the score speak.

**3. Survivorship is recorded per field; the loser is tombstoned (LINK-SURVIVOR-001).** The merge appends one immutable decision per identity field — winning value, source record, the rule that chose it — and marks the loser `MERGED` with a forward pointer, its own values retained verbatim. The duplicate-records literature states the bar plainly: *"Ensure that there is a complete auditing trail to track the process of duplicate merges."* Deletion would orphan every old reference and make a mis-merge both irreversible and invisible.

**Incorrect — bare verdict, auto-merge everywhere, delete-the-duplicate:**

```java
public void dedupe(UUID a, UUID b) {
    if (similarity(a, b) > 0.7) {            // ❌ score thrown away — no breakdown, no thresholds
        Record loser = repo.findById(b).orElseThrow();
        copyMissingFields(a, loser);          // ❌ survivorship choices unrecorded
        repo.delete(loser);                   // ❌ references orphaned; trail erased;
    }                                         // ❌ uncertain band auto-merged — no human
}
```

**Correct — recorded banded proposal; human-owned REVIEW; tombstoning merge under ordered locks:**

```java
@Transactional
public MatchProposal propose(UUID aId, UUID bId) {
    LinkageRecord a = records.findByIdForUpdate(min(aId, bId)).orElseThrow(...);   // ✅ ascending-id
    LinkageRecord b = records.findByIdForUpdate(max(aId, bId)).orElseThrow(...);   //    lock order
    requireActive(a); requireActive(b);                       // merged participant → 409
    Scorecard card = scorer.score(a, b);                      // deterministic reference scorer
    MatchBand band = card.total() >= UPPER ? AUTO_MATCH
                   : card.total() >= LOWER ? REVIEW : NO_MATCH;
    MatchProposal p = proposals.saveAndFlush(new MatchProposal(UUID.randomUUID(), a.getId(), b.getId(),
        card.total(), card.breakdownJson(), LOWER, UPPER, band, Instant.now(clock)));
    if (band == MatchBand.AUTO_MATCH) {
        p.decide(ProposalStatus.CONFIRMED, "AUTO", Instant.now(clock));  // ✅ decidedBy=AUTO on the trail
        executeMerge(p, a, b);                                           //    — unattended, same trail
    }
    return p;
}

@Transactional
public MatchProposal confirm(UUID proposalId, String decider) {
    MatchProposal p = proposals.findByIdForUpdate(proposalId).orElseThrow(...);
    if (p.getStatus() != ProposalStatus.PROPOSED) throw LinkageException.alreadyDecided(); // 409
    if (p.getBand() == MatchBand.NO_MATCH) throw LinkageException.notConfirmable();        // 422
    LinkageRecord a = records.findByIdForUpdate(p.getLowRecordId()).orElseThrow(...);
    LinkageRecord b = records.findByIdForUpdate(p.getHighRecordId()).orElseThrow(...);
    requireActive(a); requireActive(b);                       // captured elsewhere meanwhile → 409
    p.decide(ProposalStatus.CONFIRMED, decider, Instant.now(clock));   // ✅ who/when
    executeMerge(p, a, b);
    return p;
}

private void executeMerge(MatchProposal p, LinkageRecord survivor, LinkageRecord loser) {
    // reference survivor choice: the LOW-id record — deterministic (audit-stable), data-arbitrary;
    // a fork may swap in most-complete/most-recent policies, recording theirs the same way
    for (Field f : IDENTITY_FIELDS) {                          // ✅ one decision PER FIELD
        Survivorship s = pickValue(survivor, loser, f);        //    (rule: survivor's non-blank wins,
        members.persist(new SurvivorshipDecision(UUID.randomUUID(), p.getId(), f.name(),
            s.value(), s.sourceRecordId(), s.rule(), Instant.now(clock)));  // else loser's — recorded)
        survivor.applySurvivorship(f, s.value());
    }
    loser.tombstone(survivor.getId());                         // ✅ MERGED + pointer — values retained
}
```

The ascending-id lock order is the deadlock guard; the proposal lock makes the decide-once 409 deterministic; re-checking both participants under their locks makes the merge-once 409 deterministic even when two proposals share a record (CWE-362). `resolve(id)` follows `mergedIntoId` chains with a visited-set so a (corruption-only) pointer loop errors instead of spinning. SurvivorshipDecision rows are `@AggregateMember` of the proposal — root-JPQL reads, `common/MemberWriter` writes.

Verification: review-tier — confirm the proposal row carries score/breakdown/thresholds; the band cut-points match the recorded thresholds; REVIEW mutates only via confirm/reject with who/when (AUTO trail identical, NO_MATCH unconfirmable); the merge appends per-field decisions, tombstones with a pointer, and NO delete path exists on records; locks are taken proposal-then-records in ascending-id order. The behavioural proofs a fork-receiver keeps green: the double-confirm race (exactly one 2xx) and the shared-participant race (at most one capture).

Reference: [Big Data and Social Science — Record Linkage](https://textbook.coleridgeinitiative.org/chap-link.html)

Reference: [Duplicate Medical Records (PMC2815491)](https://pmc.ncbi.nlm.nih.gov/articles/PMC2815491/)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
