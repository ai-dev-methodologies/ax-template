---
title: Post-trade settlement must commit its two legs ATOMICALLY (delivery occurs if and only if payment occurs), reach an IRREVOCABLE final state after which novation/cancel/amend are all refused, conserve the obligation across any pre-finality counterparty novation (recorded append-only), and walk the fail ladder with exactly-once transitions under a row lock that lets exactly one settle finalize
impact: HIGH
impactDescription: "Settling one leg without the other is principal risk — the Herstatt failure that destroys a counterparty (you delivered the securities and never got paid); allowing a 'cancel' or re-settle after finality unwinds a trade other parties already relied on as irrevocable; a novation that changes the obligation amount silently transfers value off the books; and a settle with no row lock lets two concurrent attempts double-settle or interleave a partial commit — every one of these is a real, money-losing post-trade defect that no amount of after-the-fact reconciliation can reverse"
tags:
  - state-machine
  - concurrency
  - conservation
  - finality
  - audit
spec_ref: "specs/settlement-finality-l0.yaml#SETTLE-DVP-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/settlement/SettlementService.java + backend/src/main/java/com/ax/template/authblueprint/settlement/SettlementInstruction.java + backend/src/main/java/com/ax/template/authblueprint/settlement/SettlementFailLadder.java"
  pattern: "A SettlementInstruction settles both legs in ONE transaction or neither (DvP: a @Check delivery_settled = payment_settled makes partial settlement unrepresentable, not just service logic); SETTLED is the irrevocable final state (@Check SETTLED implies both legs settled + a recorded final instant) after which novation/cancel/amend/re-settle and every fail-ladder edge are deterministic 409s; a pre-finality novation replaces ONE leg's counterparty while CONSERVING the net obligation (net_obligation is @Column(updatable=false); an append-only immutable NovationRecord records released party, assuming party, the conserved obligation, who/when); the fail ladder PENDING→FAILED→RETRY→BUYIN is walked by a state machine that is the sole status mutator on the fail path and rejects every off-graph edge (skip/reverse/repeat) 409; and every write-path takes the instruction's PESSIMISTIC_WRITE row lock so of N concurrent settles exactly one finalizes (CWE-362)"
upstream:
  - "https://www.bis.org/cpmi/publ/d00b.htm"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "BIS CPMI, 'A glossary of terms used in payments and settlement systems' — the canonical definition of delivery versus payment (the all-or-nothing link between the two transfers)"
    url: "https://www.bis.org/cpmi/publ/d00b.htm"
    quote: "a link between a securities transfer system and a funds transfer system that ensures that delivery occurs if, and only if, payment occurs."
    quoted_at: "2026-06-22"
  - source_type: external
    citation: "BIS CPMI, 'A glossary of terms used in payments and settlement systems' — final settlement (the irrevocability of finality) and novation (the obligation-conserving substitution of parties)"
    url: "https://www.bis.org/cpmi/publ/d00b.htm"
    quote: "final settlement: settlement which is irrevocable and unconditional. novation: satisfaction and discharge of existing contractual obligations by means of their replacement by new obligations (whose effect, for example, is to replace gross with net payment obligations). The parties to the new obligations may be the same as those to the existing obligations or, in the context of some clearing house arrangements, there may additionally be substitution of parties."
    quoted_at: "2026-06-22"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent settles racing one instruction toward finality)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-22"
---

## Settlement links its two legs all-or-nothing, locks in at finality, conserves the obligation through novation, and walks its fail ladder once each

**Impact: HIGH — settling one leg without the other is principal risk (Herstatt); a cancel after finality unwinds a trade others relied on; a novation that changes the amount moves value off the books; an unlocked settle double-commits.**

*Post-trade settlement* — turning an agreed trade into the actual exchange of securities for funds — is the moment value irreversibly changes hands. BIS CPMI defines the governing discipline: *delivery versus payment* is *"a link between a securities transfer system and a funds transfer system that ensures that delivery occurs if, and only if, payment occurs"*, and *final settlement* is *"settlement which is irrevocable and unconditional."* The catalog's adjacent primitives cover none of this (`collection-conservation-netting` reduces a SET of gross obligations to net — no two-leg atomic settlement, no finality instant; `reserve-settle-balance` is a quantity drawdown against a pooled prepaid balance — not a linked DvP transfer):

```text
settle(id):     BOTH legs commit in ONE tx or NEITHER (DvP) — partial is a @Check violation
                drives PENDING/FAILED/RETRY → SETTLED + records the finality instant
finality:       SETTLED is irrevocable — novation/cancel/amend/re-settle + every ladder edge → 409
novate(leg,p):  before finality, replace ONE leg's counterparty; net obligation CONSERVED
                (net_obligation @Column(updatable=false)); append-only immutable NovationRecord
fail ladder:    PENDING→FAILED→RETRY→BUYIN, each edge once; off-graph edge → 409; BUYIN terminal
locks:          every write-path takes the instruction's PESSIMISTIC_WRITE row lock (settle-once)
```

**1. Atomic DvP (SETTLE-DVP-001).** The two legs are not two operations — they are one. The instruction carries `delivery_settled` and `payment_settled` flags constrained by a DB `@Check (delivery_settled = payment_settled)`, so a row in which one leg settled and the other did not cannot be persisted at all. Settlement flips both and stamps finality in the same transaction.

**2. Finality is irrevocable (SETTLE-FINAL-001).** Once `SETTLED`, the trade is locked in: novation, cancel, amend, a second settle, and every fail-ladder edge are deterministic `409`s. The `@Check` backstops that `SETTLED` implies a recorded `final_at` and that a non-final instruction carries none — the finality instant is the auditable proof of irrevocability.

**3. Novation conserves the obligation (SETTLE-NOVATE-001).** Before finality a counterparty may be substituted — *"there may additionally be substitution of parties"* — but the obligation the new party assumes is identical to the one the old party was released from. `net_obligation` is `@Column(updatable=false)`, so a novation cannot drift it; an immutable append-only `NovationRecord` records the released party, the assuming party, the conserved obligation, and who/when. The original instruction is retained.

**Incorrect — settle one leg at a time, mutable after finality, novation that changes the amount, no lock:**

```java
public void settle(UUID id) {
    SettlementInstruction s = repo.findById(id).orElseThrow();   // ❌ no row lock — concurrent double-settle
    s.setDeliverySettled(true);                                  // ❌ one leg flips alone — partial DvP
    repo.save(s);
    chargeFunds(s);                                              // ❌ if THIS throws, delivery already stuck true
    s.setPaymentSettled(true);
    s.setStatus(SETTLED);                                        // ❌ no finality instant recorded
}
public void novate(UUID id, String newParty, BigDecimal newAmount) {
    SettlementInstruction s = repo.findById(id).orElseThrow();
    s.setPaymentParty(newParty);
    s.setNetObligation(newAmount);                              // ❌ obligation NOT conserved — value moved
}                                                              // ❌ nothing blocks novation after finality
```

**Correct — atomic two-leg commit under a row lock; irrevocable after finality; conserving append-only novation; exactly-once fail ladder:**

```java
@Transactional
public SettlementInstruction settle(UUID id) {
    SettlementInstruction s = instructions.findByIdForUpdate(id).orElseThrow(SettlementException::notFound);
    if (s.isFinal()) throw SettlementException.alreadyFinal();              // ✅ settle-once 409 under lock
    if (s.getStatus() == SettlementStatus.BUYIN) throw SettlementException.notSettleable(s.getStatus());
    s.settleBothLegs(Instant.now(clock));                                  // ✅ both legs + finality instant, one tx
    return s;
}

@Transactional
public SettlementInstruction novate(UUID id, SettlementLeg leg, String assumingParty, String novatedBy) {
    SettlementInstruction s = instructions.findByIdForUpdate(id).orElseThrow(SettlementException::notFound);
    if (s.isFinal()) throw SettlementException.alreadyFinal();              // ✅ irrevocable — counterparty locked in
    String released = leg == SettlementLeg.DELIVERY ? s.getDeliveryParty() : s.getPaymentParty();
    if (Objects.equals(released, assumingParty)) throw SettlementException.novationNoChange();   // 422
    BigDecimal conserved = s.getNetObligation();                           // ✅ net_obligation is updatable=false
    members.persist(new NovationRecord(UUID.randomUUID(), s.getId(), leg, released, assumingParty,
        conserved, novatedBy, Instant.now(clock)));                        // ✅ append-only, obligation conserved
    if (leg == SettlementLeg.DELIVERY) s.replaceDeliveryParty(assumingParty);
    else s.replacePaymentParty(assumingParty);
    return s;
}
```

```java
// the fail ladder is the SOLE status mutator on the fail path (HG-STATE-SOLE-MUTATOR)
public void advance(SettlementInstruction instruction, SettlementStatus next) {
    SettlementStatus from = instruction.getStatus();
    if (!ALLOWED.getOrDefault(from, Set.of()).contains(next)) {            // PENDING→FAILED→RETRY→BUYIN
        throw SettlementException.illegalLadderEdge(from, next);           // ✅ off-graph edge → 409
    }
    instruction.moveStatus(next);                                         // package-private hook
}
```

**4. The fail ladder is walked once each (SETTLE-LADDER-001).** A failed settlement walks `PENDING→FAILED→RETRY→BUYIN`; `SettlementFailLadder` is the only mutator of `status` on the fail path and rejects any off-graph edge (skip/reverse/repeat) with a `409`. `BUYIN` is terminal-failed (zero outgoing edges). A non-final instruction on the ladder may still recover to finality via an explicit settle.

**5. Exactly one settle finalizes (SETTLE-CONCURRENT-001).** Every write-path takes the instruction's `PESSIMISTIC_WRITE` row lock. Of N concurrent settles, exactly one finds the instruction non-final and commits the DvP; the rest see `SETTLED` and get `409` — *"a timing window exists in which the shared resource can be modified by another code sequence operating concurrently"* is closed by the lock. The DvP commit and finality stamp are in the same transaction under that lock, so a partial or double settlement is impossible.

Verification: review-tier — confirm settle flips both legs in one transaction and the `@Check` makes partial unrepresentable on both entity and migration; `SETTLED` implies a recorded `final_at` and every mutating verb after finality returns `409`; novation reads `net_obligation` (which is `updatable=false`) into the `NovationRecord` so the amount cannot drift, and the record is fully `@Column(updatable=false)`; the fail ladder rejects off-graph edges; and all write-paths use `findByIdForUpdate`. The behavioural proof a fork-receiver keeps green: the concurrent-settle race (exactly one 2xx, the rest 409).

Reference: [BIS CPMI — A glossary of terms used in payments and settlement systems](https://www.bis.org/cpmi/publ/d00b.htm)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
