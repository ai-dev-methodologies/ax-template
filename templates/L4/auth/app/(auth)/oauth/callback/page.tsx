/*
---
template_id: L4/auth/app/(auth)/oauth/callback/page
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: oauthCallback
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — OAuth callback page composing L2/blocks/oauth-callback-panel with L3 auth-callback-page for full-page wrapping."
  - source_type: external
    citation: "RFC 6749 §4.1.2 — Authorization Code Response redirect handling"
    url: "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import React, { Suspense } from 'react'
import OAuthCallbackPanel from '../../../../../../L2/blocks/oauth-callback-panel'
import AuthCallbackPage from '../../../../../../L3/pages/auth-callback-page/page'

/**
 * OAuthCallbackPage — L4 auth vertical OAuth callback route.
 *
 * Called by the OAuth provider after authorization. Reads ?code + ?provider
 * from searchParams, exchanges the code via the backend, then renders:
 *   - loading state (L2 OAuthCallbackPanel) while exchange is in-flight
 *   - success/failure state (L3 AuthCallbackPage full-page result)
 *
 * Backend operation: GET /auth/oauth/{provider}/callback
 *   (operationId: oauthCallback)
 *
 * Fork instructions:
 *   1. Replace OAuthExchangeClient fetch with your auth store / tRPC mutation
 *   2. Read the provider from searchParams or the route path (/oauth/google/callback)
 *   3. Adjust successHref / failureHref for your routing
 *   4. Ensure the backend CORS + redirect URI is configured for this page's URL
 */

interface OAuthExchangeClientProps {
  provider: string
  code: string | undefined
  error: string | undefined
}

/**
 * OAuthExchangeClient — resolves OAuth code exchange and picks the result state.
 * This is an async server component; convert to 'use client' + useEffect for CSR.
 */
async function OAuthExchangeClient({
  provider,
  code,
  error,
}: OAuthExchangeClientProps) {
  // Immediate error from provider (user denied, etc.)
  if (error || !code) {
    return (
      <AuthCallbackPage
        provider={provider}
        status="failure"
        successHref="/dashboard"
        failureHref="/login"
        statusSlot={
          <OAuthCallbackPanel
            status="error"
            errorMessage={error ?? 'Authorization was cancelled.'}
            onRetry={undefined}
          />
        }
      />
    )
  }

  // Exchange code for token via backend
  const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? '/api'
  let callbackStatus: 'success' | 'failure' = 'failure'

  try {
    const res = await fetch(
      `${API_BASE}/auth/oauth/${encodeURIComponent(provider)}/callback?code=${encodeURIComponent(code)}`,
      { method: 'GET', credentials: 'include' }
    )
    callbackStatus = res.ok ? 'success' : 'failure'
  } catch {
    callbackStatus = 'failure'
  }

  return (
    <AuthCallbackPage
      provider={provider}
      status={callbackStatus}
      successHref="/dashboard"
      failureHref="/login"
    />
  )
}

interface OAuthCallbackPageProps {
  searchParams: Promise<{
    provider?: string
    code?: string
    error?: string
    state?: string
  }>
}

export default async function OAuthCallbackPage({ searchParams }: OAuthCallbackPageProps) {
  const params = await searchParams
  const provider = params.provider ?? 'OAuth'
  const code = params.code
  const error = params.error

  return (
    <Suspense
      fallback={
        <OAuthCallbackPanel
          status="loading"
          redirectMessage={`Completing ${provider} sign-in…`}
        />
      }
    >
      <OAuthExchangeClient provider={provider} code={code} error={error} />
    </Suspense>
  )
}
