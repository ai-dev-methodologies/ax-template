---
title: A facet-count aggregation MUST be computed over the IDENTICAL authorization/filter scope as the list query it accompanies, and the field a caller may facet on MUST be a compile-time allowlist — a non-allowlisted field is rejected by NAME with 422, fail-closed, before any aggregation query runs
impact: HIGH
impactDescription: "A facet-count endpoint that aggregates over a wider scope than the caller's own list query leaks the existence/volume of rows the caller cannot list — another tenant's or another user's row counts bleeding into a shared sidebar aggregate is an authorization-boundary leak by another name (CWE-639). Forwarding a client-supplied facet-by field name unchecked into a dynamically-built GROUP BY repeats the exact property-enumeration mistake query-field-allowlist-l0 closed for sort/filter (OWASP API3:2023), just on the aggregate path instead of the row path"
tags:
  - security
  - authorization
  - input-validation
  - pagination
spec_ref: "specs/facet-count-l0.yaml#FACET-COUNT-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/facetcount/FacetCountService.java + backend/src/main/java/com/ax/template/authblueprint/facetcount/FacetFieldAllowlist.java + backend/src/main/java/com/ax/template/authblueprint/facetcount/FacetCountController.java"
  pattern: "The facet-count query's WHERE clause scopes to the authenticated caller's own rows (ownerId), the IDENTICAL predicate the list endpoint would use — never a wider aggregate; the requested facet field is resolved through a compile-time allowlist BEFORE any repository call (422 FACET_FIELD_NOT_ALLOWED naming the field if absent); the aggregation query is selected from a fixed, pre-written parameterized query per allowlisted field — never a client string concatenated into JPQL; the bucket list returned is top-K by count plus an explicit otherCount remainder, and Σ(bucket counts) + otherCount always equals the caller's total scoped row count"
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
  - "https://cwe.mitre.org/data/definitions/639.html"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 — API3:2023 Broken Object Property Level Authorization (OWASP) — the cherry-pick discipline the facet-field allowlist applies to the aggregate path"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
    quote: "When exposing an object using an API endpoint, always make sure that the user should have access to the object's properties you expose."
    quoted_at: "2026-07-14"
  - source_type: external
    citation: "CWE-639: Authorization Bypass Through User-Controlled Key — MITRE (an unscoped aggregate leaks another principal's row volumes, the same class of authorization-boundary leak as a direct record fetch)"
    url: "https://cwe.mitre.org/data/definitions/639.html"
    quote: "The system's authorization functionality does not prevent one user from gaining access to another user's data or record by modifying the key value identifying the data."
    quoted_at: "2026-07-14"
---

## A facet-count sidebar is an authorization surface too — scope it identically to the list, and allowlist the facet-by field

**Impact: HIGH — a facet-count aggregate that runs over a wider scope than the caller's own list query leaks row volumes the caller cannot otherwise see (CWE-639); a facet-by field forwarded unchecked into a GROUP BY repeats the property-enumeration mistake OWASP API3:2023 warns against.**

`query-field-allowlist-l0` bounded WHICH field a client may sort/filter the ROWS of a list on. It did not address a second, easy-to-miss surface: the small "12 OPEN / 3 CLOSED" bucket-count sidebar many list UIs render next to the rows. That aggregate is computed by a SEPARATE query from the list itself — and if that separate query is written against a wider scope (e.g. it forgets the tenant/owner WHERE clause the list query has), the caller learns the volume of rows they cannot list. That is the same authorization-boundary leak CWE-639 describes, just delivered as a count instead of a record.

The primitive: (1) the facet-count query's scope predicate MUST be built from the SAME authorization/filter logic as the list query — never a bespoke, potentially-wider one; (2) the facet-BY field is resolved through a compile-time allowlist before any aggregation query is built, exactly mirroring `QueryFieldAllowlist`'s sort/filter discipline, but applied to the GROUP BY column instead of the ORDER BY / WHERE column.

**Incorrect — a facet aggregate that forgets the caller's scope, and a facet-by field forwarded raw:**

```java
// <!-- catalog-example-ok: FacetRepository — illustrative anti-pattern, not a shipped symbol -->
@GetMapping("/api/items/facets")
public Map<String, Long> facets(@RequestParam String field) {
    // ❌ no ownerId scope — this counts EVERY caller's rows, not just this caller's
    String jpql = "SELECT i." + field + ", COUNT(i) FROM Item i GROUP BY i." + field; // ❌ raw field concatenated
    return repo.query(jpql);   // ❌ unbounded bucket cardinality too
}
```

**Correct — identical scope predicate as the list; the field resolved through an allowlist first:**

```java
@Transactional(readOnly = true)
public FacetCountResponse facets(String ownerId, String publicField) {
    String internal = FacetFieldAllowlist.resolve(publicField);   // 422 FACET_FIELD_NOT_ALLOWED before any query
    List<Object[]> rows = switch (internal) {
        case "category" -> items.countsByCategoryForOwner(ownerId);   // SAME ownerId scope the list uses
        case "status"   -> items.countsByStatusForOwner(ownerId);
        default -> throw FacetCountException.notAllowed(publicField, FacetFieldAllowlist.allowed()); // unreachable
    };
    return FacetBucketing.topKWithRemainder(rows, MAX_BUCKETS);       // bounded cardinality, Σ + other == total
}
```

**1. Scope parity (FACET-COUNT-001).** The facet aggregation's WHERE clause is the caller's own scope — the same one the list endpoint applies — never a table-wide rollup misrepresented as "your counts".

**2. Field allowlist (FACET-ALLOWLIST-002).** `FacetFieldAllowlist.resolve` throws BEFORE any repository call for a field the resource did not declare facetable — the 422 names the offending field, and the aggregation query for each allowlisted field is a fixed, pre-written parameterized query, never a dynamically concatenated one.

**3. Bounded cardinality (FACET-BOUND-003).** The response returns at most K buckets by count, plus an explicit `otherCount` remainder — a high-cardinality field can never produce an unbounded response, and the conservation identity `Σ(bucket counts) + otherCount == total` proves no row was silently dropped by the bounding.

Verification: review-tier — confirm the facet query's scope predicate is textually identical to (or derived from the same helper as) the list query's scope predicate, the field resolves through the allowlist before any repository access, and the bucket response is bounded with the conservation identity holding.

Reference: [OWASP API3:2023 Broken Object Property Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/)

Reference: [CWE-639: Authorization Bypass Through User-Controlled Key](https://cwe.mitre.org/data/definitions/639.html)
