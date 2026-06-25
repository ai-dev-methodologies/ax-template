---
title: A denormalized derived aggregate (an average / count / sum cached on a parent from its child rows) MUST be recomputed from its CURRENT source rows on every change (never hand-edited, never drifting), computed over a DECLARED eligibility predicate (so an unapproved / soft-deleted row cannot silently move it), with a DEFINED empty-set sentinel (never a divide-by-zero) — a denormalized MEAN is a quotient, distinct from the catalog's SUM-conservation family
impact: MEDIUM
impactDescription: "A product's averageRating + reviewCount, a post's like/helpful count, a user's follower count, a leaderboard score — any denormalized aggregate cached on a parent silently corrupts when it is hand-edited or left stale after a source row changes: a stored average drifts from the reviews it claims to summarize (the displayed rating is a lie that no structural check catches — it is non-null and in range), an unmoderated or spam review silently moves a user-facing score, and an empty review set divides by zero / leaks NaN to the API. Recomputing the aggregate from the current rows in the same transaction as every source-row change, over a declared eligibility predicate, with a defined empty sentinel, makes the drift unrepresentable. A denormalized MEAN is the key case the catalog's conservation specs do NOT cover: balanced-posting / collection-conservation / valuation-run-projection all assert a SUM identity (Σ parts == total); a mean is a QUOTIENT with no conservation law, whose only correctness property is derivational consistency."
tags:
  - data-integrity
  - aggregate
  - denormalization
  - consistency
spec_ref: "specs/derived-aggregate-consistency-l0.yaml#DERIVED-AGG-CONSISTENCY-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/ratingsummary/RatingSummaryService.java + backend/src/main/java/com/ax/template/authblueprint/ratingsummary/RatingSummary.java"
  pattern: "A parent carries a denormalized aggregate (avg/count) as a derived column with NO public setter that writes a caller-supplied value; a sole-recompute service re-derives the aggregate from the parent's CURRENT eligible source rows inside the SAME transaction as any source-row insert/update/delete (never hand-edited), over a declared eligibility predicate (status=APPROVED — an ax strengthening over Broadleaf, which averages all rows); the empty eligible set yields a documented sentinel (average 0 / explicit no-data), never Σ/0 or NaN; the stored aggregate equals an independent repo AVG/COUNT re-derivation; @Version guards concurrent recompute; the displayed average is exact decimal (BigDecimal, round-once)."
upstream:
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/rating/domain/RatingSummaryImpl.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/rating/dao/RatingSummaryDaoImpl.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/rating/service/type/ReviewStatusType.java"
  - "https://www.postgresql.org/docs/current/rules-materializedviews.html"
evidence:
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) RatingSummaryImpl.resetAverageRating — the recompute-from-rows mean absorbed: the average is Σ ratings / count, re-derived from the current child rows (a denormalized mean, not a sum)"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/rating/domain/RatingSummaryImpl.java"
    quote: "this.averageRating = sum / ratings.size();"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) RatingSummaryDaoImpl.saveRatingSummary — the recompute-on-every-save absorbed: resetAverageRating() is invoked immediately before the persist, so the aggregate is never hand-set and never drifts"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/rating/dao/RatingSummaryDaoImpl.java"
    quote: "summary.resetAverageRating();"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) ReviewStatusType — the moderation status enum: PENDING/APPROVED/REJECTED. NB Broadleaf moderates the review TEXT but averages ALL rating rows with no status predicate; the eligibility-filtered aggregate is an ax STRENGTHENING grounded on this enum"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/rating/service/type/ReviewStatusType.java"
    quote: "public static final ReviewStatusType PENDING = new ReviewStatusType(\"PENDING\");"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "PostgreSQL documentation — Materialized Views: the canonical statement that a denormalized derived aggregate is stale until recomputed from its defining query (the consistency obligation this rule enforces at the application layer)"
    url: "https://www.postgresql.org/docs/current/rules-materializedviews.html"
    quote: "fresh data can be generated for the materialized view with:"
    quoted_at: "2026-06-25"
---

## Rule

A **denormalized derived aggregate** — a product's `averageRating` + `reviewCount`, a post's like/helpful count, a follower count, a leaderboard score — is a value CACHED on a parent that is computed from the parent's child rows. It is a read-optimization (you do not re-`AVG` every render), and its hazard is **drift**: the cached value silently diverging from the rows it claims to summarize. A denormalized **MEAN** is the case the catalog's conservation specs do not cover — `balanced-posting` / `collection-conservation` / `valuation-run-projection` all assert a SUM identity (`Σ parts == total`); a mean is a **quotient** with no conservation law, whose only correctness property is **derivational consistency**. It is also distinct from `denormalized-counter-reconcilable` (`per-tenant-resource-quota-l0#QUOTA-RECONCILE-001`), its closest neighbor: that rule **increments** a counter on the write path and repairs drift **asynchronously** via a scheduled sweep (integer `truth != stored` equality, eventually-consistent); this rule **never increments** — it **recomputes from the rows synchronously** in the same transaction (so it cannot drift) and adds the eligibility predicate + the quotient/divide-by-zero sentinel an integer-counter reconcile has no concept of. Three obligations:

1. **Recompute from current rows on every change (DERIVED-AGG-CONSISTENCY-001).** The aggregate MUST be re-derived from the parent's current source rows **in the same transaction** as any insert/update/delete of a source row — never hand-edited (no public setter writing a caller-supplied value), never left stale. The stored value MUST equal an **independent** re-derivation (a repo `AVG`/`COUNT` query — a different code path than the in-memory fold). Per Broadleaf, `RatingSummary` recomputes `averageRating = Σ ratings / count` via `resetAverageRating()`, invoked on **every** `saveRatingSummary` before persist.
2. **Compute over a declared eligibility predicate (DERIVED-AGG-ELIGIBILITY-001 — ax STRENGTHENING).** The aggregate MUST be computed over a **declared** eligibility set (e.g. `status = APPROVED`); a PENDING/REJECTED/soft-deleted row MUST NOT contribute, and a row transitioning INTO the predicate triggers a recompute. **This strengthens Broadleaf** — Broadleaf's `resetAverageRating` averages **all** rating rows with no status predicate (only the review *text* is moderated via `ReviewStatusType`). ax generalizes: an aggregate that feeds a user-facing or decision number should not be moved by an unapproved/spam/soft-deleted row.
3. **Defined empty-set sentinel (DERIVED-AGG-EMPTY-001).** The empty eligible set yields a **documented sentinel** (average 0 / explicit no-data), never `Σ/0`, NaN, Infinity, or a null leaked to the API. Per Broadleaf, the empty/null rating set yields `averageRating = 0.0`.

The aggregate column has no public setter; `@Version` guards concurrent recompute; the displayed average is exact decimal (`BigDecimal`, round-once — composes `spring-practices-l0` averaging discipline).

**Correct — recompute-from-rows over the eligible set, defined empty sentinel, no hand-edit:**

```java
// backend/.../ratingsummary/RatingSummaryService.java — sole recompute; called in the same tx as any review change
@Transactional
public void onReviewChanged(UUID productId) {
    List<Review> eligible = reviews.findByProductIdAndStatus(productId, APPROVED);   // declared eligibility predicate
    RatingSummary s = summaries.findByIdForUpdate(productId);                         // @Version concurrent guard
    s.recomputeFrom(eligible);   // sets count + average FROM the rows — no public setAverage(value)
}
// RatingSummary.recomputeFrom — derived; empty set → documented sentinel, never Σ/0
void recomputeFrom(List<Review> eligible) {
    this.reviewCount = eligible.size();
    this.average = eligible.isEmpty()
        ? BigDecimal.ZERO                                                            // empty sentinel (no divide-by-zero)
        : sumStars(eligible).divide(BigDecimal.valueOf(eligible.size()), 2, HALF_UP); // mean, round-once
}
```

**Incorrect — hand-edited aggregate / averages everything / divides by zero:**

```java
summary.setAverage(req.average());          // WRONG: hand-edited — drifts from the rows; no recompute
double avg = sumAll() / reviews.size();      // WRONG: averages PENDING+REJECTED too; Σ/0 when empty (NaN/exception)
```

A hand-edited average is a number that lies about its rows; averaging unmoderated rows lets spam move a user-facing score; `Σ/count` on an empty set divides by zero. Recomputing from the current eligible rows in the same transaction, with a defined empty sentinel and no public setter, makes each defect unrepresentable.

Reference: [Broadleaf RatingSummaryImpl.resetAverageRating (recompute mean from rows; 0.0 empty sentinel)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/rating/domain/RatingSummaryImpl.java)

Reference: [Broadleaf RatingSummaryDaoImpl (resetAverageRating before every merge)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/rating/dao/RatingSummaryDaoImpl.java)

Reference: [PostgreSQL — Materialized Views (a derived aggregate is stale until recomputed)](https://www.postgresql.org/docs/current/rules-materializedviews.html)
