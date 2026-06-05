---
title: A temporally-versioned record MUST answer an as-of query with exactly the one row in force at the instant, and hold at most one open-ended current row per scope
impact: HIGH
impactDescription: "If an as-of (point-in-time) query returns two rows for one instant, a downstream 'what was the salary on date X' has two conflicting answers and any computation built on it is wrong; if a scope key has two open-ended current rows, 'the current value' is ambiguous and a new write can append a second current period instead of closing the prior one. Both corrupt the temporal record silently — overlaps and double-currents only surface when a historical question is asked."
tags:
  - temporal
  - bitemporal
  - point-in-time
  - range-types
  - postgresql
  - data-integrity
spec_ref: "specs/temporal-validity-l0.yaml#TEMPORAL-POINT-IN-TIME-001"
verification:
  type: review
  source: "specs/temporal-validity-l0.yaml#TEMPORAL-POINT-IN-TIME-001"
  pattern: "An as-of (point-in-time) query for a scope key at instant X MUST return EXACTLY the one row whose half-open validity range contains X — `valid_from <= X AND (valid_to IS NULL OR X < valid_to)` — exploiting strict less-than on the upper bound so adjacent windows never both match; empty before the earliest row (TEMPORAL-POINT-IN-TIME-001). A scope key MUST hold AT MOST ONE open-ended current row (`valid_to IS NULL`); writing a new record MUST close the prior open row (set its valid_to) in the SAME transaction, and a partial unique index `(scope_key) WHERE valid_to IS NULL` is the structural backstop (TEMPORAL-OPEN-CURRENT-001). These compose the non-overlap exclusion constraint (TEMPORAL-NON-OVERLAP-001). Reject an as-of query that can return two rows for one instant (inclusive-both-bounds), a new write that appends a current row without closing the prior one, and a scope with no partial-unique-index backstop on the open row."
upstream:
  - "https://www.postgresql.org/docs/current/rangetypes.html"
  - "https://www.postgresql.org/docs/current/ddl-constraints.html"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — Range Types (definition)"
    url: "https://www.postgresql.org/docs/current/rangetypes.html"
    quote: "Range types are data types representing a range of values of some element type (called the range's _subtype_)."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "PostgreSQL Documentation — Constraints (CHECK / structural enforcement)"
    url: "https://www.postgresql.org/docs/current/ddl-constraints.html"
    quote: "A check constraint is the most generic constraint type. It allows you to specify that the value in a certain column must satisfy a Boolean (truth-value) expression."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A temporally-versioned record MUST answer an as-of query with exactly one in-force row and hold at most one open-ended current row

**Impact: HIGH — A temporal record (a salary history, a price schedule, an effective-dated policy) stores many rows per scope key, each valid over a time window. Two invariants make it trustworthy. First, an as-of query for one instant must return EXACTLY one row — ask "what was the salary in force on 2026-01-14" and get one answer, never two. Second, only one row may be the open-ended *current* one. PostgreSQL gives the tools: range types are *data types representing a range of values of some element type*, and a CHECK/exclusion constraint *allows you to specify that the value in a certain column must satisfy a Boolean (truth-value) expression* — the structural backstop. Get the bounds wrong (inclusive on both ends) and adjacent windows both match an instant; forget to close the prior current row and a scope has two "current" values. Both are silent until a historical question exposes them.**

This rule governs the as-of/current-row items of `specs/temporal-validity-l0.yaml`, composing the non-overlap exclusion constraint (`TEMPORAL-NON-OVERLAP-001`, the existing `temporal-validity-record-non-overlap` rule).

**1. Exactly-one as-of query (TEMPORAL-POINT-IN-TIME-001).** The point-in-time query is `valid_from <= X AND (valid_to IS NULL OR X < valid_to)` — half-open, **strict** less-than on the upper bound. Because windows are half-open `[from, to)`, an instant at a shared boundary belongs to exactly one window. The query returns exactly one row when X is within the record's history, and empty before the earliest `valid_from` — never two rows, never a silently-picked arbitrary one.

**2. At most one open-ended current row (TEMPORAL-OPEN-CURRENT-001).** `valid_to IS NULL` marks the current row; a scope key may have at most one. Writing a new record is a single transaction that CLOSES the prior open row (sets its `valid_to` to the new `valid_from`) and inserts the new open row — never appends a second `NULL` row. A partial unique index `UNIQUE (scope_key) WHERE valid_to IS NULL` is the structural backstop that makes a second current row impossible.

**Incorrect — inclusive-both-bounds as-of (two rows at a boundary); appends a current row without closing the prior:**

```sql
-- VIOLATION: BETWEEN is inclusive on both ends → an instant on a shared boundary matches TWO rows
SELECT * FROM salary WHERE emp_id = :id AND :x BETWEEN valid_from AND valid_to;   -- TEMPORAL-POINT-IN-TIME-001
```
```java
// VIOLATION: inserts a new open row without closing the prior open one → two "current" rows
salaryRepo.save(new Salary(empId, newAmount, asOf, null));   // TEMPORAL-OPEN-CURRENT-001
```

**Correct — half-open strict-upper as-of; close-then-open in one transaction; partial unique index backstop:**

```sql
-- exactly one in-force row (half-open, strict upper bound)  TEMPORAL-POINT-IN-TIME-001
SELECT * FROM salary
 WHERE emp_id = :id AND valid_from <= :x AND (valid_to IS NULL OR :x < valid_to);
-- structural backstop: at most one open current row per scope  TEMPORAL-OPEN-CURRENT-001
CREATE UNIQUE INDEX one_current_salary ON salary (emp_id) WHERE valid_to IS NULL;
```
```java
@Transactional
public void recordNew(long empId, BigDecimal amount, Instant asOf) {
    salaryRepo.closeOpenRow(empId, asOf);                 // set prior open row's valid_to = asOf
    salaryRepo.save(new Salary(empId, amount, asOf, null)); // single new open row — commit together
}
```

Verification: review-tier. As-of correctness is a query-semantics property — an inclusive-bounds query and a no-close insert both compile and look right on a single-period record, failing only once two periods exist and a boundary instant or "current" is queried. Verify by review against `specs/temporal-validity-l0.yaml`: the as-of query uses half-open strict-upper bounds and returns exactly one (or zero) rows; a new write closes the prior open row in the same transaction; a partial unique index enforces at most one current row; non-overlap is backed by the exclusion constraint. When a fork-receiver wires a real IT (boundary-instant as-of returns one row; two open rows rejected by the index), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [PostgreSQL — Range Types](https://www.postgresql.org/docs/current/rangetypes.html)

Reference: [PostgreSQL — Constraints](https://www.postgresql.org/docs/current/ddl-constraints.html)
