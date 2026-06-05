---
title: Customs e-trade MUST classify by WCO HS code, exchange UN/EDIFACT messages over authenticated certs, honor AEO status, and retain declarations for the statutory period
impact: HIGH
impactDescription: "A customs declaration with a wrong/ad-hoc commodity code is mis-tariffed and rejected by the customs authority; one not in the agreed EDI message format cannot be transmitted to UNI-PASS at all; one sent without the authenticated customs certificate is unauthenticated; AEO status ignored loses the facilitation the operator is entitled to; declarations not retained for the statutory period (관세법 §38, 5 years) fail an audit. Each gap blocks legitimate trade or breaches customs law."
tags:
  - customs
  - hs-code
  - edifact
  - aeo
  - international-trade
  - compliance
spec_ref: "specs/customs-e-trade-l0.yaml#CUSTOMS-CLASSIFICATION-001"
verification:
  type: review
  source: "specs/customs-e-trade-l0.yaml#CUSTOMS-CLASSIFICATION-001"
  pattern: "Customs e-trade MUST classify every traded item by a valid WCO Harmonized System (HS) code — the international product nomenclature — not an ad-hoc internal category (CUSTOMS-CLASSIFICATION-001). Declarations are submitted to the customs authority (관세청 UNI-PASS) in the required electronic form (CUSTOMS-DECLARATION-001) using the agreed UN/EDIFACT message format/version (D.16B) (CUSTOMS-EDI-001). Messages are authenticated with the customs-authority certificate and the operator's EORI/identification number (CUSTOMS-CERT-001). An operator's AEO (Authorized Economic Operator) status, where held, is honored for the facilitation it grants (CUSTOMS-AEO-001). Declarations and their audit trail are retained for the statutory period — 관세법 §38, five years (CUSTOMS-AUDIT-001). Reject an ad-hoc commodity code, a declaration not in the agreed EDI format, an unauthenticated submission, and a declaration store with no statutory retention."
upstream:
  - "https://www.wcoomd.org/en/topics/nomenclature/overview/what-is-the-harmonized-system.aspx"
  - "https://www.rfc-editor.org/rfc/rfc5280"
  - "https://unece.org/trade/uncefact/introducing-unedifact"
evidence:
  - source_type: external
    citation: "World Customs Organization — What is the Harmonized System (HS)"
    url: "https://www.wcoomd.org/en/topics/nomenclature/overview/what-is-the-harmonized-system.aspx"
    quote: "The Harmonized Commodity Description and Coding System generally referred to as \"Harmonized System\" or simply \"HS\" is a multipurpose international product nomenclature developed by the World Customs Organization (WCO)."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "World Customs Organization — What is the Harmonized System (global adoption)"
    url: "https://www.wcoomd.org/en/topics/nomenclature/overview/what-is-the-harmonized-system.aspx"
    quote: "The system is used by more than 200 countries and economies as a basis for their Customs tariffs and for the collection of international trade statistics."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "RFC 5280 — Internet X.509 PKI Certificate and CRL Profile (Abstract)"
    url: "https://www.rfc-editor.org/rfc/rfc5280"
    quote: "This memo profiles the X.509 v3 certificate and X.509 v2 certificate revocation list (CRL) for use in the Internet."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Customs e-trade MUST classify by WCO HS code, exchange UN/EDIFACT over authenticated certs, honor AEO, and retain for the statutory period

**Impact: HIGH — Cross-border customs declarations are interoperability- and compliance-critical: they must speak the global language of trade or they are rejected at the border. The commodity code must be a WCO Harmonized System code — per the WCO, *the Harmonized Commodity Description and Coding System ... is a multipurpose international product nomenclature developed by the World Customs Organization (WCO)*, and *the system is used by more than 200 countries and economies as a basis for their Customs tariffs and for the collection of international trade statistics*. The message must be in the agreed UN/EDIFACT format to reach the customs system (관세청 UNI-PASS) at all, authenticated with the customs certificate (an X.509 certificate, the profile RFC 5280 defines), and the declaration retained for the statutory period or an audit fails.**

There are six load-bearing requirements — the items of `specs/customs-e-trade-l0.yaml`, all governed by this rule.

**1. WCO HS classification (CUSTOMS-CLASSIFICATION-001).** Every traded item carries a valid WCO Harmonized System code (the 6-digit international code, extended nationally). An ad-hoc internal category cannot be tariffed and is rejected; the HS code is the basis for the tariff and the trade statistics.

**2. Declaration submission to UNI-PASS (CUSTOMS-DECLARATION-001).** Import/export declarations are submitted electronically to the customs authority (관세청 UNI-PASS) in the required structured form, with the mandatory fields the authority validates.

**3. UN/EDIFACT message format (CUSTOMS-EDI-001).** Messages use the agreed UN/EDIFACT directory/version (D.16B) — the United Nations rules for Electronic Data Interchange for Administration, Commerce and Transport — so the customs system can parse them. A non-conforming message cannot be transmitted.

**4. Authenticated certificate + EORI (CUSTOMS-CERT-001).** Submissions are authenticated with the customs-authority certificate (an X.509 certificate per RFC 5280) and carry the operator's EORI / identification number, so the sender is verified and the submission non-repudiable.

**5. AEO status (CUSTOMS-AEO-001).** Where the operator holds Authorized Economic Operator status (WCO SAFE Framework), the system honors it for the trade-facilitation benefits (expedited processing, reduced inspection) it confers.

**6. Statutory retention + audit (CUSTOMS-AUDIT-001).** Declarations and their audit trail are retained for the statutory period — 관세법 §38, five years — and are tamper-evidently auditable, so a post-clearance audit can reconstruct every declaration.

**Incorrect — ad-hoc internal code, no EDI format, unauthenticated, no retention:**

```java
void declare(Shipment s) {
    var decl = new Declaration(s.internalCategory(), s.value());  // VIOLATION: internal category, not an HS code (CLASSIFICATION)
    http.post(UNIPASS_URL, toJson(decl));                         // VIOLATION: JSON, not UN/EDIFACT D.16B (EDI); unauthenticated (CERT)
    // VIOLATION: nothing retained for the 5-year statutory period (AUDIT)
}
```

**Correct — HS-coded, UN/EDIFACT, certificate-authenticated, AEO-aware, retained 5 years:**

```java
void declare(Shipment s) {
    HsCode hs = hsClassifier.classify(s.product());              // valid WCO HS code (CUSTOMS-CLASSIFICATION-001)
    EdifactMessage msg = edifact.build("CUSDEC", "D.16B", hs, s, operator.eori()); // UN/EDIFACT (CUSTOMS-EDI-001)
    SignedMessage signed = customsCert.sign(msg);               // X.509 customs cert + EORI (CUSTOMS-CERT-001)
    UnipassResponse r = unipass.submit(signed, operator.aeoStatus()); // UNI-PASS, AEO-aware (DECLARATION/AEO)
    declarationStore.retain(signed, r, Period.ofYears(5));      // 관세법 §38 retention (CUSTOMS-AUDIT-001)
}
```

Verification: review-tier. Customs conformance is an interoperability + compliance property with no compile-time signal — an internal-category JSON "declaration" compiles and is simply rejected by the authority, or passes an internal demo while being non-compliant. Verify by review against `specs/customs-e-trade-l0.yaml`: items carry valid WCO HS codes; declarations submit to UNI-PASS in UN/EDIFACT D.16B; submissions are authenticated with the customs X.509 certificate + EORI; AEO status is honored; declarations are retained 5 years per 관세법 §38 with an audit trail. When a fork-receiver wires a real test (HS code validated against the WCO table; EDIFACT message schema-validated), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [WCO — What is the Harmonized System](https://www.wcoomd.org/en/topics/nomenclature/overview/what-is-the-harmonized-system.aspx)

Reference: [RFC 5280 — X.509 PKI Certificate and CRL Profile](https://www.rfc-editor.org/rfc/rfc5280)
