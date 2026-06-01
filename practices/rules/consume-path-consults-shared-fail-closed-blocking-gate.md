---
title: Consuming a referenced entity MUST consult one shared fail-closed blocking-status gate, re-read in-transaction
impact: HIGH
impactDescription: "An operation that ships, transacts, authorizes, publishes, or forwards a referenced entity off a stale not-blocked snapshot — or re-implements the blocking check per call site — eventually consumes an entity that was held / suspended / frozen / embargoed / deprecated / quarantined after the snapshot, releasing a blocked lot, charging a frozen card, or publishing an embargoed document"
tags:
  - eligibility-gate
  - fail-closed
  - blocking-status
  - quality-hold
  - account-suspension
  - embargo
  - default-deny
  - access-control
spec_ref: "specs/blocking-status-gate-l0.yaml#GATE-CONSULT-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/common/ConsentGate.java"
  pattern: "A single shared gate component (sibling of the fail-closed common/ConsentGate and dsr/DsrRestrictionGate) exposes one requireNotBlocked(entityRef)/checkClearToConsume entrypoint that every consume/forward service calls; the gate re-reads the referenced entity's blocking status inside the same @Transactional consume method (not from a request-scoped snapshot or client payload), defaults to BLOCKED on lookup failure / unknown / null status (try/catch returns blocked=true), and no consume entrypoint reaches the forward/consume sink without a preceding gate call"
upstream:
  - "https://owasp.org/www-project-application-security-verification-standard/"
  - "https://owasp.org/www-community/Fail_securely"
evidence:
  - source_type: external
    citation: "OWASP ASVS 4.0 — V4.1 General Access Control Design, requirement 4.1.5"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/master/4.0/en/0x12-V4-Access-Control.md"
    quote: "Verify that access controls fail securely including when an exception occurs."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "OWASP — Fail securely (secure-design principle, default-deny on failure)"
    url: "https://owasp.org/www-community/Fail_securely"
    quote: "In general, you should design your security mechanism so that a failure will follow the same execution path as disallowing the operation."
    quoted_at: "2026-06-01"
---

## Consuming a referenced entity MUST consult one shared fail-closed blocking-status gate, re-read in-transaction

**Impact: HIGH — a stale not-blocked snapshot, or a per-call-site check, eventually forwards an entity that was blocked in the window**

Many mutations do not just edit their own row — they CONSUME or FORWARD a *referenced* entity A: shipping picks a lot, a charge draws on a card, a publish releases a document, an outbound call targets an API version, a production step consumes an input batch. Each of those referenced entities can be independently put into a blocking state by a *different* actor at a *different* time: a quality engineer places a lot on hold, risk freezes a card, compliance embargoes a document, a vendor deprecates an API version, QA quarantines an input. The danger is the window: the consuming operation was composed (and A's status read) at time t1, but A is blocked at t2, and the operation commits at t3. If the consume trusts the t1 snapshot, it releases a held lot or charges a frozen card at t3 — exactly the action the block existed to stop.

Two failure shapes recur, and both are structural rather than a one-off bug.

First, **stale-read**: the status is read once early in the request (or trusted from the client payload) and not re-read at the decision point. The fix is to re-read A's authoritative blocking status *inside the same transaction* as the consuming write, so the operation observes A's status as of its own commit point. This is the same discipline the optimistic-lock and reparent-acyclicity rules apply: the guard runs under the write's transaction, not before it.

Second, **fail-open on the unknown path**: the status lookup throws / times out / returns a status the code does not recognise, and the `catch` (or a missing `else`) lets the consume proceed. OWASP ASVS 4.1.5 is explicit — *access controls fail securely including when an exception occurs* — and the Fail Securely principle states the rule operationally: *a failure should follow the same execution path as disallowing the operation*. A blocking status is an eligibility control; "I could not prove A is unblocked" must resolve to *blocked*, never *allowed*. An unreachable status source that defaults to allow forwards a possibly-blocked entity precisely when the system is degraded — the worst possible time.

The third property ties the first two together: there must be **exactly one shared gate component**, not a hand-rolled check per call site. A per-site reimplementation guarantees that one path eventually omits the re-read or the fail-closed default — and the omitted path is the breach. One gate is the single source of truth for what "blocked" means, for the in-transaction re-read, and for the default-deny, so those properties hold uniformly instead of drifting. This repo already ships this exact shape twice: `common/ConsentGate` (one derivation, absence-is-no-consent) and `dsr/DsrRestrictionGate` / the legal-hold gate (one component both delete paths consult, fail-closed on a registry timeout). This rule is the consumption-axis sibling.

When A is blocked the consume MUST be rejected before any state change — 409 Conflict (incompatible state) or 423 Locked (explicit hold) with an RFC 9457 body of `type=urn:problem:entity-blocked` naming the entity ref, the block reason, and the earliest-eligible time — never a silent skip and never a partial apply.

**Incorrect — status read from a stale snapshot, no shared gate, and the lookup failure falls through to a consume:**

```java
// ShippingService — trusts a status captured earlier in the request
@Transactional
void shipLot(ShipCommand cmd) {
    // cmd.lotStatus was read when the pick list was built minutes ago
    if ("RELEASED".equals(cmd.lotStatus())) {          // ❌ stale snapshot; lot may be on hold NOW
        carrier.dispatch(cmd.lotId());                 // ❌ ships a held lot
    }
}

// ChargeService — a SECOND consume path with its own ad-hoc check
@Transactional
void charge(ChargeCommand cmd) {
    boolean frozen = false;
    try {
        frozen = cardClient.status(cmd.cardId()) == FROZEN;
    } catch (RuntimeException e) {
        // ❌ fail-OPEN: lookup failed, frozen stays false, charge proceeds
    }
    if (!frozen) gateway.capture(cmd.amount(), cmd.cardId());  // ❌ charges a possibly-frozen card on a timeout
}
```

**Correct — one shared fail-closed gate, re-read in-transaction, every consume path routes through it:**

```java
@Component
public class BlockingStatusGate {

    private final BlockingStatusSource source;   // lot holds, card freezes, embargoes, deprecations

    public BlockingStatusGate(BlockingStatusSource source) {
        this.source = source;
    }

    /** True ONLY when we have affirmatively established the entity is not blocked. */
    public boolean isBlocked(EntityRef ref) {
        try {
            BlockingStatus s = source.currentStatus(ref);   // authoritative re-read
            // fail-closed: an unrecognised / null status is NOT a license to consume
            return s == null || s.isBlocked() || s == BlockingStatus.UNKNOWN;
        } catch (RuntimeException lookupFailed) {
            return true;                                     // ✅ unreachable source ⇒ assume blocked
        }
    }

    /** Sole guard every consume/forward path calls, INSIDE the consuming @Transactional method. */
    public void requireNotBlocked(EntityRef ref) {
        if (isBlocked(ref)) {
            throw EntityBlockedException.of(ref);   // → 409/423 + urn:problem:entity-blocked
        }
    }
}

@Transactional
void shipLot(ShipCommand cmd) {
    blockingStatusGate.requireNotBlocked(EntityRef.lot(cmd.lotId()));  // ✅ re-read in-tx, fail-closed
    carrier.dispatch(cmd.lotId());
}

@Transactional
void charge(ChargeCommand cmd) {
    blockingStatusGate.requireNotBlocked(EntityRef.card(cmd.cardId()));  // ✅ SAME gate, no second check
    gateway.capture(cmd.amount(), cmd.cardId());
}
```

The gate is the single place the blocking decision lives; every consume path is physically unable to forward a blocked entity because the call is on the only route to the sink. Fail-closed on an unknown or unreachable status means a degraded system over-blocks (recoverable: the operation retries when the source is healthy) rather than over-consumes (irreversible: a shipped held lot, a charged frozen card). This mirrors `dsr/DsrRestrictionGate` (Art 18 restriction), the legal-hold gate (destruction axis), and `common/ConsentGate` (purpose gate): when in doubt, the safe default is *do not act*. Note the distinct axis — the legal-hold gate stops DESTROYING the held entity; this gate stops CONSUMING a separate blocked entity that an operation references.

Verification (review-tier): confirm a single `BlockingStatusGate`-style component exists (sibling of `common/ConsentGate`), that every consume/forward service calls it inside the same transaction as the write (no `dispatch`/`capture`/`publish`/`forward` sink reachable without a preceding `requireNotBlocked`), that the status lookup re-reads the source at the decision point rather than trusting a snapshot or client field, and that a lookup throw / null / unknown status returns *blocked*. A fork-receiver with a concrete `BlockingStatusSource` adds a RestAssured negative test: block entity A → consume → 409/423 with the `entity-blocked` problem type and A's consumers unchanged; and a source-down simulation → the consume is rejected, never applied.

Reference: [OWASP ASVS 4.0 — V4.1.5 access controls fail securely](https://owasp.org/www-project-application-security-verification-standard/)

Reference: [OWASP — Fail securely (default-deny on failure)](https://owasp.org/www-community/Fail_securely)
