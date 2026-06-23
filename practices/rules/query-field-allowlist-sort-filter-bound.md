---
title: A list/search endpoint that accepts client-supplied SORT and FILTER field names MUST bound them with a per-resource ALLOWLIST (the exact fields it permits, mapping each PUBLIC name to an internal entity property, restricting direction to asc/desc and operator to a closed safe set), rejecting any non-allowlisted field by NAME with a 422 — never forwarding the raw string into a Sort/Specification, never silently ignoring it
impact: HIGH
impactDescription: "A list endpoint that passes the client's sort=/filter= field name straight into Sort.by(rawString) or a Specification is three vulnerabilities at once: an INJECTION sink when the field name is interpolated into a query (CWE-89); a PROPERTY-ENUMERATION vector — a caller sorts/filters on `password`, `internalNotes`, or any private property to order by or infer data the API never meant to expose (OWASP API3:2023 Broken Object Property Level Authorization); and an IDOR-flavoured access vector when the probed field is a foreign key the caller controls (CWE-639). Silently ignoring an unknown field is just as wrong — it returns a surprise result set the caller did not ask for"
tags:
  - security
  - input-validation
  - injection
  - authorization
  - pagination
spec_ref: "specs/query-field-allowlist-l0.yaml#QUERY-ALLOWLIST-SORT-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/queryguard/QueryFieldAllowlist.java + backend/src/main/java/com/ax/template/authblueprint/queryguard/QueryGuardService.java + backend/src/main/java/com/ax/template/authblueprint/queryguard/QueryGuardController.java"
  pattern: "A per-resource QueryFieldAllowlist maps each PUBLIC sort/filter field name to its INTERNAL entity property; the service consults the allowlist BEFORE building any Sort or Specification, so a Sort is built only from an allowlisted internal property (toSort) and a filter predicate only from an allowlisted property + a typed FilterOperator + a parameter-bound value (filterProperty + buildFilterSpec); a sort field not in the sortable allowlist → 422 QUERY_FIELD_NOT_SORTABLE naming the field, a filter field not in the filterable allowlist → 422 QUERY_FIELD_NOT_FILTERABLE, a direction outside {asc,desc} → 422 QUERY_DIRECTION_INVALID, an operator outside {eq,ne,gt,gte,lt,lte,like} → 422 QUERY_OPERATOR_INVALID; the list response is a bounded PageEnvelope over a stable sort; the raw client string is never handed to Sort.by(...) or concatenated into a query"
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
  - "https://cwe.mitre.org/data/definitions/89.html"
  - "https://cwe.mitre.org/data/definitions/639.html"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 — API3:2023 Broken Object Property Level Authorization (OWASP) — the cherry-pick discipline the field allowlist generalizes to the query surface: only expose object properties the user should have access to"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
    quote: "When exposing an object using an API endpoint, always make sure that the user should have access to the object's properties you expose."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-89: Improper Neutralization of Special Elements used in an SQL Command ('SQL Injection') — MITRE (a client-named sort/filter column interpolated into a query)"
    url: "https://cwe.mitre.org/data/definitions/89.html"
    quote: "The product constructs all or part of an SQL command using externally-influenced input from an upstream component, but it does not neutralize or incorrectly neutralizes special elements that could modify the intended SQL command when it is sent to a downstream component."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-639: Authorization Bypass Through User-Controlled Key — MITRE (a user-controlled field name used as a record/property key)"
    url: "https://cwe.mitre.org/data/definitions/639.html"
    quote: "The system's authorization functionality does not prevent one user from gaining access to another user's data or record by modifying the key value identifying the data."
    quoted_at: "2026-06-23"
---

## A queryable column is a privilege, not a free text param — bound it with a per-resource allowlist

**Impact: HIGH — a list endpoint that forwards the client's `sort=`/`filter=` field name unchecked into `Sort.by(...)` or a `Specification` is an injection (CWE-89) + property-enumeration (OWASP API3:2023) + IDOR (CWE-639) surface at once.**

OWASP API3:2023 states the discipline directly: *"When exposing an object using an API endpoint, always make sure that the user should have access to the object's properties you expose."* A sort/filter param is exactly such an exposure — naming a column lets the caller **order by** or **infer** that property. The catalog already bounded page SIZE (`pagination-l0`: `PageEnvelope` + `OffsetPageSupport`) and appended a stable-sort tiebreaker, but had **no primitive that bounds WHICH field a client may name**. So a hand-rolled list endpoint that does `Sort.by(request.getParameter("sort"))` lets a caller sort on `password` or `internalNotes`, or — when the value is interpolated — smuggle a SQL fragment (CWE-89).

The primitive: a per-resource **`QueryFieldAllowlist`** maps each PUBLIC field name to its INTERNAL entity property, separately for sortable and filterable fields, and is consulted **before** any `Sort`/`Specification` is built:

```text
QueryFieldAllowlist(resource):  sortable   { public-name → internal-property }
                                filterable { public-name → internal-property }
toSort(field, direction):       field MUST be sortable (else 422 NOT_SORTABLE, names field);
                                direction MUST be asc|desc (else 422 DIRECTION_INVALID);
                                Sort.by built ONLY from the mapped internal property
filterProperty(field):          field MUST be filterable (else 422 NOT_FILTERABLE);
                                predicate built ONLY from the mapped property + a typed
                                operator {eq,ne,gt,gte,lt,lte,like} + a BOUND value
```

A field the resource did not declare is **unrepresentable** in a built `Sort`/predicate — the raw client string is never the thing handed to JPA. That closes the injection / enumeration / IDOR surface *by construction*, not by neutralizing a dangerous string after the fact.

**Incorrect — the client's field name forwarded straight into a Sort / a concatenated query:**

```java
// <!-- catalog-example-ok: ItemRepository — illustrative anti-pattern, not a shipped symbol -->
@GetMapping("/api/items")
public List<Item> list(@RequestParam String sort, @RequestParam String filter) {
    Sort s = Sort.by(Sort.Direction.ASC, sort);              // ❌ raw client field → Sort.by (sort=password orders by it)
    String jpql = "SELECT i FROM Item i WHERE i." + filter;  // ❌ field name concatenated into the query (CWE-89)
    return repo.findAll(jpql, s);                            // ❌ unbounded list; no allowlist, no page bound
}
```

**Correct — the per-resource allowlist gates the field BEFORE any Sort/Specification is built:**

```java
static final QueryFieldAllowlist CATALOG_ITEM_ALLOWLIST = QueryFieldAllowlist.builder()
    .sortable("name", "name")
    .sortable("createdAt", "createdAt")
    .sortable("status", "status")
    .sortable("priceMinor", "priceMinor")
    .filterable("name", "name")
    .filterable("status", "status")
    .filterable("priceMinor", "priceMinor")
    .build();

@Transactional(readOnly = true)
public PageEnvelope<CatalogItemDto> list(String sortField, String direction, String filter,
                                         int page, int size) {
    String effectiveField = (sortField == null || sortField.isBlank()) ? DEFAULT_SORT_FIELD : sortField;
    String effectiveDir = (direction == null || direction.isBlank()) ? DEFAULT_SORT_DIRECTION : direction;

    // QUERY-ALLOWLIST-SORT-001 — Sort built ONLY from the allowlisted internal property.
    Sort sort = CATALOG_ITEM_ALLOWLIST.toSort(effectiveField, effectiveDir);   // 422 if not sortable / bad direction

    // QUERY-ALLOWLIST-FILTER-001 — Specification built ONLY from allowlisted property + bound value.
    Specification<CatalogItem> spec =
        (filter == null || filter.isBlank()) ? null : buildFilterSpec(filter);

    // QUERY-ALLOWLIST-PAGE-001 — clamped size + stable-sort tiebreaker + bounded PageEnvelope.
    PageRequest request = OffsetPageSupport.clamp(page, size, MAX_PAGE_SIZE)
        .withSort(OffsetPageSupport.stableSort(sort));
    Page<CatalogItem> result = items.findAll(spec, request);
    return PageEnvelope.from(result, CatalogItemDto::of);
}
```

```java
// QueryFieldAllowlist.toSort — the raw client string never reaches Sort.by(...)
public Sort toSort(String publicField, String directionToken) {
    String internalProperty = sortable.get(publicField);
    if (internalProperty == null) {
        throw QueryGuardException.notSortable(publicField, sortable.keySet());   // 422 — names the field
    }
    SortDirection direction = SortDirection.parse(directionToken)
        .orElseThrow(() -> QueryGuardException.directionInvalid(directionToken)); // 422 — asc|desc only
    Sort.Direction springDirection =
        direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    return Sort.by(springDirection, internalProperty);                           // mapped internal property ONLY
}
```

**1. The sort field is allowlisted; the direction is closed (QUERY-ALLOWLIST-SORT-001).** `toSort` resolves the public field through the sortable map (422 `QUERY_FIELD_NOT_SORTABLE` naming the field if absent) and parses the direction to `{asc, desc}` (422 `QUERY_DIRECTION_INVALID` otherwise). `Sort.by(...)` only ever sees the mapped internal property.

**2. The filter field + operator are allowlisted; the value is bound (QUERY-ALLOWLIST-FILTER-001).** `filterProperty` resolves the public field through the filterable map (422 `QUERY_FIELD_NOT_FILTERABLE`); the operator parses to the closed `FilterOperator` set (422 `QUERY_OPERATOR_INVALID`); the predicate is built with the criteria API from the property + a parameter-bound value — never string concatenation (CWE-89).

**3. The mapping hides internal names (QUERY-ALLOWLIST-MAPPING-001).** Clients send only PUBLIC names; the allowlist translates to the internal property. An internal property a client guesses does not resolve, so arbitrary properties are unprobeable (CWE-639).

**4. The response is a bounded PageEnvelope (QUERY-ALLOWLIST-PAGE-001).** Size is clamped by `OffsetPageSupport`, a stable-sort tiebreaker is appended, and the result is the canonical `{data, pagination}` envelope — never an unbounded list.

Verification: review-tier — confirm the allowlist is consulted before any `Sort`/`Specification` is built, the four 422 codes name the offending field/direction/operator, and the raw client string never reaches `Sort.by(...)` or a concatenated query. The behavioural proof a fork-receiver keeps green is the keystone: `sort=password` → 422 `QUERY_FIELD_NOT_SORTABLE`, `filter=internalNotes:eq:x` → 422 `QUERY_FIELD_NOT_FILTERABLE`, a SQL-fragment field → 422, query never executed — no silent pass-through.

Reference: [OWASP API3:2023 Broken Object Property Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/)

Reference: [CWE-89: SQL Injection](https://cwe.mitre.org/data/definitions/89.html)

Reference: [CWE-639: Authorization Bypass Through User-Controlled Key](https://cwe.mitre.org/data/definitions/639.html)
