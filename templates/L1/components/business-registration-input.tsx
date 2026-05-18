/*
---
template_id: L1/components/business-registration-input
layer: L1
provenance_class: locked_constraint
evidence:
  - source_type: external
    citation: "국세청 사업자등록번호 검증 알고리즘 — 10자리 사업자등록번호의 체크섬 알고리즘: 승수 [1,3,7,1,3,7,1,3,5], 9번째 자리 특수 처리 (floor(5×d9/10) + (5×d9)%10)"
    url: "https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2227&cntntsId=7870"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "개인정보보호법 §24 — 사업자등록번호는 법인 식별자로 고유식별정보에 해당하지 않으나, 잘못된 번호 수집은 오결제·세금계산서 오류를 유발함"
    url: "https://www.law.go.kr/법령/개인정보보호법"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "행정안전부 공공데이터포털 — 사업자등록번호 공개 데이터셋 (open-data.go.kr)"
    url: "https://www.data.go.kr/data/15081808/fileData.do"
    quoted_at: "2026-05-18"
a11y_criteria:
  - "aria-invalid='true' when checksum validation fails"
  - "aria-describedby links to error message"
  - "Formatted display: XXX-XX-XXXXX (hyphens auto-inserted)"
applies_rule: business-registration-checksum-required
dependencies: []
imports_from: []
imports_forbidden: [L2, L3, L4, app/, lib/auth/]
---
*/

/**
 * BusinessRegistrationInput — L1 primitive.
 *
 * Korean B2B onboarding / tax-invoice surfaces require 사업자등록번호 (Business Registration
 * Number) input with real-time checksum validation per the NTS (국세청) algorithm.
 *
 * The checksum algorithm uses multipliers [1,3,7,1,3,7,1,3,5] with a special split
 * for the 9th digit: floor(5×d9/10) + (5×d9)%10. See practices/upstream/nts-business-reg-2026-05.snapshot.md.
 *
 * Formatting: auto-inserts hyphens → XXX-XX-XXXXX on blur/value change.
 */

import * as React from 'react'
import { cn } from '../lib/utils'

// ─── Checksum algorithm ───────────────────────────────────────────────────────

/** Thrown when the input does not match the expected 10-digit format (letters, wrong length, etc.). */
export class FormatViolationError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'FormatViolationError'
  }
}

/**
 * Validate a Korean business registration number (사업자등록번호).
 *
 * Algorithm (NTS 국세청):
 * 1. Strip hyphens/spaces → exactly 10 digits required (FormatViolationError otherwise).
 * 2. Multiply digits 1-8 by weights [1,3,7,1,3,7,1,3]; for digit 9 add floor(5×d9/10).
 * 3. Add (5×d9) % 10 separately.
 * 4. check = (10 − (sum % 10)) % 10. Valid if check === digit 10.
 *
 * @returns true if valid checksum, false if wrong check digit (valid format but invalid number)
 * @throws FormatViolationError if the input is not exactly 10 digits
 */
export function validateBusinessRegistration(brn: string): boolean {
  const digits = brn.replace(/[-\s]/g, '').split('').map(Number)

  if (
    digits.length !== 10 ||
    brn.replace(/[-\s]/g, '').match(/[^0-9]/)
  ) {
    throw new FormatViolationError(
      `Business registration number must be exactly 10 digits (received "${brn}")`
    )
  }

  const weights = [1, 3, 7, 1, 3, 7, 1, 3, 5]
  let sum = 0
  for (let i = 0; i < 8; i++) {
    sum += digits[i] * weights[i]
  }
  // Special treatment for 9th digit (0-indexed: digits[8])
  sum += Math.floor((digits[8] * 5) / 10)
  sum += (digits[8] * 5) % 10

  const checkDigit = (10 - (sum % 10)) % 10
  return checkDigit === digits[9]
}

/** Format a raw 10-digit string as XXX-XX-XXXXX. */
export function formatBrn(raw: string): string {
  const digits = raw.replace(/\D/g, '').slice(0, 10)
  if (digits.length <= 3) return digits
  if (digits.length <= 5) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`
}

// ─── Component ───────────────────────────────────────────────────────────────

export interface BusinessRegistrationInputProps {
  /** Controlled raw value (digits only, no hyphens) */
  value?: string
  /** Called with the raw digit string when the user types */
  onChange?: (raw: string) => void
  /** Called with (raw, isValid) when the field loses focus */
  onBlur?: (raw: string, isValid: boolean) => void
  /** True while form submission is in progress */
  disabled?: boolean
  /** Override label text */
  label?: string
  /** Override error message */
  errorMessage?: string
  /** Custom CSS class for the wrapper div */
  className?: string
  /** Input id for htmlFor linkage */
  id?: string
}

/**
 * Korean business registration number (사업자등록번호) input.
 *
 * - Auto-formats to XXX-XX-XXXXX on change.
 * - Validates NTS checksum on blur; sets aria-invalid when invalid.
 * - Throws FormatViolationError on non-digit input (reported as field error).
 *
 * Applies rule: `business-registration-checksum-required` (practices-react/rules/).
 */
export default function BusinessRegistrationInput({
  value = '',
  onChange,
  onBlur,
  disabled = false,
  label = '사업자등록번호',
  errorMessage,
  className,
  id = 'brn-input',
}: BusinessRegistrationInputProps) {
  const [displayValue, setDisplayValue] = React.useState<string>(formatBrn(value))
  const [error, setError] = React.useState<string | null>(errorMessage ?? null)

  // Sync external value changes
  React.useEffect(() => {
    setDisplayValue(formatBrn(value))
  }, [value])

  // External errorMessage override
  React.useEffect(() => {
    if (errorMessage !== undefined) setError(errorMessage)
  }, [errorMessage])

  const errorId = `${id}-error`

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const raw = e.target.value.replace(/\D/g, '').slice(0, 10)
    setDisplayValue(formatBrn(raw))
    setError(null)
    onChange?.(raw)
  }

  function handleBlur() {
    const raw = displayValue.replace(/\D/g, '')
    let isValid = false
    try {
      isValid = validateBusinessRegistration(raw)
      if (!isValid) setError('유효하지 않은 사업자등록번호입니다.')
    } catch (err) {
      if (err instanceof FormatViolationError) {
        setError('10자리 숫자로 입력해 주세요.')
      }
    }
    onBlur?.(raw, isValid)
  }

  const hasError = error !== null && error !== ''

  return (
    <div className={cn('flex flex-col gap-1', className)}>
      <label
        htmlFor={id}
        className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
      >
        {label}
      </label>
      <input
        id={id}
        type="text"
        inputMode="numeric"
        autoComplete="off"
        value={displayValue}
        onChange={handleChange}
        onBlur={handleBlur}
        disabled={disabled}
        aria-invalid={hasError}
        aria-describedby={hasError ? errorId : undefined}
        placeholder="000-00-00000"
        maxLength={12} /* 10 digits + 2 hyphens */
        className={cn(
          'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1',
          'text-sm shadow-sm transition-colors',
          'placeholder:text-muted-foreground',
          'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring',
          'disabled:cursor-not-allowed disabled:opacity-50',
          hasError && 'border-destructive focus-visible:ring-destructive'
        )}
      />
      {hasError && (
        <p id={errorId} role="alert" className="text-xs text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}
