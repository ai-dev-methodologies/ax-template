---
title: "Frontend forms must not include RRN (주민등록번호) input fields by default"
rule_id: no-rrn-in-form-fields
impact: CRITICAL
impactDescription: "RRN is Sensitive Personal Information under 개인정보보호법 §24; collecting it through a standard form field without explicit legal basis and consent gate is a compliance violation"
tags:
  - privacy
  - pii
  - rrn
  - forms
  - locked_constraint
  - korean-compliance
applicable_to:
  - react
  - nextjs
provenance_class: locked_constraint
protects_template_id: templates/L2/blocks/
failing_fixture_path: practices/evals/fixtures/no-rrn-in-form-fields/fail_rrn_field/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-004"
verification:
  type: review
  status: manual
  notes: "Static check: grep -r 'name=\"rrn\"\\|name=\"주민\\|id=\"rrn\"' templates/L2/ templates/L4/ must return zero matches. If identity verification is required, it must be in a dedicated KYC component with explicit legal-basis display and PII-handling review."
evidence:
  - source_type: external
    citation: "개인정보보호법 제24조 — 고유식별정보의 처리 제한: RRN (주민등록번호) is a unique identification number; its collection requires explicit legal basis, separate consent, and technical/administrative safeguards"
    url: "https://www.law.go.kr/법령/개인정보보호법"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "KISA 개인정보보호법 가이드라인 — 주민등록번호 처리: 기업은 법령에 특별한 규정이 있는 경우가 아닌 한 주민등록번호를 처리할 수 없음"
    url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "행정안전부 주민등록법 제7조의5 — 주민등록번호의 사용 제한: 정보통신서비스 제공자는 원칙적으로 주민등록번호를 수집·이용할 수 없음"
    url: "https://www.law.go.kr/법령/주민등록법"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Frontend forms must not include RRN (주민등록번호) input fields by default

**Impact: CRITICAL — 개인정보보호법 §24 classifies the Resident Registration Number as a unique identification information (고유식별정보). Collecting it through a standard form field without explicit legal basis and a dedicated consent gate is a compliance violation carrying administrative penalties up to ₩30M per violation.**

This rule is a **locked constraint** derived from statute. It cannot be relaxed by project-level override.

### The violation — standard form with RRN field

```tsx
// ❌ WRONG — form with name="rrn" input field
export default function RegistrationForm() {
  return (
    <form>
      <input name="name" ... />
      <input name="email" type="email" ... />
      {/* VIOLATION: RRN collected as a standard form field */}
      <input
        name="rrn"
        id="rrn"
        placeholder="000000-0000000"
        type="text"
      />
      <button type="submit">Register</button>
    </form>
  );
}
```

### Correct — registration form without RRN field

```tsx
// ✅ CORRECT — collect only minimum required information
export default function RegistrationForm() {
  return (
    <form>
      <input name="name" ... />
      <input name="email" type="email" ... />
      {/* CORRECT: no RRN field
          If identity verification is later required, use <KycVerificationModal/>
          which includes: legal-basis disclosure + separate consent + audit trail */}
      <button type="submit">Register</button>
    </form>
  );
}
```

### If identity verification is required

Use the dedicated `<KycVerificationModal>` component with mandatory legal-basis display:

```tsx
// ✅ CORRECT — KYC flow with explicit consent gate
import { KycVerificationModal } from "templates/L2/blocks/kyc-verification-modal";

export default function OnboardingPage() {
  const [kycOpen, setKycOpen] = useState(false);
  return (
    <div>
      <button onClick={() => setKycOpen(true)}>Verify Identity</button>
      <KycVerificationModal
        open={kycOpen}
        legalBasis="본인 확인을 위해 주민등록번호 뒷자리를 수집합니다 (개인정보보호법 §24)"
        onVerified={(result) => { /* result contains only a verification token, not RRN */ }}
        onClose={() => setKycOpen(false)}
      />
    </div>
  );
}
```

### Why this rule exists

개인정보보호법 §24 and 주민등록법 §7의5 impose strict restrictions:
1. **Collection prohibition** — The RRN may not be collected without specific legal authorization (주민등록법 §7의5).
2. **Consent requirement** — A separate, explicit consent gate is required (개인정보보호법 §18).
3. **Encryption requirement** — If collected, must be stored encrypted (개인정보보호법 §29).
4. **Penalties** — Unauthorized collection triggers mandatory breach notification and fines up to ₩30M per violation.

Standard form fields do not satisfy any of these requirements. A `<KycVerificationModal>` component with legal-basis disclosure and audit logging is the only acceptable collection path.

Frontend companion to: `no-rrn-logging.md` in `practices/rules/`.

Reference: [개인정보보호법 제24조 — 고유식별정보의 처리 제한](https://www.law.go.kr/법령/개인정보보호법)

Reference: [KISA 개인정보보호법 가이드라인 — 주민등록번호 처리](https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO)
