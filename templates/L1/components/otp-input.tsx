/*
---
template_id: L1/components/otp-input
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: input-otp-2026-05
    section: InputOTP
    quote: "One-time password input component for React. Accessible. Unstyled. Customizable."
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: input-otp
    quote: "Accessible one-time password component with copy paste functionality."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 1.3.5-identify-input-purpose
    quote: "The purpose of each input field collecting information about the user can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 1.3.5 — autocomplete='one-time-code' on the underlying input"
  - "WCAG 2.2 SC 2.1.1 — keyboard navigable; tab/arrow moves between slots"
  - "WCAG 2.2 SC 3.3.2 — label associated with the OTP group"
  - "Paste handling: pasting a 6-digit string fills all slots"
dependencies: ["input-otp@^1"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#otp-input"
---
*/
import * as React from 'react'
import { OTPInput, OTPInputContext } from 'input-otp'
import { Dot } from 'lucide-react'
import { cn } from '../lib/utils'

export interface OtpInputProps {
  /** Total number of OTP digits (default: 6) */
  maxLength?: number
  /** Controlled value */
  value?: string
  /** Called on every change */
  onChange?: (value: string) => void
  /** Called when all digits are filled */
  onComplete?: (value: string) => void
  /** Restricts input to digits only (default: true) */
  digitsOnly?: boolean
  /** Disables the input */
  disabled?: boolean
  /** Additional className */
  className?: string
}

/**
 * OtpInput — shadcn-compatible OTP input with 6-slot layout.
 *
 * Layout: [0][1][2] · [3][4][5] — two groups of 3 with a dot separator.
 * Paste handling is built into `input-otp`; pasting a 6-digit string fills all slots.
 * `autocomplete="one-time-code"` is set by input-otp for WCAG 1.3.5 compliance.
 */
export function OtpInput({
  maxLength = 6,
  value,
  onChange,
  onComplete,
  digitsOnly = true,
  disabled = false,
  className,
}: OtpInputProps) {
  const pattern = digitsOnly ? '^[0-9]+$' : undefined

  return (
    <OTPInput
      maxLength={maxLength}
      value={value}
      onChange={onChange}
      onComplete={onComplete}
      pattern={pattern}
      disabled={disabled}
      containerClassName={cn('flex items-center gap-[--space-2] has-[:disabled]:opacity-50', className)}
      render={({ slots }) => (
        <>
          <OtpGroup slots={slots.slice(0, Math.ceil(maxLength / 2))} />
          <OtpSeparator />
          <OtpGroup slots={slots.slice(Math.ceil(maxLength / 2))} />
        </>
      )}
    />
  )
}

// ─── Internal sub-components ───────────────────────────────────────────────

interface OtpSlotProps {
  char: string | null
  hasFakeCaret: boolean
  isActive: boolean
  placeholderChar?: string | null
}

function OtpSlot({ char, hasFakeCaret, isActive }: OtpSlotProps) {
  return (
    <div
      className={cn(
        'relative flex h-10 w-10 items-center justify-center',
        'rounded-[--radius-md] border border-[--color-border]',
        'text-[length:--text-sm] font-[number:--weight-medium]',
        'transition-all duration-[--duration-fast]',
        isActive && 'z-10 ring-2 ring-[--color-focus-ring] ring-offset-1'
      )}
    >
      {char !== null ? char : (
        <span className="text-[--color-text-placeholder] opacity-50">○</span>
      )}
      {hasFakeCaret && (
        <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
          <div className="h-4 w-px animate-caret-blink bg-[--color-text] duration-1000" />
        </div>
      )}
    </div>
  )
}

interface OtpGroupProps {
  slots: OtpSlotProps[]
}

function OtpGroup({ slots }: OtpGroupProps) {
  return (
    <div className="flex items-center">
      {slots.map((slot, idx) => (
        <OtpSlot key={idx} {...slot} />
      ))}
    </div>
  )
}

function OtpSeparator() {
  return (
    <div role="separator" aria-hidden="true">
      <Dot className="h-4 w-4 text-[--color-text-muted]" />
    </div>
  )
}

// Re-export context for advanced usage (e.g., custom renders)
export { OTPInputContext }
