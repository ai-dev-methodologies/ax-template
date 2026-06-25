# Broadleaf-absorption parity — identity-claim-on-auth [completeness-sweep RESIDUE]

- vertical: identity-claim
- broadleaf_source: core/.../registration/MergeOrdersByEmailPostRegistrationObserver.java:64-65 (claim anonymous orders by email on registration; check-then-act `!isRegistered()` guard)
- spec_items: IDCLAIM-CLAIM-001, IDCLAIM-IDEMPOTENT-001, IDCLAIM-GUARD-001
- rule: practices/rules/identity-claim-on-auth-atomic-idempotent-guarded.md
- behavioral_test: backend/src/test/java/com/ax/template/authblueprint/identityclaim/IdentityClaimComplianceTest.java
- adversarial_review: ACCEPT-WITH-RESERVATIONS → 2 MINOR fixed. All 3 invariants + atomic CAS HELD (no read-then-write race / CWE-367 eliminated by construction; idempotent replay = 0 rows; guard test genuine A/B principal ids; anti-re-find justified vs ownership-transfer[inverse]/record-linkage[probabilistic]; 3 evidence quotes byte-accurate; no-hand-edit proven). MINOR fixed: scoped the inspection GET (caller-owned / unclaimed only — closed the IDOR in a leak-prevention domain) + @Modifying consistency.

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| anonymous orders are reassigned to the registering customer | IdentityClaimComplianceTest CLAIM: 2 anonymous records keyed to guest email → A claims → count 2, both owned by A |
| (ax STRENGTHENING) the claim is idempotent | IDEMPOTENT: replay claim → count 0, no duplicate, still 2 owned by A |
| (ax STRENGTHENING, hardens Broadleaf's check-then-act) a record already owned by another principal is not claimed | GUARD: record owned by A → B's claim same key → count 0, A retains (atomic CAS WHERE owner IS NULL, not a CWE-367 pre-check) |

> RESIDUE of the Phase-2 completeness sweep: the single genuine new invariant beyond the 7 ultragoal
> verticals. ax strengthens Broadleaf's non-transactional check-then-act (`!isRegistered()`, CWE-367 TOCTOU)
> into an atomic compare-and-set on `owner_user_id IS NULL` (the promotion-l0 "absorb + harden the racy
> guard" precedent). Distinct from ownership-transfer-l0 (inverse direction) + record-linkage-l0 (probabilistic).
