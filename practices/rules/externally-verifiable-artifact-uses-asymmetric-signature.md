---
title: Artifacts a third party must verify MUST use a detached asymmetric signature — never HMAC
impact: HIGH
impactDescription: "Signing a third-party-verifiable artifact (certificate, invoice, report, badge, federated attestation) with HMAC forces the issuer to hand its signing secret to every verifier, which lets any verifier forge artifacts; trusting the token's own alg header opens alg:none and ES->HS algorithm-confusion forgeries"
tags:
  - security
  - signing
  - jws
  - asymmetric
  - alg-confusion
  - federation
spec_ref: "specs/signed-artifact-l0.yaml#SIGNED-ASYM-001"
verification:
  type: review
  source: "practices/rules/webhook-hmac-required.md (symmetric envelope — signer==verifier) vs this rule (asymmetric — verifier != signer)"
  pattern: "Any issuer of a completion certificate / signed receipt / exportable report / public badge / federated attestation signs with JWS alg pinned to ES256 or EdDSA over a published JWKS kid; the verifier enforces an asymmetric-only alg allow-list and selects the algorithm from server config by kid, never from the token's own alg header. No HMAC (HS*) and no alg:none on a third-party-verifiable artifact."
upstream:
  - "https://www.rfc-editor.org/rfc/rfc7515"
  - "https://www.rfc-editor.org/rfc/rfc7518"
  - "https://www.rfc-editor.org/rfc/rfc8037"
  - "https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html"
evidence:
  - source_type: external
    citation: "RFC 7515 — JSON Web Signature (JWS), §1 Introduction (definition of JWS) and §4.1.1 the 'alg' Header Parameter"
    url: "https://www.rfc-editor.org/rfc/rfc7515"
    quote: "JSON Web Signature (JWS) represents content secured with digital signatures or Message Authentication Codes (MACs) using JSON-based data structures."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "RFC 7518 — JSON Web Algorithms (JWA), §3.1 'alg' (Algorithm) Header Parameter Values for JWS (ES256 Recommended+, HS256 Required, none Optional) and §3.6 Using the Algorithm 'none'"
    url: "https://www.rfc-editor.org/rfc/rfc7518"
    quote: "An Unsecured JWS uses the \"alg\" value \"none\" and is formatted identically to other JWSs, but MUST use the empty octet sequence as its JWS Signature value."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "RFC 8037 — CFRG ECDH and Signatures in JOSE, §5 IANA registration of the EdDSA 'alg' value"
    url: "https://www.rfc-editor.org/rfc/rfc8037"
    quote: "Algorithm Name: \"EdDSA\""
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "OWASP JSON Web Token Cheat Sheet — None Hashing Algorithm attack + explicit algorithm enforcement during validation (algorithm-confusion class, CWE-347 Improper Verification of Cryptographic Signature)"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html"
    quote: "This attack ... occurs when an attacker alters the token and changes the hashing algorithm to indicate, through the none keyword, that the integrity of the token has already been verified."
    quoted_at: "2026-06-01"
---

## Artifacts a third party must verify MUST use a detached asymmetric signature — never HMAC

**Impact: HIGH — an HMAC-signed certificate hands the issuer's signing secret to every verifier (any of whom can then forge), and a verifier that trusts the token's own `alg` header is forgeable via `alg:none` and ES->HS algorithm-confusion.**

The catalog already ships two symmetric-HMAC rules: `webhook-hmac-required` and `presigned-url-signature-required`. Both are correct — *because in both cases the signer and the verifier are the same party*. The webhook receiver verifies a signature it shares a secret with; the file-storage download endpoint verifies a URL its own service signed. HMAC works there precisely because the secret never leaves the trust boundary.

A third-party-verifiable artifact breaks that assumption. A course **completion certificate** an employer checks, a **signed invoice** an external auditor validates, an **exportable compliance report** a regulator opens, a **public achievement badge**, a **federated attestation** another tenant trusts — in every case the verifier is *not* the signer. With HMAC, verification requires the verifier to hold the same secret used to sign. Distributing that secret to every relying party means every relying party can now *forge* artifacts indistinguishable from the issuer's. That is not a signature; it is a shared password.

The fix is a **detached asymmetric signature**. Sign with a private key that never leaves the issuer; publish the corresponding **public** verifying key at a JWKS / `.well-known` endpoint. Use JWS (RFC 7515) with `alg` pinned to **ES256** or **EdDSA**, a `kid` in the protected header pointing at the published key, over a **canonical** serialization of the payload. Any party can verify; no party can forge.

Two verifier-side traps must be closed, both rooted in trusting the token's *own* `alg` header — which is attacker-controlled:

1. **`alg:none` (Unsecured JWS, RFC 7518 §3.6).** An empty signature is never a verified signature. Reject it.
2. **Algorithm confusion (ES/RS -> HS).** An attacker takes your *published public key bytes*, uses them as an HMAC secret to re-sign a tampered payload, and flips the header from `ES256` to `HS256`. A verifier that reads `alg` from the token and "verifies accordingly" will HMAC-verify against the public key it already trusts — and accept the forgery.

The defense is a single discipline: the verifier enforces an explicit **asymmetric-only `alg` allow-list** decided *before* it looks at the token, and selects the algorithm + key from server-side config keyed by `kid` — never from the token header.

**Incorrect — HMAC over an artifact a third party must verify (issuer secret must be shared to verify → every verifier can forge); and verifier trusts the token's alg:**

```java
// Issuer side — certificate signed with a shared symmetric secret
String jwt = Jwts.builder()
        .claim("learner", learnerId)
        .claim("course", courseId)
        .signWith(SignatureAlgorithm.HS256, SHARED_CERT_SECRET)   // ❌ symmetric
        .compact();
// To let an external employer verify, you must give them SHARED_CERT_SECRET —
// which lets the employer mint their own "valid" certificates.

// Verifier side — algorithm taken from the token itself
Jwts.parserBuilder()
        .setSigningKey(publishedKey)        // ❌ alg read from token header
        .build()                            // alg:none and HS-over-public-key both slip through
        .parseClaimsJws(token);
```

**Correct — detached asymmetric JWS over a published kid; verifier pins an asymmetric-only allow-list:**

```java
// Issuer side — private key never leaves the issuer; kid points at published JWKS
String jws = Jwts.builder()
        .setHeaderParam("kid", "cert-2026-01")
        .claim("learner", learnerId)
        .claim("course", courseId)
        .signWith(issuerEcPrivateKey, SignatureAlgorithm.ES256)   // ✅ asymmetric
        .compact();

// Verifier side (any third party) — public key only, alg allow-list enforced first
static final Set<String> ALLOWED = Set.of("ES256", "EdDSA");   // asymmetric-only

String alg = readProtectedHeaderAlg(token);          // inspect, do NOT trust
if (!ALLOWED.contains(alg)) {                        // rejects none + every HS*
    throw new SignatureVerificationException("alg not allow-listed: " + alg);
}
PublicKey verifyKey = jwks.resolvePublicKey(readKid(token));   // by kid, from config
Jwts.parserBuilder()
        .require("kid", /* expected published kid */ )
        .setSigningKey(verifyKey)                    // public key — no issuer secret
        .build()
        .parseClaimsJws(token);                      // ✅ ES256/EdDSA only, by config
```

Symmetric HMAC stays the right tool for signer==verifier envelopes — keep using `webhook-hmac-required` for inbound webhooks and `presigned-url-signature-required` for self-issued storage URLs. The boundary is who verifies: same party → HMAC; a different party → asymmetric JWS with a published key and an asymmetric-only allow-list.

Verification: review-tier. Confirm every issuer of a third-party-verifiable artifact signs with JWS `alg` ∈ {ES256, EdDSA} over a published `kid`, and every verifier (a) rejects `alg:none`, (b) rejects all `HS*` algorithms, and (c) resolves algorithm + key from server config keyed by `kid` rather than from the token's own header. No issuer secret is reachable from any third-party verification path. Spec contract: `specs/signed-artifact-l0.yaml#SIGNED-ASYM-001` (asymmetric choice) + `#SIGNED-ALG-ALLOWLIST-001` (allow-list, alg:none + alg-confusion negatives).

Reference: [RFC 7515 — JSON Web Signature (JWS)](https://www.rfc-editor.org/rfc/rfc7515)

Reference: [RFC 7518 — JSON Web Algorithms (JWA), §3.1 alg values and §3.6 Unsecured JWS](https://www.rfc-editor.org/rfc/rfc7518)

Reference: [RFC 8037 — EdDSA for JOSE](https://www.rfc-editor.org/rfc/rfc8037)

Reference: [OWASP JSON Web Token Cheat Sheet — None algorithm + explicit algorithm enforcement](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
