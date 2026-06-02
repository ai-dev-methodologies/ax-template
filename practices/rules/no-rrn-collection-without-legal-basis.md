---
title: "Backend services must not accept, store, or process raw RRN (주민등록번호) without an explicit @LegalBasis annotation"
rule_id: no-rrn-collection-without-legal-basis
impact: CRITICAL
impactDescription: "RRN is a Sensitive Personal Information (고유식별정보) under 개인정보보호법 §24; processing it without explicit statutory legal basis triggers mandatory breach notification and fines up to ₩30M per violation"
tags:
  - privacy
  - pii
  - rrn
  - identity
  - locked_constraint
  - korean-compliance
provenance_class: locked_constraint
protects_template_id: templates/backend/identity-verification/
failing_fixture_path: practices/evals/fixtures/no-rrn-collection-without-legal-basis/fail_rrn_no_legal_basis/
spec_ref: "specs/identity-verification-l0.yaml#IDV-CALLBACK-003"
verification:
  type: review
  status: manual
  notes: "Static analysis: grep -rn '@RequestParam\\|@RequestBody\\|String.*rrn\\|String.*주민' --include='*.java' | grep -v '@LegalBasis\\|//.*CORRECT\\|test/\\|fixture/' must return zero matches in production code. Structural check: VerifiedIdentity entity must have no field named rrn/residentRegistrationNumber/socialSecurityNumber."
evidence:
  - source_type: external
    citation: "개인정보보호법 제24조 제1항 — 고유식별정보의 처리 제한: 사업자는 법령에 특별한 규정이 있는 경우 외에는 주민등록번호 등 고유식별정보를 처리할 수 없음"
    url: "https://www.law.go.kr/법령/개인정보보호법"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "KISA 본인인증 가이드라인 — CI(연결정보)/DI(중복확인정보)를 이용하여 주민등록번호를 수집하지 않고 본인인증을 수행하는 방법"
    url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "OWASP ASVS V6.2.1 — Verify that regulated private data is stored encrypted at rest and that this data cannot be easily decrypted"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Backend services must not accept, store, or process raw RRN (주민등록번호) without an explicit `@LegalBasis` annotation

**Impact: CRITICAL — 개인정보보호법 §24-1 classifies the Resident Registration Number (주민등록번호) as 고유식별정보 (Unique Identification Information). Processing it without a specific statutory basis is prohibited and carries:**
- **Administrative fines up to ₩30M per violation**
- **Mandatory breach notification obligations**
- **Criminal liability for responsible officers (up to 5 years imprisonment, ₩50M fine)**

This rule is a **locked constraint**: it derives from statute and cannot be relaxed by project-level override.

**Incorrect — DTO accepts raw RRN without @LegalBasis annotation:**

```java
// VIOLATION: RRN in DTO without @LegalBasis — 개인정보보호법 §24 violation
@PostMapping("/api/users/register")
public ResponseEntity<Void> register(@RequestBody RegistrationRequest request) {
    userService.register(request.name(), request.rrn());
    return ResponseEntity.ok().build();
}
public record RegistrationRequest(String name, String email, String rrn) {}
```

**Correct — use CI/DI from KISA 본인인증 instead of RRN:**

```java
// CORRECT — identity verified via CI/DI; no RRN field in any DTO
@PostMapping("/api/users/register")
public ResponseEntity<Void> register(@RequestBody RegistrationRequest request) {
    userService.registerWithVerifiedIdentity(request.getName(), request.ci());
    return ResponseEntity.ok().build();
}
public record RegistrationRequest(String name, String email, String ci) {}
```

Reference: https://www.law.go.kr/법령/개인정보보호법

### If RRN processing is legally required (rare statutory case)

```java
// ✅ CORRECT (statutory exception only) — @LegalBasis annotation is mandatory
@PostMapping("/api/kyc/verify")
public ResponseEntity<Void> kycVerify(@RequestBody KycRequest request) {
    // CORRECT: @LegalBasis documents the specific statute
    kycService.verifyWithRrn(request.rrn());
    return ResponseEntity.ok().build();
}

public record KycRequest(
    @LegalBasis(law = "금융실명거래 및 비밀보장에 관한 법률 §3",
                purpose = "금융거래 실명확인 — 법령상 수집 의무",
                retentionYears = 5)
    String rrn   // STATUTORY EXCEPTION: documented legal basis required
) {}
```

### Why this matters

개인정보보호법 §24 and related statutes impose:
1. **Collection prohibition** — Unless a specific law (금융실명법, 주민등록법 §7의5 등) authorizes it.
2. **Separate consent requirement** — A specific, separate consent gate (§18).
3. **Encryption requirement** — If collected, must be stored encrypted (§29).
4. **Minimum necessary principle** — Collect only the minimum required for the stated purpose.

For identity verification (본인인증), KISA provides a lawful alternative:
- **PASS / KCB 본인인증** produces CI (Connecting Information) and DI (Duplicate Information)
- CI is a 64-byte hex token that uniquely identifies a person across services — **without the RRN**
- Use `templates/backend/identity-verification/` for the vendor-agnostic adapter pattern

### RRN field name patterns this rule targets

```
rrn, residentRegistrationNumber, socialSecurityNumber, idNumber (context: RRN),
주민등록번호, 주민번호, juminNumber, rrNum
```

Exclusions (false-positive guard per Risk 4 in PRD):
```
ci, di, verifiedIdentityNumber, externalId, connectingInfo, duplicateInfo
```

## Failing fixture

See: `practices/evals/fixtures/no-rrn-collection-without-legal-basis/fail_rrn_no_legal_basis/`
— A DTO with a field named `rrn` and no `@LegalBasis` annotation. Static analysis catches field name pattern.

React companion rule: `practices-react/rules/no-rrn-collection-without-legal-basis.md`

Reference: [개인정보보호법 제24조 — 고유식별정보의 처리 제한](https://www.law.go.kr/법령/개인정보보호법)

Reference: [KISA 본인인증 가이드라인 — CI/DI 대체 방법](https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO)
