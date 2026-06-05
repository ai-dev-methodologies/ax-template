---
title: A signed document MUST use a declared standard signature format over a verified PKI chain, with a trusted timestamp, revocation-checked verification, and long-term retention
impact: HIGH
impactDescription: "A signature that is not validated against a trusted certificate chain can be forged by any self-signed key; one with no trusted timestamp cannot prove WHEN it was signed (so a back-dated or post-expiry signature passes); one verified without a revocation check accepts a signature from a stolen/revoked certificate; one not retained for the legal period cannot be re-verified years later when disputed. Each gap voids the legal weight the signature was supposed to carry."
tags:
  - digital-signature
  - pki
  - x509
  - timestamp
  - revocation
  - non-repudiation
spec_ref: "specs/document-signing-l0.yaml#DOC-CERT-001"
verification:
  type: review
  source: "specs/document-signing-l0.yaml#DOC-CERT-001"
  pattern: "A signed document MUST use a DECLARED standard signature format (XMLDSig / PAdES / CAdES per the artifact type), not an ad-hoc scheme (DOC-FORMAT-001). The signing certificate MUST chain to a trusted root and be validated against that PKI trust hierarchy — a self-signed or untrusted-chain certificate is rejected (DOC-CERT-001). The signer identity is bound to the certificate subject; any RRN/national-id is stored separately and never logged (composes no-rrn-logging) (DOC-IDENTITY-001). A trusted RFC 3161 time-stamp token from a TSA proves the signing time, so signing time is not the signer's own clock (DOC-TIMESTAMP-001). Verification MUST include a revocation check (OCSP/CRL) — a signature from a revoked or expired certificate is invalid even if the math checks out (DOC-VERIFY-001). The signed artifact + its validation material (chain, timestamp, revocation evidence) are retained for the legal period (e.g. 10 years) so the signature is re-verifiable long-term (DOC-RETENTION-001). Reject a signature accepted without chain validation, without a trusted timestamp, or without a revocation check."
upstream:
  - "https://www.rfc-editor.org/rfc/rfc5280"
  - "https://www.rfc-editor.org/rfc/rfc3161"
  - "https://www.rfc-editor.org/rfc/rfc6960"
evidence:
  - source_type: external
    citation: "RFC 5280 — Internet X.509 PKI Certificate and CRL Profile (Abstract)"
    url: "https://www.rfc-editor.org/rfc/rfc5280"
    quote: "This memo profiles the X.509 v3 certificate and X.509 v2 certificate revocation list (CRL) for use in the Internet."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "RFC 3161 — Time-Stamp Protocol (TSP) (Section 1)"
    url: "https://www.rfc-editor.org/rfc/rfc3161"
    quote: "A time-stamping service supports assertions of proof that a datum existed before a particular time."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "RFC 6960 — X.509 Online Certificate Status Protocol (OCSP) (Abstract)"
    url: "https://www.rfc-editor.org/rfc/rfc6960"
    quote: "This document specifies a protocol useful in determining the current status of a digital certificate without requiring Certificate Revocation Lists (CRLs)."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A signed document MUST use a declared format over a verified PKI chain, with a trusted timestamp, revocation-checked verification, and long-term retention

**Impact: HIGH — A digital signature only carries legal weight (non-repudiation) if every link of its trust chain holds. The signing certificate must chain to a trusted root — RFC 5280 *profiles the X.509 v3 certificate and X.509 v2 certificate revocation list (CRL) for use in the Internet* — because a signature validated against an untrusted or self-signed certificate can be forged by anyone. The signing time must come from a trusted timestamp, not the signer's clock — per RFC 3161, *a time-stamping service supports assertions of proof that a datum existed before a particular time* — or a back-dated or post-expiry signature passes. And verification must check revocation — per RFC 6960, OCSP is *a protocol useful in determining the current status of a digital certificate without requiring Certificate Revocation Lists (CRLs)* — because a stolen or revoked certificate's signature must be rejected even when the cryptographic math verifies.**

There are six load-bearing requirements — the items of `specs/document-signing-l0.yaml`, all governed by this rule.

**1. Declared signature format (DOC-FORMAT-001).** The signature uses a declared standard format appropriate to the artifact — XMLDSig for XML, PAdES (ISO 32000-2 / ETSI EN 319 142) for PDF, CAdES for binary — not an ad-hoc home-grown scheme that no third party can validate.

**2. PKI trust chain (DOC-CERT-001).** The signing certificate is validated up to a trusted root through the X.509 chain. A self-signed certificate, or one whose chain does not terminate at a trusted anchor, is rejected.

**3. Signer identity, PII-separated (DOC-IDENTITY-001).** The signer is the validated certificate subject. Any national identifier (RRN) tied to the identity is stored separately and never logged — composes the no-RRN-logging discipline.

**4. Trusted timestamp (DOC-TIMESTAMP-001).** An RFC 3161 time-stamp token from a TSA establishes WHEN the document was signed, independent of the signer's own clock — so a signature cannot be back-dated and a post-expiry signature is detectable.

**5. Revocation-checked verification (DOC-VERIFY-001).** Verification includes a revocation check (OCSP or CRL): a signature from a revoked or expired certificate is INVALID even if the signature value verifies against the public key. Math-only verification is insufficient.

**6. Long-term retention (DOC-RETENTION-001).** The signed artifact AND its validation material — the certificate chain, the timestamp token, and the revocation evidence captured at signing time — are retained for the legal period (e.g. 10 years), so the signature remains re-verifiable long after the certificate itself expires.

**Incorrect — verifies the signature math only; no chain validation, no timestamp, no revocation check:**

```java
boolean verify(byte[] doc, byte[] sig, PublicKey key) {
    Signature s = Signature.getInstance("SHA256withRSA");
    s.initVerify(key);                       // VIOLATION: trusts a raw key, no chain to a trusted root (DOC-CERT-001)
    s.update(doc);
    return s.verify(sig);                     // VIOLATION: no timestamp (DOC-TIMESTAMP), no revocation check (DOC-VERIFY)
    // a signature from a self-signed or REVOKED cert passes; signing time is unknowable
}
```

**Correct — validate chain to trusted root, require RFC 3161 timestamp, OCSP/CRL revocation check, retain evidence:**

```java
SignatureValidation verify(SignedDocument d) {
    CertPath chain = pki.buildAndValidate(d.signerCert(), trustAnchors);   // chain to trusted root (DOC-CERT-001)
    revocation.check(chain);                                               // OCSP/CRL — reject revoked (DOC-VERIFY-001)
    Instant signedAt = tsa.verifyTimestampToken(d.timestampToken(), d);    // RFC 3161 trusted time (DOC-TIMESTAMP-001)
    if (!d.certValidAt(signedAt)) throw new SignatureInvalid("signed outside cert validity");
    boolean ok = d.format().validate(d);                                   // declared format (DOC-FORMAT-001)
    archive.retain(d, chain, d.timestampToken(), revocation.evidence(),     // long-term (DOC-RETENTION-001)
                   Period.ofYears(10));
    return new SignatureValidation(ok, signedAt, d.signerSubject());        // identity = validated subject (DOC-IDENTITY-001)
}
```

Verification: review-tier. Signature validity is a trust-chain property — a math-only verify compiles and accepts legitimate signatures while silently accepting forged/revoked ones. Verify by review against `specs/document-signing-l0.yaml`: a declared standard format; the cert chains to a trusted root; signer identity is the validated subject with RRN stored separately/unlogged; an RFC 3161 timestamp establishes signing time; verification performs an OCSP/CRL revocation check; the artifact + validation material are retained for the legal period. When a fork-receiver wires a real IT (a revoked-cert signature is rejected; a self-signed chain is rejected), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [RFC 5280 — X.509 PKI Certificate and CRL Profile](https://www.rfc-editor.org/rfc/rfc5280)

Reference: [RFC 3161 — Time-Stamp Protocol](https://www.rfc-editor.org/rfc/rfc3161)

Reference: [RFC 6960 — OCSP](https://www.rfc-editor.org/rfc/rfc6960)
