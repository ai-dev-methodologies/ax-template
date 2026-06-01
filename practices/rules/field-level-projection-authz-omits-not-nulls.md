---
title: Field-level projection MUST be server-decided per caller and OMIT unauthorized fields — never load-then-null
impact: HIGH
impactDescription: "A caller-graded field set that nulls (instead of omitting) unauthorized fields leaks the sensitive value through logs, exception payloads, and over-broad DTOs — OWASP API3:2023 Broken Object Property Level Authorization"
tags:
  - authz
  - object-property-level-authz
  - data-exposure
  - dto
  - field-projection
  - least-privilege
spec_ref: "specs/field-projection-authz-l0.yaml#FIELD-PROJECTION-001"
verification:
  type: review
  source: "specs/field-projection-authz-l0.yaml"
  pattern: "A reviewer confirms each authorization class has a DISTINCT response DTO/projection whose members are exactly the fields that class may read; unauthorized fields are ABSENT from the DTO type (no member), never a nulled field on a shared DTO; the caller's class is derived from Authentication, not a client parameter"
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
  - "https://owasp.org/API-Security/editions/2019/en/0xa3-excessive-data-exposure/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API3:2023 Broken Object Property Level Authorization (prevention guidance)"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
    quote: "When exposing an object using an API endpoint, always make sure that the user should have access to the object's properties you expose."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "OWASP API Security Top 10 (2019) — API3:2019 Excessive Data Exposure (the failure mode merged into API3:2023)"
    url: "https://owasp.org/API-Security/editions/2019/en/0xa3-excessive-data-exposure/"
    quote: "APIs rely on clients to perform the data filtering."
    quoted_at: "2026-06-01"
---

## Field-level projection MUST be server-decided per caller and OMIT unauthorized fields — never load-then-null

**Impact: HIGH — a load-then-null projection still ships the sensitive value into the response object, where logs, exception payloads, and the next over-broad DTO leak it.**

Some resources are seen by callers in different authorization classes, and the SAME row must reveal DIFFERENT fields to each. A manager reading a direct report's record sees the salary; a peer sees only name and title; the employee reading their own record sees their own salary. A patient record's clinical-notes section is visible to the care team but not to billing. A marketplace order shows the buyer their shipping address and the seller their payout split — each hidden from the other. A social profile grades email/phone/birthday visibility by follower tier.

This is **object-PROPERTY-level authorization** (OWASP API3:2023), and it is distinct from the two adjacent catalog axes:

- **relationship-authz-l0 is ROW level** — "may I see this row at all". A grant lookup decides whole-object access. Once a caller is authorized for the row, *this* rule decides which of its FIELDS they may read.
- **pii-masked-at-dto-boundary is a UNIFORM mask** — every caller gets the SAME masked value (`203.0.113.xxx`, `Chrome on Windows`). That is privacy-by-design applied identically to all. This rule is **role-GRADED**: the manager sees the real salary, the peer sees no salary field at all. The difference is the *caller's authorization*, not a blanket redaction.

Two invariants:

1. **The visible field set is decided SERVER-SIDE** from the caller's authenticated role/relationship — never by the client, and never by returning every property and trusting the consumer to hide some. The 2019 Excessive Data Exposure failure mode was exactly this: *"APIs rely on clients to perform the data filtering."* Derive the caller's class from `Authentication` (role claim and/or a grant lookup); never from a request parameter that could widen the field set.
2. **Unauthorized fields are OMITTED, not loaded-then-nulled.** Give each authorization class a DISTINCT DTO whose members are exactly the fields it may read. A field that was never declared on the DTO cannot leak — through logging, through an exception serializer, through a future endpoint that forgets to re-null it.

**Incorrect — one shared DTO, fields nulled for unauthorized callers (load-then-null):**

```java
public record ReportView(UUID id, String name, String title, BigDecimal salary) {
    public static ReportView of(Employee e, boolean callerMaySeeComp) {
        return new ReportView(
            e.getId(), e.getName(), e.getTitle(),
            callerMaySeeComp ? e.getSalary() : null   // ❌ salary still loaded onto the object
        );
    }
}
// The real salary is read from the entity and lives on the response object even for a peer.
// It leaks via: log.info("returning {}", view), an exception that serializes the object,
// a /debug echo, or a sibling endpoint that maps the entity without the null branch.
// And the `salary` JSON key is PRESENT as null — a property-level oracle the peer can see.
```

**Correct — server picks a per-class projection; the unauthorized field is ABSENT from the type:**

```java
// Each authorization class has its OWN DTO. The peer DTO simply has no salary member,
// so it is structurally impossible to serialize the salary to a peer.
public record PeerProfileView(UUID id, String name, String title) {}
public record ManagerCompView(UUID id, String name, String title, BigDecimal salary) {}
public record SelfCompView(UUID id, String name, String title, BigDecimal salary) {}

@GetMapping("/api/employees/{id}")
public ResponseEntity<?> getEmployee(@PathVariable UUID id, Authentication auth) {
    Employee e = service.requireReadable(id, auth);            // ROW-level gate (relationship-authz)
    return switch (projectionFor(e, auth)) {                   // FIELD-level: class from Authentication
        case SELF    -> ResponseEntity.ok(SelfCompView.of(e));     // own salary
        case MANAGER -> ResponseEntity.ok(ManagerCompView.of(e));  // report's salary
        case PEER    -> ResponseEntity.ok(PeerProfileView.of(e));  // ✅ no salary member at all
    };
}
// projectionFor() derives the class from auth (role claim + manager-of grant lookup),
// NEVER from a client parameter. The salary is fetched only when the chosen DTO has a slot
// for it; a peer's response object never holds the value, so nothing downstream can leak it.
```

Apply this when the same resource exposes a caller-graded field set: HR compensation, healthcare record sections, marketplace seller-vs-buyer views, social profile visibility tiers. Pick the DTO from the caller's authorization class server-side, and let an unauthorized field be *absent from the DTO type* rather than *null on a shared instance*.

**Verification (review tier):** a reviewer confirms each authorization class has a distinct projection DTO whose members are exactly the readable fields, that unauthorized fields are absent from the DTO type (not nulled on a shared DTO), and that the caller's class is derived from `Authentication` rather than a client-supplied parameter. There is no single `@Tag` because the per-class projection map is a human-judgment policy checked against the authorization model.

Reference: [OWASP API Security Top 10 (2023) — API3:2023 Broken Object Property Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/)

Reference: [OWASP API Security Top 10 (2019) — API3:2019 Excessive Data Exposure](https://owasp.org/API-Security/editions/2019/en/0xa3-excessive-data-exposure/)

Reference: [OWASP ASVS v4.0.3 — V4.1.3 Access Control (least privilege)](https://owasp.org/www-project-application-security-verification-standard/)
