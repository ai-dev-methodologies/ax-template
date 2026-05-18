/*
---
template_id: L3/pages/mfa-setup
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: external
    citation: "RFC 6238 — TOTP: Time-Based One-Time Password Algorithm"
    url: "https://datatracker.ietf.org/doc/html/rfc6238"
  - source_type: internal_design
    rationale: "Generic MFA setup skeleton. Accepts QR code and OTP input as ReactNode slots from L4 (L4 wires L1 otp-input from SP14). Exposes onConfirm callback. No TOTP logic — L4 generates QR URI and validates OTP."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
'use client'

import * as React from 'react'

/**
 * MfaSetupPage — generic MFA (TOTP) setup skeleton.
 *
 * Slot props:
 *   - qrCodeSlot  (required) QR code image node (L4 renders <img src={qrUri} /> or SVG)
 *   - otpSlot     (required) OTP input node (L4 passes L1 <OtpInput /> from SP14)
 *   - onConfirm   (optional) called when user clicks "Verify & Enable"
 *   - secretKey   (optional) shows a manual-entry fallback below the QR code
 *   - backHref    (optional) back link href
 *
 * L4 usage:
 *   import MfaSetupPage from 'templates/L3/pages/mfa-setup/page'
 *   import { OtpInput } from 'templates/L1/components/otp-input'
 *
 *   export default function MfaSetupRoute() {
 *     const [otp, setOtp] = React.useState('')
 *     const { qrUri, secret } = useTotpSetup()
 *
 *     async function handleConfirm() {
 *       await api.post('/auth/mfa/verify', { otp })
 *     }
 *     return (
 *       <MfaSetupPage
 *         qrCodeSlot={<img src={qrUri} alt="MFA QR code" width={180} height={180} />}
 *         otpSlot={<OtpInput value={otp} onChange={setOtp} />}
 *         secretKey={secret}
 *         onConfirm={handleConfirm}
 *         backHref="/settings/security"
 *       />
 *     )
 *   }
 */
export interface MfaSetupPageProps {
  /** QR code image/SVG node rendered by L4 */
  qrCodeSlot: React.ReactNode
  /** OTP input node — L4 passes L1 OtpInput with its own state */
  otpSlot: React.ReactNode
  /** Called when the user clicks "Verify & Enable" */
  onConfirm?: () => void | Promise<void>
  /** Optional manual-entry secret key (displayed below QR code) */
  secretKey?: string
  /** Optional back-link href */
  backHref?: string
}

export default function MfaSetupPage({
  qrCodeSlot,
  otpSlot,
  onConfirm,
  secretKey,
  backHref,
}: MfaSetupPageProps) {
  const [isPending, setIsPending] = React.useState(false)

  async function handleConfirm() {
    setIsPending(true)
    try {
      await onConfirm?.()
    } finally {
      setIsPending(false)
    }
  }

  return (
    <main className="flex min-h-svh items-center justify-center px-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="space-y-1 text-center">
          <h1 className="text-2xl font-semibold tracking-tight">
            Set up two-factor authentication
          </h1>
          <p className="text-sm text-muted-foreground">
            Scan the QR code with your authenticator app, then enter the code below.
          </p>
        </div>

        {/* QR code slot */}
        <div className="flex justify-center">
          <div
            className="rounded-lg border bg-white p-4"
            aria-label="QR code for authenticator app"
          >
            {qrCodeSlot}
          </div>
        </div>

        {/* Manual key fallback */}
        {secretKey && (
          <div className="rounded-md bg-muted px-4 py-3 space-y-1">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
              Manual entry key
            </p>
            <p className="text-sm font-mono break-all select-all">{secretKey}</p>
          </div>
        )}

        {/* OTP input slot */}
        <div className="space-y-1">
          <p className="text-sm font-medium text-center">
            Enter the 6-digit code from your app
          </p>
          <div className="flex justify-center">{otpSlot}</div>
        </div>

        <button
          type="button"
          onClick={handleConfirm}
          disabled={isPending}
          className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity disabled:pointer-events-none disabled:opacity-50"
        >
          {isPending ? 'Verifying…' : 'Verify & Enable'}
        </button>

        {backHref && (
          <p className="text-center text-sm text-muted-foreground">
            <a
              href={backHref}
              className="underline underline-offset-4 hover:text-foreground transition-colors"
            >
              Cancel
            </a>
          </p>
        )}
      </div>
    </main>
  )
}
