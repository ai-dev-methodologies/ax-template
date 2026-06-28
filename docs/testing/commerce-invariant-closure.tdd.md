# TDD Evidence Report — commerce-invariant-closure

**Branch:** `feat/commerce-invariant-closure` · **Date:** 2026-06-28
**Scope:** 4 generic catalog invariants closing the remaining Broadleaf-absorption re-audit gaps (P1-56~59) + a doc honesty fix (P1-60). All built spec → evidence-rule → independent impl → behavioral test + ViolationProof, each verified GREEN before integration. Zero external-product source ported; rules anchored to CWE / RFC / ISO / OWASP.

> Discipline note (honest): each domain was built test-alongside-impl by a delegated executor, then driven to GREEN per-domain. The verification proof is the pair {behavioral ComplianceTest asserting the invariant's real round-trip behavior, structural ViolationProofTest asserting the invariant is unrepresentable-to-violate}. ViolationProof is the falsification gate — it fails if the structural guarantee is removed. Non-vacuity is further checked by the G004 adversarial opus review.

## User journeys (invariant guarantees)

1. As a merchandiser, I declare a conditional offer so that a target item is discounted ONLY when qualifier items meet a minimum quantity AND the customer is eligible — otherwise the offer is silently not-applied (fail-closed).
2. As a billing operator, I re-price an order any number of times so that there is always exactly ONE tax record == the correct tax, and a tax-exempt customer/line yields zero tax.
3. As a platform, I forbid silent cross-currency arithmetic so that adding amounts of different currencies without a recorded conversion fails closed (422), never silently coerces.
4. As a user, when I reset my password every outstanding reset token of mine is invalidated so that a leaked second token cannot be reused.

## Task report

| Domain (gap) | Guarantee verified | Validation command | RED/GREEN evidence |
|---|---|---|---|
| offereligibility (P1-56) | qualifier-minqty gate, customer/segment eligibility, fail-closed-on-missing | `./gradlew testOfferEligibility` | GREEN 14/14 (7 Compliance + 7 ViolationProof) |
| taxapplication (P1-57) | exempt-skip → 0 tax, idempotent recompute → exactly 1 row, now-exempt removes prior row | `./gradlew testTaxApplication` | GREEN 14/14 (7 + 7) |
| currencyarithmetic (P1-58) | cross-currency add/subtract → 422 + balance unchanged, same-ccy exact, explicit conversion allowed | `./gradlew testCurrencyArithmetic` | GREEN 14/14 (6 + 8) |
| auth token-family (P1-59) | reset with token1 invalidates the whole family → token2 rejected | `./gradlew testAsvs` | GREEN 41/41 (incl. `passwordReset_successInvalidatesEntireTokenFamily`) |

## Test specification (human-readable guarantees)

| # | What is guaranteed | Test | Type | Result |
|---|---|---|---|---|
| 1 | A BOGO target is NOT discounted when qualifier qty < min (HTTP 200, not-applied) | `OfferEligibilityComplianceTest` | integration | PASS |
| 2 | A non-eligible customer receives the offer as not-applied; eligible applies | `OfferEligibilityComplianceTest` | integration | PASS |
| 3 | Missing/unresolvable criteria ⇒ fail-closed not-applied (deny-by-default) | `OfferEligibilityComplianceTest` + ViolationProof | integration+structural | PASS |
| 4 | Re-pricing twice converges to exactly ONE tax row, same id, correct amount | `TaxApplicationComplianceTest` | integration | PASS |
| 5 | Tax-exempt customer ⇒ 0 tax; an exempt line contributes 0 | `TaxApplicationComplianceTest` | integration | PASS |
| 6 | A now-exempt re-price REMOVES the prior tax row (no stranding); UNIQUE(order_id) makes a 2nd row unrepresentable | `TaxApplicationComplianceTest` + ViolationProof | integration+structural | PASS |
| 7 | `CurrencyMoney.plus/minus` across currencies throws CURRENCY_MISMATCH (422) before any value/persist | `CurrencyArithmeticComplianceTest` + ViolationProof | integration+structural | PASS |
| 8 | Same-currency arithmetic returns the exact integer result; explicit recorded conversion is the only cross-ccy path | `CurrencyArithmeticComplianceTest` | integration | PASS |
| 9 | On reset success, ALL the user's unused reset tokens are invalidated (token2 rejected after token1 reset) | `PasswordResetAsvsTest.passwordReset_successInvalidatesEntireTokenFamily` | integration | PASS |

## Coverage and known gaps

- Coverage mechanism = per-domain `./gradlew test{Domain}` (binary pass/fail, the ax-template canonical gate) + each domain's ViolationProofTest (structural falsification). Each invariant's happy path, fail-closed/edge path, and structural-unrepresentability are all asserted.
- Known gap (honest): `auth token-family` reuses the existing auth ASVS suite's MockMvc black-box style (not RestAssured) — surgical match to local style; flagged for the G004 adversarial review.
- All 4 run inside the full R25 per-domain suite (G004) + run-all-guards (109/0 at integration).
