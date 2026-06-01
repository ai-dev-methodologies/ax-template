---
title: A public possession-of-token read MUST use an unguessable, PK-distinct token and deny bad tokens as an indistinguishable 404 — the token IS the authorization
impact: HIGH
impactDescription: "A public unauthenticated lookup (parcel tracking, guest order-status, share-by-link, e-receipt, unsubscribe) whose token is sequential / PK-derived, or whose bad-token path returns a distinguishable error, turns the endpoint into an open enumeration of every record's PII"
tags:
  - authz
  - capability-token
  - idor
  - existence-hiding
  - pii-minimization
  - anonymous-access
spec_ref: "specs/capability-token-l0.yaml#CAPTOKEN-UNGUESSABLE-001"
verification:
  type: review
  source: "specs/capability-token-l0.yaml"
  pattern: "public lookup token is >=128-bit SecureRandom, URL-safe, non-sequential, a SEPARATE column from the PK and never derivable from the internal id; bad/absent/expired token → byte-indistinguishable 404 (no 401/403/410 oracle, constant-time compare); the anonymous projection is a strict subset of the owner projection (dedicated public DTO, no internal id, masked PII)"
upstream:
  - "https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html"
  - "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md"
  - "https://datatracker.ietf.org/doc/html/rfc4122#section-6"
evidence:
  - source_type: external
    citation: "OWASP IDOR Prevention Cheat Sheet — Mitigation (replace enumerable IDs with complex random identifiers)"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html"
    quote: "As an additional defense-in-depth measure, replace enumerable numeric identifiers with more complex, random identifiers."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "RFC 4122 §6 — Security Considerations (UUIDs are not security capabilities)"
    url: "https://datatracker.ietf.org/doc/html/rfc4122#section-6"
    quote: "Do not assume that UUIDs are hard to guess; they should not be used as security capabilities (identifiers whose mere possession grants access), for example."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "OWASP ASVS v4.0.3 — V4.2.1 Operation Level Access Control (IDOR) + V4.1.5 (fail securely)"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md"
    quote: "Verify that sensitive data and APIs are protected against Insecure Direct Object Reference (IDOR) attacks targeting creation, reading, updating and deletion of records, such as creating or updating someone else's record, viewing everyone's records, or deleting all records."
    quoted_at: "2026-06-01"
---

## A public possession-of-token read MUST use an unguessable, PK-distinct token and deny bad tokens as an indistinguishable 404 — the token IS the authorization

**Impact: HIGH — when there is no logged-in user, the token carries the ENTIRE access decision; a weak token or a leaky error path is a full data-enumeration hole**

The catalog's authorization patterns all assume a logged-in principal. Owner-equality (`caller-authentication-only-no-userid-param.md`) derives the accessor from `Authentication.getName()`. Tenant-equality (`multi-tenant-l0`) scopes by the caller's tenant. Relationship-authz (`relationship-authz-l0` REBAC-LOOKUP-001) resolves a grant row keyed on the caller. Every one of them needs an `Authentication`. But a huge class of real endpoints has **no caller at all**: the parcel tracking page e-mailed to a consignee who has no account, the guest order-status link, the share-by-link document view, the e-receipt URL, the one-click unsubscribe. For these, the authorization is inverted — **possession of an unguessable token in the request IS the authorization**. There is no principal to check, no role, no ownership. This rule governs that surface, and it is the photographic negative of ReBAC: ReBAC asks "does a grant row exist for this caller?"; here we ask "is this request holding a valid capability token?" with nobody on the line.

Three invariants make the inversion safe, and each is testable:

1. **The token must be unguessable and distinct from the primary key.** It is at least 128 bits of `SecureRandom`, URL-safe-encoded, non-sequential, in a SEPARATE column — never the DB id, never derivable from the id, and the internal id never appears in the URL or response. A v4 UUID is the floor only, and only when it is `SecureRandom`-sourced and treated as opaque, because RFC 4122 itself says UUIDs "should not be used as security capabilities." If the token is sequential or PK-derived, the public endpoint becomes an open `for id in 1..N` enumeration of everyone's data.
2. **A bad, absent, expired, or revoked token denies as a byte-indistinguishable 404.** Possession of a *valid, unexpired* token is the SOLE authorization decision; its absence is the ONLY denial path, and it always returns the same 404 — same status, same `urn:problem:not-found` body, same shape — whether the token was never issued or was issued-then-expired. Any divergence (401/403/410/422, or a different message for the expired case) is an existence oracle that confirms guesses and harvests live tokens. Compare in constant time (`MessageDigest.isEqual` on the hashed token), so timing does not leak validity. This is REBAC-404-001 existence-hiding achieved with NO caller in the request.
3. **The anonymous projection is a strict subset of the owner projection.** The forwardable, screenshot-able, possibly-indexed link returns a dedicated public DTO that masks/omits PII (name, address, contact, internal ids, pricing internals, other parties) and exposes only what the use-case needs. It must never reach the owner entity, never accept a field-selection parameter that widens it, never leak the internal id.

**Incorrect — PK-derived/sequential token, distinguishable expired-vs-missing error, owner entity serialized to the anonymous bearer:**

```java
// ❌ token IS the primary key — /track/1, /track/2, ... enumerates every shipment
@GetMapping("/track/{id}")
public Shipment track(@PathVariable Long id) {
    Shipment s = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    if (s.getTrackingExpiresAt().isBefore(now())) {
        // ❌ 410 GONE distinguishes 'real but expired' from 'never existed' → existence oracle
        throw new ResponseStatusException(GONE, "tracking link expired");
    }
    return s;   // ❌ owner entity: consignee full name, full address, phone, price all leak
}
```

**Correct — 128-bit SecureRandom token in its own column, indistinguishable 404 for any non-live token, dedicated masked public DTO:**

```java
@Entity
class Shipment {
    @Id @GeneratedValue Long id;                       // internal, never public
    @Column(unique = true, updatable = false, nullable = false)
    String trackingToken;                              // SEPARATE from the PK
    Instant trackingExpiresAt;
    // ... owner-only PII fields ...
    static String newToken() {                         // >=128-bit, URL-safe, opaque
        byte[] b = new byte[16];
        new java.security.SecureRandom().nextBytes(b);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}

// permitAll() surface — NO Authentication parameter; the token is the authorization.
@GetMapping("/track/{token}")
public PublicTrackingView track(@PathVariable String token) {
    Shipment s = repo.findByTrackingToken(token)                 // single indexed lookup
        .filter(x -> x.getTrackingExpiresAt().isAfter(Instant.now()))
        .orElseThrow(NotFound::new);   // ✅ missing OR expired OR revoked → identical 404
    return PublicTrackingView.of(s);   // ✅ strict-subset DTO: status + ETA + masked city only
}
```

The negative test that proves it: `GET /track/<random-128-bit>` → **404**; `GET /track/<an-issued-but-expired-token>` → a **byte-identical 404** (diff the two responses — they must be equal); `GET /track/<the-live-token>` → **200** carrying only the public view. Assert `PublicTrackingView`'s field set is a strict subset of the owner DTO's and contains no internal id and no unmasked PII. The whole access decision lives in the token's entropy and the indistinguishability of the deny path.

Verification: review-tier. A reviewer confirms (a) the token is `SecureRandom`, >=128-bit, URL-safe, in a column distinct from the PK and not derivable from the internal id, with the internal id absent from the public URL/response; (b) malformed/absent/expired/revoked tokens all return the same 404 with no divergent status or message, and the compare is constant-time; (c) the anonymous response is a dedicated public DTO whose fields are a strict subset of the owner DTO with PII masked. No `@Tag` test is claimed because the public token surface and its 404/200 flip are recipe-instantiated, not present as a generic backend module in this template. This composes UNDER existing recipes as a separate `permitAll()` endpoint and MUST NOT be confused with the authenticated owner path: never accept a capability token on an owner endpoint, and never derive a principal from a capability token.

Reference: [OWASP IDOR Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html)

Reference: [RFC 4122 §6 — Security Considerations](https://datatracker.ietf.org/doc/html/rfc4122#section-6)

Reference: [OWASP ASVS v4.0.3 — V4 Access Control](https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md)
