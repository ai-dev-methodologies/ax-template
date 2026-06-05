---
title: Electronic government approval MUST be GPKI-signed with a sequential approval line, post-approval and security-grade escalation, and long-term tamper-evident retention
impact: HIGH
impactDescription: "An e-approval (전자결재) without a GPKI/NPKI signature has no legal effect as an official document; one with no enforced sequential approval line lets a step be skipped or approved out of order; one with no post-approval path stalls urgent actions taken before formal sign-off; one whose security grade does not escalate the approval line under-reviews classified documents; one not retained tamper-evidently for the statutory period (30 years) fails an audit. Each breaks the legal validity or governance of the official record."
tags:
  - e-government
  - approval-workflow
  - gpki
  - pki
  - retention
  - compliance
spec_ref: "specs/e-government-approval-l0.yaml#EGA-PKI-001"
verification:
  type: review
  source: "specs/e-government-approval-l0.yaml#EGA-PKI-001"
  pattern: "Electronic government approval MUST be signed with a trusted government PKI certificate — GPKI/NPKI, an X.509 certificate chain (the profile RFC 5280 defines) validated to the government trust root — giving the approval legal force (EGA-PKI-001). The document acquires legal effect (효력) only when the approval ceremony completes per the governing statute (전자정부법 / 전자문서법) (EGA-FRAMEWORK-001). Approval follows an ordered approval line (결재선) — sequential steps with delegation support, each step approved in order by an authorized approver (EGA-LINE-001, composes approval-workflow). A post-approval (사후결재) path records a formally-ratified action taken before sign-off, never a silent bypass (EGA-POSTAPPROVAL-001). The document's security grade (보안등급) drives the approval line — a higher classification auto-escalates the required approvers (EGA-CLASSIFICATION-001). The approval record + its full flow is retained tamper-evidently for the statutory period — 30 years (EGA-AUDIT-001, composes tamper-evident-log). Reject an unsigned approval, an out-of-order or skipped approval step, a post-approval with no formal ratification record, and a classified document that does not escalate its approval line."
upstream:
  - "https://www.rfc-editor.org/rfc/rfc5280"
evidence:
  - source_type: external
    citation: "RFC 5280 — Internet X.509 PKI Certificate and CRL Profile (Abstract)"
    url: "https://www.rfc-editor.org/rfc/rfc5280"
    quote: "This memo profiles the X.509 v3 certificate and X.509 v2 certificate revocation list (CRL) for use in the Internet."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Electronic government approval MUST be GPKI-signed with a sequential approval line, post-approval and security-grade escalation, and long-term tamper-evident retention

**Impact: HIGH — 전자결재 (electronic government approval) produces official records with legal effect, and that effect rests on a trusted government PKI signature. GPKI/NPKI certificates are X.509 certificates — RFC 5280 *profiles the X.509 v3 certificate and X.509 v2 certificate revocation list (CRL) for use in the Internet*, the chain that must validate to the government trust root for an approval to be valid. Around that signature sit the governance requirements: an approval acquires legal effect (효력) only when the statutory ceremony completes (전자정부법 / 전자문서법); the approval must traverse the ordered approval line (결재선); urgent actions need a formal post-approval (사후결재) path rather than a silent bypass; classified documents must escalate their approval line by security grade; and the record must be retained tamper-evidently for 30 years. A skipped step, an unsigned approval, or a mutable record voids the official document.**

There are six load-bearing requirements — the items of `specs/e-government-approval-l0.yaml`, all governed by this rule.

**1. Legal effect framework (EGA-FRAMEWORK-001).** The document gains legal effect (효력) only when the approval completes per the governing statute (전자정부법 §29-30 / 전자문서법) — the system models that ceremony, not an ad-hoc "approved" boolean.

**2. GPKI/NPKI trust chain (EGA-PKI-001).** Each approval is signed with a government PKI (GPKI/NPKI) X.509 certificate validated to the government trust root (with revocation checking) — an untrusted or self-signed certificate yields no valid approval.

**3. Sequential approval line (EGA-LINE-001).** Approval traverses an ordered 결재선 — sequential steps, each approved in order by an authorized approver, with delegation (전결/대결) support. A step cannot be skipped or approved out of order. Composes the `approval-workflow` domain.

**4. Post-approval (EGA-POSTAPPROVAL-001).** An action taken before formal sign-off (urgency) is captured by a 사후결재 path that records the formal ratification afterward — never a silent bypass that leaves no approval record.

**5. Security-grade escalation (EGA-CLASSIFICATION-001).** The document's 보안등급 (security classification) drives the approval line: a higher grade automatically escalates the required approvers, so classified documents are not under-reviewed.

**6. 30-year tamper-evident retention (EGA-AUDIT-001).** The approval record and its full flow (who approved, when, in what order) are retained tamper-evidently for the statutory 30 years. Composes the `tamper-evident-log` pattern.

**Incorrect — boolean "approved" flag, no signature, no ordered line:**

```java
void approve(Document d, User approver) {
    d.setApproved(true);              // VIOLATION: a boolean, no GPKI signature → no legal effect (EGA-PKI/FRAMEWORK)
    d.setApprover(approver.id());     // VIOLATION: no ordered 결재선, any approver, any order (EGA-LINE-001)
    docRepo.save(d);                  // VIOLATION: mutable record, no tamper-evident 30-year retention (EGA-AUDIT-001)
}
```

**Correct — GPKI-signed step on an ordered line, grade-escalated, tamper-evidently recorded:**

```java
ApprovalStep approve(ApprovalRequest req, User approver, GpkiCert cert) {
    ApprovalLine line = req.line();                          // ordered 결재선 (EGA-LINE-001)
    line.assertNextApprover(approver);                       // sequential, in order, authorized
    pki.verifyChainToGovRoot(cert);                          // GPKI/NPKI X.509 to trust root (EGA-PKI-001)
    ApprovalStep step = line.signStep(approver, pki.sign(req, cert)); // signed → contributes to 효력 (EGA-FRAMEWORK-001)
    tamperLog.append(step);                                  // tamper-evident, 30-yr retention (EGA-AUDIT-001)
    return step;
}
// security grade escalates the line (EGA-CLASSIFICATION-001); 사후결재 records formal ratification (EGA-POSTAPPROVAL-001).
```

Verification: review-tier. E-approval validity is a legal/governance property with no compile-time signal — an "approved=true" flag compiles and looks done while being legally void and un-auditable. Verify by review against `specs/e-government-approval-l0.yaml`: approvals are GPKI/NPKI X.509-signed and validated to the gov root; legal effect follows the statutory ceremony; the approval line is sequential with delegation and no skipping; post-approval is formally ratified; security grade escalates the line; the record is tamper-evidently retained 30 years. When a fork-receiver wires real ITs (out-of-order approval rejected; unsigned approval rejected), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [RFC 5280 — X.509 PKI Certificate and CRL Profile](https://www.rfc-editor.org/rfc/rfc5280)
