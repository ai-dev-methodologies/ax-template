---
title: When an anonymous principal authenticates for the first time, the records it accreted while anonymous (cart / order / draft / wishlist) MUST be CLAIMED by the now-authenticated identity atomically and idempotently — transferred exactly once across all N records, a replayed or concurrent claim a no-op — and the claim MUST refuse any record already owned by a different registered principal via a structural compare-and-set (owner IS NULL), never a check-then-act pre-check that races (CWE-367)
impact: HIGH
impactDescription: "Guest checkout, wishlist-before-signup, draft-before-register: an anonymous principal accretes records, then authenticates. If the claim is non-atomic, a crash mid-claim strands half the cart (the user sees some items, loses the rest). If it is not idempotent, a concurrent first-login from two devices — or an at-least-once-delivered claim event — transfers the records twice (duplicate orders) or loses them. Worst, if the already-owned guard is a check-then-act pre-check (read isRegistered, then setCustomer), the ownership can change between the check and the set (CWE-367 TOCTOU) so a guest-email collision claims another registered customer's order onto the wrong account — a cross-principal data leak. Claiming all matching unclaimed records in one transaction, keyed for idempotency, with an atomic compare-and-set that updates ONLY rows whose owner is still null, makes the strand, the double-claim, and the cross-principal claim all unrepresentable."
tags:
  - identity
  - data-integrity
  - concurrency
  - e-commerce
spec_ref: "specs/identity-claim-on-auth-l0.yaml#IDCLAIM-GUARD-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/identityclaim/IdentityClaimService.java"
  pattern: "Anonymous records carry a nullable owner_user_id + an anonymous claim key (session id / guest email). On first authentication a sole-claim service, in ONE @Transactional, transfers every matching record via an atomic conditional UPDATE … SET owner_user_id = :user WHERE claim_key = :key AND owner_user_id IS NULL (compare-and-set) — so a record already owned by anyone else is unclaimable by construction (not by a racy isRegistered pre-check, the CWE-367 TOCTOU); the claim is idempotent (re-claim hits zero null rows → no-op, same outcome); @Version guards the row; the claimed record is thereafter user-scoped (order-l0 ORDER-AUTHZ-001). NEVER a read-then-write guard that can race between the check and the set."
upstream:
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/registration/MergeOrdersByEmailPostRegistrationObserver.java"
  - "https://cwe.mitre.org/data/definitions/367.html"
evidence:
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) MergeOrdersByEmailPostRegistrationObserver — the anonymous-order claim-on-registration absorbed: anonymous orders found by the registering customer's email are reassigned to the new customer"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/registration/MergeOrdersByEmailPostRegistrationObserver.java"
    quote: "o.setCustomer(customer);"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) MergeOrdersByEmailPostRegistrationObserver — the already-registered guard absorbed-and-STRENGTHENED: Broadleaf gates the claim with a check-then-act `!isRegistered()` pre-check (a CWE-367 TOCTOU); ax replaces it with an atomic compare-and-set on owner IS NULL"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/registration/MergeOrdersByEmailPostRegistrationObserver.java"
    quote: "if (!o.getCustomer().isRegistered()) {"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "MITRE CWE-367 (Time-of-check Time-of-use TOCTOU Race Condition) — the weakness Broadleaf's check-then-act claim guard exhibits and the atomic compare-and-set absorbed here makes unrepresentable"
    url: "https://cwe.mitre.org/data/definitions/367.html"
    quote: "The product checks the state of a resource before using that resource, but the resource's state can change between the check and the use in a way that invalidates the results of the check."
    quoted_at: "2026-06-25"
---

## Rule

A **guest** accretes records — a cart, an order, a wishlist, a draft — keyed by a session id or a guest email, while **anonymous**. When they **authenticate for the first time** (login or register), those records must be **claimed** by the now-authenticated identity. Per Broadleaf, `MergeOrdersByEmailPostRegistrationObserver` finds anonymous orders by email and `o.setCustomer(customer)`. Three obligations the naive observer does not guarantee:

1. **Atomic claim (IDCLAIM-CLAIM-001).** All N matching unclaimed records transfer in **one transaction** — a crash mid-claim cannot strand half the cart. The claim sets `owner_user_id` from null to the authenticated user; the records are thereafter user-scoped (`order-l0` ORDER-AUTHZ-001).
2. **Idempotent claim (IDCLAIM-IDEMPOTENT-001).** A replayed claim, or a concurrent first-login from a second device, is a **no-op** — the conditional update hits zero still-null rows the second time. Never a duplicate transfer, never a lost record. Composes `idempotency-l0`.
3. **Already-owned guard via compare-and-set (IDCLAIM-GUARD-001).** A record already owned by a **different** registered principal MUST NOT be claimed by a guest-email collision. Broadleaf's `if (!o.getCustomer().isRegistered())` is a **check-then-act** pre-check — a **CWE-367 TOCTOU**: the ownership can change between the read and the `setCustomer`. ax **strengthens** it: the claim is a single atomic conditional `UPDATE … SET owner_user_id = :user WHERE claim_key = :key AND owner_user_id IS NULL` (compare-and-set), so an already-owned record is **unclaimable by construction**, not by a racy pre-check.

**Correct — atomic compare-and-set claim (idempotent, guarded by construction):**

```java
// backend/.../identityclaim/IdentityClaimService.java — claim all unclaimed records for this key, in one tx
@Transactional
public ClaimResult claimOnFirstAuth(String claimKey, UUID userId) {
    // atomic compare-and-set: transfers ONLY rows still unclaimed — owner IS NULL.
    // already-owned (by anyone) → not matched → not claimed (no CWE-367 read-then-write race).
    int claimed = records.claimUnowned(claimKey, userId);   // UPDATE ... SET owner_user_id=:u WHERE claim_key=:k AND owner_user_id IS NULL
    return new ClaimResult(claimed);                         // idempotent: a replay matches 0 null rows → claimed=0, same outcome
}
```
```sql
-- the compare-and-set: WHERE owner_user_id IS NULL is the structural guard
UPDATE claimable_records SET owner_user_id = :userId
 WHERE claim_key = :claimKey AND owner_user_id IS NULL;
```

**Incorrect — read-then-write guard (CWE-367 race) + non-atomic per-row loop:**

```java
for (Record r : records.findByKey(claimKey)) {     // WRONG: not one atomic statement; partial on crash
    if (r.getOwner() == null || !r.getOwner().isRegistered()) {  // WRONG: check-then-act — owner can change before save
        r.setOwner(userId);                         // CWE-367 TOCTOU: claims a record another principal just took
        records.save(r);                            // WRONG: no idempotency key → replay double-claims
    }
}
```

The read-then-write guard races (a concurrent claim flips ownership between the `if` and the `save`); the per-row loop strands records on a mid-loop crash; no idempotency key lets an at-least-once-delivered claim run twice. The single atomic compare-and-set on `owner_user_id IS NULL`, in one transaction, makes the strand, the double-claim, and the cross-principal claim unrepresentable.

Reference: [Broadleaf MergeOrdersByEmailPostRegistrationObserver (anonymous-order claim + the check-then-act guard)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/registration/MergeOrdersByEmailPostRegistrationObserver.java)

Reference: [MITRE CWE-367 — TOCTOU Race Condition (the weakness the compare-and-set eliminates)](https://cwe.mitre.org/data/definitions/367.html)
