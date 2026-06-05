---
title: A tamper-evident log MUST anchor its chain head to a sink outside the log owner's unilateral write control
impact: HIGH
impactDescription: "A hash-chained log proves no entry was altered WITHOUT detection — but only against an attacker who cannot also rewrite the chain. An owner who controls both the log and its hashes can re-hash the whole chain after editing an entry and the tamper is invisible. Anchoring the chain head to an external sink the owner cannot unilaterally rewrite (a detached signature, a trusted timestamp, an external log) is what makes the log evidence against the owner, not just against an outsider."
tags:
  - tamper-evident-log
  - hash-chain
  - external-anchor
  - non-repudiation
  - integrity
spec_ref: "specs/tamper-evident-log-l0.yaml#TAMPER-ANCHOR-001"
verification:
  type: review
  source: "specs/tamper-evident-log-l0.yaml#TAMPER-ANCHOR-001"
  pattern: "The current chain head (the latest entry's chain hash, optionally a signed tree head over the whole log) MUST be periodically committed to a sink OUTSIDE the log owner's unilateral write control, via a pluggable TamperEvidentAnchor SPI. Acceptable anchors: a detached signature produced by a key the log-writing service cannot itself rotate/forge, an RFC 3161 trusted timestamp over the head, an append to an external/third-party log, or publication to a write-once medium. The anchor records the head value + the anchoring time so a later verification can prove the head existed at that time and detect any retroactive rewrite of the chain. Reject a tamper-evident log whose ONLY integrity proof is a chain the log owner can fully recompute (no external anchor), and an anchor key the log service can itself rotate."
upstream:
  - "https://www.rfc-editor.org/rfc/rfc3161"
  - "https://www.rfc-editor.org/rfc/rfc6962"
evidence:
  - source_type: external
    citation: "RFC 3161 — Time-Stamp Protocol (TSP) (Section 1)"
    url: "https://www.rfc-editor.org/rfc/rfc3161"
    quote: "A time-stamping service supports assertions of proof that a datum existed before a particular time."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A tamper-evident log MUST anchor its chain head to a sink outside the owner's unilateral write control

**Impact: HIGH — A hash-chained log (each entry's hash includes the prior hash) detects any after-the-fact edit, because changing one entry breaks every downstream hash. But the chain only protects against an attacker who CANNOT recompute it. The log's own owner can: edit an entry, re-hash the whole chain, and the tampering is invisible — the log is evidence against outsiders but not against the owner. The fix is to anchor the chain head externally. A trusted timestamp is exactly such an anchor — per RFC 3161, *a time-stamping service supports assertions of proof that a datum existed before a particular time* — so once the head at time T is anchored, the owner cannot retroactively present a different chain for the period before T without contradicting the external anchor. The `tamper-evident-log-hashchain` rule builds the chain; THIS rule makes it trustworthy against the owner.**

There is one load-bearing requirement for `TAMPER-ANCHOR-001`.

**External anchor of the chain head.** The current head (latest chain hash, or a signed tree head over the whole log) is periodically committed to a sink OUTSIDE the log owner's unilateral control, through a pluggable `TamperEvidentAnchor` SPI. Acceptable anchors:
- a **detached signature** produced by a key the log-writing service cannot itself rotate or forge (e.g. an HSM / external signer);
- an **RFC 3161 trusted timestamp** over the head value;
- an **append to an external / third-party log** (a transparency log, RFC 6962-style);
- **publication to a write-once medium**.

The anchor records `{head_value, anchored_at}`. Verification recomputes the chain and checks the head against the anchored values: a retroactive rewrite changes the head and contradicts the anchor, exposing the tamper.

**Incorrect — chain only; the owner can re-hash after editing and nobody can tell:**

```java
void append(LogEntry e) {
    e.setPrevHash(lastHash);
    e.setHash(sha256(lastHash + e.payload()));   // chain only — owner can recompute the WHOLE chain after an edit
    logRepo.save(e); lastHash = e.getHash();
    // VIOLATION: head is never anchored externally → tampering by the owner is undetectable (TAMPER-ANCHOR-001)
}
```

**Correct — periodic external anchoring of the head via the SPI:**

```java
interface TamperEvidentAnchor { AnchorReceipt anchor(byte[] head, Instant at); } // pluggable: TSA / signer / ext log

@Scheduled(fixedDelay = ANCHOR_INTERVAL)
void anchorHead() {
    byte[] head = currentChainHead();
    AnchorReceipt r = anchor.anchor(head, clock.now());  // RFC 3161 timestamp / detached sig / external log
    anchorReceipts.save(r);                              // {head_value, anchored_at, receipt} (TAMPER-ANCHOR-001)
}
// verify(): recompute chain head, assert it matches the anchored receipts → detects any owner rewrite.
```

Verification: review-tier. Anchoring is an against-the-owner integrity property with no compile-time signal — a chain-only log compiles and detects outsider edits while remaining forgeable by the owner. Verify by review against `specs/tamper-evident-log-l0.yaml#TAMPER-ANCHOR-001`: the head is periodically committed to an external sink via the anchor SPI; the anchor key is outside the log service's rotation control; the anchor records head+time; verification checks the recomputed head against the anchor. When a fork-receiver wires a real IT (anchored head; an edited entry fails verification against the anchor), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [RFC 3161 — Time-Stamp Protocol](https://www.rfc-editor.org/rfc/rfc3161)

Reference: [RFC 6962 — Certificate Transparency (external transparency log)](https://www.rfc-editor.org/rfc/rfc6962)
