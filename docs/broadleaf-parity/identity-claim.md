# Broadleaf-absorption parity — identity-claim-on-auth [completeness-sweep RESIDUE]

- vertical: identity-claim
- broadleaf_source: core/.../registration/MergeOrdersByEmailPostRegistrationObserver.java:64-65 (claim anonymous orders by email on registration; check-then-act `!isRegistered()` guard)
- spec_items: IDCLAIM-CLAIM-001, IDCLAIM-IDEMPOTENT-001, IDCLAIM-GUARD-001
- rule: practices/rules/identity-claim-on-auth-atomic-idempotent-guarded.md
- behavioral_test: backend/src/test/java/com/ax/template/authblueprint/identityclaim/IdentityClaimComplianceTest.java
- adversarial_review: opus critic ACCEPT-WITH-RESERVATIONS → 2 MINOR fixed (scoped inspection GET — closed an IDOR + routed through service to fix a controller→repository layering break; @Modifying flags). codex final review flagged the claimKey↔caller BINDING gap (an arbitrary caller-supplied key could claim another guest's unclaimed records) → reconciled spec+rule: the claim key is an UNGUESSABLE possession token (high-entropy, not a guessable email), so possession IS the binding — an ax STRENGTHENING over Broadleaf's guessable findOrdersByEmail. All 3 invariants + atomic CAS HELD (CWE-367 eliminated by construction; idempotent replay=0; guard test genuine A/B; anti-re-find justified; 3 quotes byte-accurate; no-hand-edit proven).

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
