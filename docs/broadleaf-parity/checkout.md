# Broadleaf-absorption parity — checkout (saga register-before-act)

- vertical: checkout
- broadleaf_source: core/.../checkout/service/workflow/DecrementInventoryActivity.java:71; workflow/SequenceProcessor.java:117; workflow/state/ActivityStateManagerImpl.java:121
- spec_items: SAGA-COMPENSATE-002
- rule: practices/rules/saga-compensating-transactions.md
- behavioral_test: REVIEW-TIER (saga-orchestration-l0 is a review-tier spec — no backend domain; the invariant is enforced by the rule + the register-before-act ordering, like the rest of the saga spec)
- violation_proof: REVIEW-TIER (saga-orchestration-l0 has no backend domain; the register-before-act invariant is structurally enforced by the rule example + ordering, not a runtime entity)
- adversarial_review: REVISE→fixed (MAJOR: the Correct code example demonstrated register-AFTER-act — the exact anti-pattern; rewrote to register-before-act keyed on sagaKey; + 2 MINOR pivot-last re-find wording + atomic-tx topology + compensate-of-no-op cross-ref; + corrected stale "no transactional-outbox spec exists" claim)

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| a step registers its compensation BEFORE the irreversible side effect | rule + example: registerCompensation(sagaKey) precedes the effecting call (review-tier) |
| reverse-order rollback only undoes legs whose compensation was registered | rule: a register-after-act crash window orphans the leg (made unrepresentable) |
| the non-compensatable pivot is ordered last (existing SAGA-COMPENSATE-001) | RE-FIND pointer: pivot-last is SAGA-COMPENSATE-001, not re-claimed as new |
| compensate-of-no-op is safe (registered-but-not-acted) | cross-ref SAGA-IDEMPOTENT-001 (compensate-of-no-op = no-op) makes register-before-act sound |

> Checkout was ~90% re-find: availability → two-axis-inventory-reservation-l0; payment-confirm →
> in-doubt-outbound-call-l0 + payment; tax/total → pricing-l0; reverse-order rollback →
> SAGA-COMPENSATE-001; idempotent placement → order-l0 + idempotency-l0. The one genuine
> residue (register-before-act) was absorbed as SAGA-COMPENSATE-002 — no hollow vertical.
