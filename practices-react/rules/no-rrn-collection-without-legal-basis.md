---
title: "Frontend components must not collect or display raw RRN (주민등록번호) fields without an explicit legal-basis disclosure gate"
rule_id: no-rrn-collection-without-legal-basis
impact: CRITICAL
impactDescription: "RRN is Sensitive Personal Information under 개인정보보호법 §24-1; collecting it in a frontend form without explicit statutory authorization and a dedicated consent gate is a compliance violation"
tags:
  - privacy
  - pii
  - rrn
  - identity
  - forms
  - locked_constraint
  - korean-compliance
applicable_to:
  - react
  - nextjs
provenance_class: locked_constraint
protects_template_id: templates/L2/blocks/phone-verification-panel.tsx
failing_fixture_path: practices/evals/fixtures/no-rrn-collection-without-legal-basis/fail_rrn_no_legal_basis/
spec_ref: "specs/identity-verification-l0.yaml#IDV-CALLBACK-003"
verification:
  type: review
  status: manual
  notes: "Static check: grep -r 'name=\"rrn\"\\|name=\"주민\\|name=\"residentReg\\|id=\"rrn\"\\|placeholder.*000000-' templates/ must return zero matches. If phone-based identity is needed, use PhoneVerificationPanel which returns CI only. The rule matcher excludes: ci, di, verifiedIdentityNumber, externalId."
evidence:
  - source_type: external
    citation: "개인정보보호법 제24조 제1항 — 주민등록번호 수집은 법령에 특별한 규정이 있는 경우 외에 원칙 금지"
    url: "https://www.law.go.kr/법령/개인정보보호법"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "주민등록법 제7조의5 — 정보통신서비스 제공자는 원칙적으로 주민등록번호를 수집·이용할 수 없음"
    url: "https://www.law.go.kr/법령/주민등록법"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "KISA 본인인증 가이드라인 — PhoneVerificationPanel(PASS/KCB)로 CI/DI 수집; RRN 대체 방법"
    url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Frontend components must not collect raw RRN (주민등록번호) without legal-basis disclosure

**Impact: CRITICAL — 개인정보보호법 §24-1 prohibits collection of the Resident Registration Number without explicit statutory authorization. Frontend forms that include an RRN input field — even masked — constitute unauthorized collection.**

This rule is a **locked constraint**: it derives from statute and cannot be relaxed by project-level override.

This rule does NOT enable RRN collection. It BLOCKS unsafe RRN collection patterns.

### The violation — form with RRN input

```tsx
// ❌ WRONG — RRN collected in a standard form field
function UserRegistrationForm() {
  return (
    <form>
      <input name="name" />
      <input name="email" type="email" />
      {/* VIOLATION: RRN field — 개인정보보호법 §24 breach */}
      <input
        name="rrn"
        type="text"
        placeholder="000000-0000000"
      />
      <input
        name="주민등록번호"
        type="text"
      />
      <button type="submit">가입</button>
    </form>
  )
}
```

### Correct — use PhoneVerificationPanel with CI/DI instead

```tsx
// ✅ CORRECT — KISA 본인인증 returns CI token; no RRN collected
import PhoneVerificationPanel from 'templates/L2/blocks/phone-verification-panel'

function OnboardingPage() {
  const [verificationResult, setVerificationResult] = useState(null)

  return (
    <div>
      {/* CORRECT: panel returns CI only — never RRN */}
      <PhoneVerificationPanel
        provider="pass"
        onRequestVerification={(carrier, provider) => {
          // Launch provider popup; backend callback persists VerifiedIdentity with CI/DI
          launchVerificationPopup(carrier, provider)
        }}
        onVerified={(result) => {
          // result.ci is the cross-service unique identifier — not the RRN
          setVerificationResult(result)
        }}
      />
    </div>
  )
}
```

### If a statutory exception exists (rare)

```tsx
// ✅ CORRECT (statutory exception — very rare) — requires legal-basis disclosure UI
// LegalBasisGate is an ILLUSTRATIVE wrapper you implement (no shipped block) — it gates RRN
// collection behind a documented legal basis and renders the consent + retention notice.

function FinancialKycForm() {
  return (
    <LegalBasisGate
      law="금융실명거래 및 비밀보장에 관한 법률 §3"
      purpose="금융거래 실명확인 — 법령상 수집 의무"
      onConsentGranted={() => {/* show RRN input only after explicit consent */}}
    />
  )
}
```

### Rule matcher (fields that trigger this rule)

Pattern (fires on `name` or `id` attributes):
```
rrn, 주민등록번호, 주민번호, residentRegistrationNumber, socialSecurityNumber,
juminNumber, rrNum, id_number (context: Korean identity)
```

Exclusions (false-positive guard — these DO NOT trigger the rule):
```
ci, di, verifiedIdentityNumber, connectingInfo, duplicateInfo, externalId
```

### Why CI/DI is the correct alternative

KISA 본인인증 (PASS/KCB) provides:
- **CI** (Connecting Information): 64-byte hex token, cross-service unique person identifier
- **DI** (Duplicate Information): 64-byte hex token, per-service unique person identifier

These replace the RRN for identity correlation. Use `<PhoneVerificationPanel>` + backend
`identity-verification/` domain (SP31).

## Failing fixture

See: `practices/evals/fixtures/no-rrn-collection-without-legal-basis/fail_rrn_no_legal_basis/`
— A React form component with `name="rrn"` input. Static analysis grep catches the pattern.

Backend companion rule: `practices/rules/no-rrn-collection-without-legal-basis.md`

Reference: [개인정보보호법 제24조](https://www.law.go.kr/법령/개인정보보호법)

Reference: [KISA 본인인증 가이드라인](https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO)
