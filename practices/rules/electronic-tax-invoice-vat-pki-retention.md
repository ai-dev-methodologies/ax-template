---
title: An electronic tax invoice MUST use the standard format, separate identifier PII, balance VAT, transmit to the authority, be PKI-signed and timestamped, and retain for the statutory period
impact: HIGH
impactDescription: "A 전자세금계산서 with a wrong VAT computation is a tax misstatement; one not transmitted to the 국세청 within the mandated window is a non-issuance penalty; one not PKI-signed has no legal force as an electronic document; one storing the 사업자등록번호/RRN unseparated leaks identifier PII; one not retained 5 years (국세기본법) fails a tax audit. Each is a statutory breach, not a cosmetic defect."
tags:
  - invoicing
  - e-tax-invoice
  - vat
  - pki-signature
  - retention
  - compliance
spec_ref: "specs/invoicing-l0.yaml#INV-SIGN-001"
verification:
  type: review
  source: "specs/invoicing-l0.yaml#INV-SIGN-001"
  pattern: "An electronic tax invoice MUST be PKI-signed for legal force — an X.509 certificate-based digital signature (the profile RFC 5280 defines) with a trusted timestamp (RFC 3161) so the issuance instant is provable (INV-SIGN-001). It MUST use the standard 전자세금계산서 format/schema (INV-FORMAT-001). Business identifiers (사업자등록번호) and any RRN MUST be stored separated from the document body and never logged (INV-IDENTIFIER-001, composes no-rrn-logging). The VAT computation MUST be internally consistent — supply value + tax == total, with exact decimal arithmetic (INV-AMOUNT-001, composes BigDecimal-for-money). The invoice MUST be transmitted to the tax authority (국세청 NTS) within the statutory window via the NTS API (INV-NTS-001). A correction is issued as a 수정세금계산서 that references the original — never an in-place edit of an issued invoice (INV-CORRECT-001). The invoice is retained for the statutory period — 국세기본법, five years (INV-RETENTION-001). Reject an unsigned/untimestamped invoice, an in-place edit of an issued invoice, a VAT total that does not foot, and identifier PII stored in the clear in the document body."
upstream:
  - "https://www.rfc-editor.org/rfc/rfc5280"
  - "https://www.rfc-editor.org/rfc/rfc3161"
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
decided_at: "2026-06-06"
---

## An electronic tax invoice MUST be standard-format, identifier-PII-separated, VAT-consistent, authority-transmitted, PKI-signed + timestamped, and retained

**Impact: HIGH — A 전자세금계산서 (electronic tax invoice) is a legal financial instrument, and every requirement on it is statutory. Its legal force comes from a PKI signature over an X.509 certificate — the profile RFC 5280 *profiles the X.509 v3 certificate and X.509 v2 certificate revocation list (CRL) for use in the Internet* — plus a trusted timestamp proving the issuance instant — RFC 3161's *time-stamping service supports assertions of proof that a datum existed before a particular time*. Beyond the signature, a wrong VAT total is a tax misstatement, a missed 국세청 transmission window is a penalty, an unseparated 사업자등록번호/RRN is an identifier-PII leak, and a sub-5-year retention fails a tax audit (국세기본법). The PDF/JSON looking right is not enough; each statutory facet must hold.**

There are seven load-bearing requirements — the items of `specs/invoicing-l0.yaml`, all governed by this rule.

**1. Standard format (INV-FORMAT-001).** The invoice uses the standard 전자세금계산서 XML schema/format so the tax authority and counterparties can parse and validate it — not an internal ad-hoc layout.

**2. Identifier PII separation (INV-IDENTIFIER-001).** The 사업자등록번호 and any RRN are stored separated from the document body (a referenced record), never logged — composing the no-RRN-logging discipline.

**3. VAT consistency (INV-AMOUNT-001).** The VAT computation foots: supply value + tax == total, computed with exact decimal arithmetic (composing BigDecimal-for-money). A total that does not balance is a tax misstatement.

**4. NTS transmission (INV-NTS-001).** The invoice is transmitted to the 국세청 (NTS) within the statutory window (e.g. by the day after issuance) via the NTS API — a late or missing transmission is a penalty under 부가가치세법.

**5. PKI signature + timestamp (INV-SIGN-001).** The invoice is signed with an X.509 certificate-based digital signature and carries an RFC 3161 timestamp, giving it legal force and a provable issuance instant.

**6. Correction via 수정세금계산서 (INV-CORRECT-001).** A correction is issued as a NEW 수정세금계산서 that references the original invoice — an issued invoice is immutable and is NEVER edited in place (which would destroy the audit trail and the signature).

**7. Statutory retention (INV-RETENTION-001).** The invoice (and its signature/timestamp material) is retained for the statutory period — 국세기본법, five years — so it remains verifiable in a tax audit.

**Incorrect — edits an issued invoice in place, no signature, VAT not re-derived:**

```java
void correctInvoice(Invoice inv, BigDecimal newTotal) {
    inv.setTotal(newTotal);          // VIOLATION: in-place edit of an issued invoice (INV-CORRECT-001)
    invoiceRepo.save(inv);           // VIOLATION: no 수정세금계산서, no re-signature, audit trail destroyed
    // VIOLATION: total set directly, VAT not re-derived/validated (INV-AMOUNT-001)
}
```

**Correct — immutable original; correction is a new signed/timestamped 수정세금계산서 transmitted to NTS:**

```java
SusungInvoice correct(Invoice original, InvoiceLines revised) {
    SusungInvoice s = SusungInvoice.referencing(original);       // new corrective invoice (INV-CORRECT-001)
    s.setLines(revised);
    s.setVat(vat.compute(revised));                              // supply + tax == total, BigDecimal (INV-AMOUNT-001)
    Signed signed = pki.signWithTimestamp(s.toStandardXml());    // X.509 + RFC 3161 (INV-SIGN-001 / INV-FORMAT-001)
    nts.transmitWithinWindow(signed);                           // 국세청 NTS (INV-NTS-001)
    archive.retain(signed, Period.ofYears(5));                  // 국세기본법 retention (INV-RETENTION-001)
    return s;                                                    // original stays immutable
}
```

Verification: review-tier. Tax-invoice compliance is a statutory property with no compile-time signal — an unsigned, in-place-edited invoice compiles and renders fine while being legally void and audit-failing. Verify by review against `specs/invoicing-l0.yaml`: standard 전자세금계산서 format; identifier PII separated and unlogged; VAT foots with exact decimals; transmitted to NTS within the window; PKI-signed + RFC 3161 timestamped; corrections issued as a referencing 수정세금계산서 (never in-place); retained 5 years per 국세기본법. When a fork-receiver wires real tests (VAT total invariant; an issued invoice rejects mutation; signature verifies), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [RFC 5280 — X.509 PKI Certificate and CRL Profile](https://www.rfc-editor.org/rfc/rfc5280)

Reference: [RFC 3161 — Time-Stamp Protocol](https://www.rfc-editor.org/rfc/rfc3161)
