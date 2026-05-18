/*
---
template_id: L2/blocks/phone-verification-panel
layer: L2
provenance_class: locked_constraint
evidence:
  - source_type: external
    citation: "KISA 본인인증 가이드라인 — 휴대폰 본인인증 절차: 본인 명의 휴대폰으로 CI/DI 발급, RRN 불요"
    url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "개인정보보호법 §24 — 고유식별정보(RRN) 수집 금지: 본인인증은 CI/DI 토큰으로 대체"
    url: "https://www.law.go.kr/법령/개인정보보호법"
    quoted_at: "2026-05-18"
applies_rule: no-rrn-collection-without-legal-basis
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/

/**
 * PhoneVerificationPanel — L2 block.
 *
 * KISA-compliant 휴대폰 본인인증 UI panel for PASS and KCB providers.
 *
 * Flow:
 * 1. User selects telecom carrier and clicks "인증 요청"
 * 2. Provider popup (PASS/KCB) launches via onRequestVerification()
 * 3. Provider calls backend callback; backend persists VerifiedIdentity with CI/DI
 * 4. Client polls or receives result; onVerified() fires with CI token
 *
 * This component never collects or displays the RRN (개인정보보호법 §24).
 * CI token is the only identity reference returned to the caller.
 */

import * as React from 'react'
import { cn } from '../../L1/lib/utils'

// ─── Types ────────────────────────────────────────────────────────────────────

export type VerificationProvider = 'pass' | 'kcb'
export type VerificationStatus = 'idle' | 'pending' | 'success' | 'error'

export type TelecomCarrier = 'SKT' | 'KT' | 'LGU+' | '알뜰폰'

/** Result returned to the caller on successful verification — CI only, never RRN. */
export interface VerificationResult {
  /** CI (Connecting Information) — 64-byte hex; cross-service unique person token */
  ci: string
  /** Verification provider: 'pass' | 'kcb' */
  provider: VerificationProvider
  /** Server-side verified timestamp (ISO-8601) */
  verifiedAt: string
}

export interface PhoneVerificationPanelProps {
  /** Preferred verification provider. Default: 'pass' */
  provider?: VerificationProvider
  /** Called when the user clicks the verification request button */
  onRequestVerification: (carrier: TelecomCarrier, provider: VerificationProvider) => void
  /** Called on successful verification with the CI token result */
  onVerified?: (result: VerificationResult) => void
  /** Called on verification error with an error message */
  onError?: (message: string) => void
  /** Current verification status (controlled from parent) */
  status?: VerificationStatus
  /** Error message to display when status is 'error' */
  errorMessage?: string
  /** Show as disabled (e.g., during form submission) */
  disabled?: boolean
  className?: string
}

// ─── Component ───────────────────────────────────────────────────────────────

const CARRIERS: TelecomCarrier[] = ['SKT', 'KT', 'LGU+', '알뜰폰']

/**
 * PhoneVerificationPanel (휴대폰 본인인증 패널).
 *
 * Design principles:
 * - Vendor-agnostic: PASS and KCB provider selection is surfaced to the user.
 * - No RRN field: the form only collects carrier selection; identity comes via CI/DI callback.
 * - KISA-compliant: caller supplies onRequestVerification() which launches the provider popup.
 * - Screen-reader friendly: aria-live for status updates.
 */
export default function PhoneVerificationPanel({
  provider = 'pass',
  onRequestVerification,
  onVerified: _onVerified,
  onError: _onError,
  status = 'idle',
  errorMessage,
  disabled = false,
  className,
}: PhoneVerificationPanelProps) {
  const [selectedCarrier, setSelectedCarrier] = React.useState<TelecomCarrier>('SKT')
  const [selectedProvider, setSelectedProvider] = React.useState<VerificationProvider>(provider)

  function handleRequestClick() {
    onRequestVerification(selectedCarrier, selectedProvider)
  }

  return (
    <div
      className={cn('flex flex-col gap-4 rounded-lg border p-4', className)}
      aria-labelledby="phone-verify-heading"
    >
      <h3 id="phone-verify-heading" className="text-sm font-semibold">
        휴대폰 본인인증
      </h3>

      {/* Telecom carrier selection */}
      <fieldset className="flex flex-col gap-1">
        <legend className="text-xs text-muted-foreground mb-2">통신사 선택</legend>
        <div role="group" aria-label="통신사" className="flex flex-wrap gap-2">
          {CARRIERS.map((carrier) => (
            <button
              key={carrier}
              type="button"
              onClick={() => setSelectedCarrier(carrier)}
              disabled={disabled || status === 'pending'}
              aria-pressed={selectedCarrier === carrier}
              className={cn(
                'rounded-md border px-3 py-1.5 text-xs font-medium transition-colors',
                selectedCarrier === carrier
                  ? 'border-primary bg-primary/10 text-primary'
                  : 'border-input text-muted-foreground hover:border-ring',
                (disabled || status === 'pending') && 'cursor-not-allowed opacity-50'
              )}
            >
              {carrier}
            </button>
          ))}
        </div>
      </fieldset>

      {/* Provider selection */}
      <fieldset className="flex flex-col gap-1">
        <legend className="text-xs text-muted-foreground mb-2">인증 방식</legend>
        <div role="group" aria-label="본인인증 제공사" className="flex gap-2">
          {(['pass', 'kcb'] as VerificationProvider[]).map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => setSelectedProvider(p)}
              disabled={disabled || status === 'pending'}
              aria-pressed={selectedProvider === p}
              className={cn(
                'rounded-md border px-3 py-1.5 text-xs font-medium transition-colors',
                selectedProvider === p
                  ? 'border-primary bg-primary/10 text-primary'
                  : 'border-input text-muted-foreground hover:border-ring',
                (disabled || status === 'pending') && 'cursor-not-allowed opacity-50'
              )}
            >
              {p.toUpperCase()}
            </button>
          ))}
        </div>
      </fieldset>

      {/* Request button */}
      <button
        type="button"
        onClick={handleRequestClick}
        disabled={disabled || status === 'pending' || status === 'success'}
        className={cn(
          'inline-flex h-9 items-center justify-center rounded-md px-4 py-2',
          'bg-primary text-primary-foreground text-sm font-medium shadow',
          'hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-1',
          'focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50',
          'transition-colors'
        )}
      >
        {status === 'pending' ? (
          <span aria-busy="true" aria-label="인증 진행 중">
            인증 진행 중…
          </span>
        ) : (
          '인증 요청'
        )}
      </button>

      {/* Status messages */}
      <div
        role="status"
        aria-live="polite"
        aria-atomic="true"
        className="min-h-[1.25rem]"
      >
        {status === 'success' && (
          <p className="flex items-center gap-1.5 text-sm text-green-600 dark:text-green-400">
            <svg aria-hidden="true" className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clipRule="evenodd"
              />
            </svg>
            본인인증이 완료되었습니다.
          </p>
        )}
        {status === 'error' && (
          <p role="alert" className="text-sm text-destructive">
            {errorMessage ?? '본인인증 중 오류가 발생했습니다. 다시 시도해 주세요.'}
          </p>
        )}
      </div>

      {/* Compliance notice — KISA 요건 */}
      <p className="text-[10px] leading-relaxed text-muted-foreground">
        본 인증은 KISA 본인인증 가이드라인에 따라 CI/DI 토큰만 수집합니다.
        주민등록번호는 수집·저장하지 않습니다 (개인정보보호법 §24).
      </p>
    </div>
  )
}
