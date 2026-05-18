---
title: "Frontend must validate 사업자등록번호 (Business Registration Number) checksum using the NTS algorithm before accepting the value"
rule_id: business-registration-checksum-required
impact: HIGH
impactDescription: "Accepting an invalid 사업자등록번호 causes tax-invoice issuance failures (세금계산서 오류) and B2B billing rejections; the NTS (국세청) algorithm is deterministic and must be applied client-side for immediate feedback"
tags:
  - form-validation
  - business-registration
  - korean-compliance
  - checksum
  - b2b
applicable_to:
  - react
  - nextjs
provenance_class: locked_constraint
protects_template_id: templates/L1/components/business-registration-input.tsx
failing_fixture_path: practices/evals/fixtures/business-registration-checksum/fail_invalid_checksum/
spec_ref: "specs/identity-verification-l0.yaml"
verification:
  type: review
  status: manual
  notes: "Component test: validateBusinessRegistration() from business-registration-input.tsx must be called in onBlur or onSubmit with all BRN inputs. Static check: any <input name='businessNo' | name='brn' | name='사업자등록번호'> must have an onBlur or onChange handler that calls validateBusinessRegistration."
evidence:
  - source_type: external
    citation: "국세청 사업자등록번호 검증 알고리즘 — 승수 [1,3,7,1,3,7,1,3,5]; 9번째 자리는 floor(5×d9/10) + (5×d9)%10 처리; 체크자리 = (10 - sum%10) % 10"
    url: "https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2227&cntntsId=7870"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "행정안전부 공공데이터포털 — 사업자 등록 정보 공개 데이터셋: https://www.data.go.kr/data/15081808/fileData.do"
    url: "https://www.data.go.kr/data/15081808/fileData.do"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "국세청 전자세금계산서 발행 규정 — 사업자등록번호 정확성 필수: 오류 번호로 발행된 세금계산서는 국세청 수령 거부"
    url: "https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2390"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Frontend must validate 사업자등록번호 checksum using the NTS algorithm before submitting

**Impact: HIGH — The Korean National Tax Service (NTS, 국세청) rejects tax invoices (세금계산서) issued with invalid 사업자등록번호. Client-side checksum validation provides immediate feedback and prevents backend round-trips for deterministically invalid numbers.**

### Algorithm (국세청 공식 — multiplier sequence [1,3,7,1,3,7,1,3,5])

The 10-digit 사업자등록번호 (format: XXX-XX-XXXXX) uses a weighted checksum:

```
weights = [1, 3, 7, 1, 3, 7, 1, 3, 5]
sum  = Σ(digits[i] × weights[i]) for i = 0..7
sum += floor(digits[8] × 5 / 10)   // 9th digit: integer part
sum += (digits[8] × 5) % 10        // 9th digit: remainder part (special case)
checkDigit = (10 - (sum % 10)) % 10
valid = (checkDigit === digits[9])
```

**Note:** A valid checksum does not confirm the business is currently registered. Server-side NTS API verification (`사업자등록증명원 API`) is required for live status checks.

### The violation — input without checksum validation

```tsx
// ❌ WRONG — accepts any 10-digit string; invalid BRNs cause downstream failures
function BusinessRegistrationForm() {
  const [brn, setBrn] = useState('')
  return (
    <form onSubmit={submitTaxInvoice}>
      {/* VIOLATION: no validateBusinessRegistration() call before submit */}
      <input
        name="businessNo"
        value={brn}
        onChange={e => setBrn(e.target.value)}
        placeholder="000-00-00000"
      />
      <button type="submit">세금계산서 발행</button>
    </form>
  )
}
```

### Correct — checksum validated on blur and on submit

```tsx
// ✅ CORRECT — use the L1 primitive with built-in NTS checksum
import BusinessRegistrationInput, {
  validateBusinessRegistration
} from 'templates/L1/components/business-registration-input'

function BusinessRegistrationForm() {
  const [brn, setBrn] = useState('')
  const [brnError, setBrnError] = useState<string | null>(null)

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    try {
      if (!validateBusinessRegistration(brn)) {
        setBrnError('유효하지 않은 사업자등록번호입니다.')
        return
      }
    } catch {
      setBrnError('10자리 숫자로 입력해 주세요.')
      return
    }
    submitTaxInvoice(brn)
  }

  return (
    <form onSubmit={handleSubmit}>
      {/* CORRECT: L1 primitive validates checksum on blur automatically */}
      <BusinessRegistrationInput
        value={brn}
        onChange={setBrn}
        errorMessage={brnError ?? undefined}
      />
      <button type="submit">세금계산서 발행</button>
    </form>
  )
}
```

### Public fixture data (verified via 국세청 + data.go.kr)

The following business registration numbers have been verified against the NTS algorithm. See `practices/evals/fixtures/business-registration-checksum/pass/` for fixture files.

| 사업자등록번호 | 검증 결과 | 출처 |
|---|---|---|
| 124-81-00998 | VALID | Samsung Electronics Co., Ltd. (공시 자료) |
| 120-81-47521 | VALID | Kakao Corp. (공시 자료) |
| 220-81-62517 | VALID | NAVER Corp. (공시 자료) |
| 107-86-14075 | VALID | LG Electronics Inc. (공시 자료) |
| 120-81-20653 | VALID | Hyundai Motor Company (공시 자료) |

All numbers are publicly registered companies with filings in the Korean business registry.
Algorithm reference: https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2227&cntntsId=7870

## Failing fixture

See: `practices/evals/fixtures/business-registration-checksum/fail_invalid_checksum/`
— Same public BRNs with the last digit mutated; `validateBusinessRegistration()` returns `false`.

See: `practices/evals/fixtures/business-registration-checksum/fail_format_violation/`
— Non-digit input and wrong-length inputs; `validateBusinessRegistration()` throws `FormatViolationError`.

Reference: [국세청 사업자등록번호 검증 알고리즘](https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2227&cntntsId=7870)
