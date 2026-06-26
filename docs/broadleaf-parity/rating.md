# Broadleaf-absorption parity — rating (derived-aggregate consistency) [G007]

- vertical: rating
- broadleaf_source: core/.../rating/domain/RatingSummaryImpl.java:124,117; rating/dao/RatingSummaryDaoImpl.java:88; rating/service/type/ReviewStatusType.java:33
- spec_items: DERIVED-AGG-CONSISTENCY-001, DERIVED-AGG-ELIGIBILITY-001, DERIVED-AGG-EMPTY-001
- rule: practices/rules/derived-aggregate-consistency-recompute-eligibility-empty.md
- behavioral_test: backend/src/test/java/com/ax/template/authblueprint/ratingsummary/RatingSummaryComplianceTest.java
- violation_proof: backend/src/test/java/com/ax/template/authblueprint/ratingsummary/RatingSummaryViolationProofTest.java
- adversarial_review: ACCEPT-WITH-RESERVATIONS → M1 fixed. All correctness vectors REFUTED (cross-derivation genuinely independent JPQL AVG vs in-memory fold; no recompute race — lock-before-read + idempotent; lazy-create safe; empty sentinel no Σ/0; no public aggregate setter proven; 4 evidence quotes byte-verified; eligibility honestly labeled ax-strengthening). M1 (HIGH honesty gap): novelty omitted the closest neighbor denormalized-counter-reconcilable → fixed (added the synchronous-recompute vs async-increment+reconcile distinction to spec scope + rule).

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| averageRating is recomputed from the rating rows (Σ/count), never hand-set | RatingSummaryComplianceTest CONSISTENCY: stored average == 4.00 from (5,3,4); == independent repo AVG query (cross-derivation) |
| the average updates when a rating row changes | add another approved review → average recomputed |
| empty rating set returns 0.0, never divide-by-zero | EMPTY: zero approved → average 0.00, reviewCount 0; reject last → sentinel restored |
| (ax STRENGTHENING — not Broadleaf parity) only eligible rows count | ELIGIBILITY: PENDING review → average unchanged; approve → recomputed to include it |

> Broadleaf NOTE: resetAverageRating averages ALL RatingDetail rows with NO status predicate (only review
> TEXT is moderated). The eligibility-filtered aggregate (DERIVED-AGG-ELIGIBILITY-001) is an ax STRENGTHENING,
> labeled as such in the spec + rule — NOT claimed as Broadleaf parity.

> G007 RE-FIND (no new items): search → RE-FIND search-l0 (SEARCH-QUERY/RANK) + query-field-allowlist-l0 +
> pagination-l0; CMS (StructuredContent sandbox/versioning/approval) → RE-FIND content-versioning-l0 +
> approval-workflow-l0; CMS content-targeting MVEL rule → SKIP (niche/non-generic). Only the rating
> derived-mean was a genuine residue.
