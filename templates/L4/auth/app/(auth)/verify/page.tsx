/*
---
template_id: L4/auth/app/(auth)/verify/page
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: emailVerify
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — email verification page composing L2/blocks/email-verify-panel and L3 auth-callback-page skeleton."
  - source_type: external
    citation: "OWASP Authentication Cheat Sheet — email verification token handling"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import React, { Suspense } from 'react'
import EmailVerifyPanel from '../../../../../L2/blocks/email-verify-panel'
import AuthCallbackPage from '../../../../../L3/pages/auth-callback-page/page'

/**
 * VerifyPage — L4 auth vertical email verification route.
 *
 * Two modes determined by ?token query param:
 *   - No token:  show "check your email" panel (L2 EmailVerifyPanel in pending state)
 *   - Has token: attempt verification; render L3 AuthCallbackPage with result status
 *
 * Backend operation: GET /auth/email/verify-email?token=<token>
 *   (operationId: emailVerify)
 *
 * Fork instructions:
 *   1. Replace VerifyClient with your auth hook / tRPC mutation for verification
 *   2. Update successHref / failureHref to match your routing
 *   3. Add error code mapping for specific failure messages if needed
 */

interface VerifyClientProps {
  token: string | undefined
}

/**
 * VerifyClient — async server component that resolves the token and delegates
 * rendering to the appropriate L2/L3 block.
 *
 * In a client-side-only setup, convert to a 'use client' component with
 * useSearchParams() + useEffect for the fetch call.
 */
async function VerifyClient({ token }: VerifyClientProps) {
  if (!token) {
    // No token: show pending state — user should check their email
    return (
      <div className="w-full max-w-sm space-y-4 text-center">
        <h1 className="text-xl font-semibold">Verify your email</h1>
        <EmailVerifyPanel
          status="pending"
          onResend={undefined}
        />
      </div>
    )
  }

  // Has token: call backend to verify
  const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? '/api'
  let callbackStatus: 'success' | 'failure' = 'failure'

  try {
    const res = await fetch(
      `${API_BASE}/auth/email/verify-email?token=${encodeURIComponent(token)}`,
      { method: 'GET', credentials: 'include' }
    )
    callbackStatus = res.ok ? 'success' : 'failure'
  } catch {
    callbackStatus = 'failure'
  }

  // Delegate full-page rendering to L3 AuthCallbackPage
  return (
    <AuthCallbackPage
      provider="Email"
      status={callbackStatus}
      successHref="/dashboard"
      failureHref="/login"
      successLabel="Go to dashboard"
      failureLabel="Back to sign in"
    />
  )
}

interface VerifyPageProps {
  searchParams: Promise<{ token?: string }>
}

export default async function VerifyPage({ searchParams }: VerifyPageProps) {
  const { token } = await searchParams
  return (
    <Suspense fallback={<p className="text-sm text-muted-foreground">Verifying…</p>}>
      <VerifyClient token={token} />
    </Suspense>
  )
}
