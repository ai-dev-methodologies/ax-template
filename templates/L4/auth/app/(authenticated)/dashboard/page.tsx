/*
---
template_id: L4/auth/app/(authenticated)/dashboard/page
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: getAuthState
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — placeholder protected dashboard page; demonstrates session data display using getAuthState operation."
  - source_type: external
    citation: "Next.js 15 App Router — protected page pattern with server session"
    url: "https://nextjs.org/docs/app/building-your-application/authentication"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import React, { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? '/api'

interface AuthState {
  email: string
  roles: string[]
  verificationState: string
  providerLinks?: Array<{ provider: string }>
}

/**
 * DashboardPage — L4 auth vertical placeholder protected page.
 *
 * Demonstrates the pattern for:
 *   - fetching the authenticated user's state (GET /auth/me, operationId: getAuthState)
 *   - displaying session data
 *   - logout flow
 *
 * Fork instructions:
 *   1. Replace inline fetch with your auth hook (useAuthStore, useSession, etc.)
 *   2. Build your actual dashboard UI on top of this shell
 *   3. The (authenticated)/layout.tsx handles the auth guard — no need to duplicate here
 */
export default function DashboardPage() {
  const router = useRouter()
  const [authState, setAuthState] = useState<AuthState | null>(null)

  useEffect(() => {
    fetch(`${API_BASE}/auth/me`, { credentials: 'include' })
      .then(r => r.ok ? r.json() : null)
      .then(data => setAuthState(data))
      .catch(() => setAuthState(null))
  }, [])

  async function handleLogout() {
    await fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      credentials: 'include',
    }).catch(() => {})
    router.push('/login')
  }

  return (
    <div className="min-h-svh p-8">
      <div className="mx-auto max-w-2xl space-y-8">
        <header className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold">Dashboard</h1>
          <button
            type="button"
            onClick={handleLogout}
            className="inline-flex items-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium shadow-sm hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            Sign out
          </button>
        </header>

        {authState ? (
          <div className="rounded-lg border p-6 space-y-3">
            <h2 className="text-sm font-medium text-muted-foreground uppercase tracking-wide">
              Account
            </h2>
            <dl className="space-y-2 text-sm">
              <div className="flex gap-2">
                <dt className="font-medium w-36 shrink-0">Email</dt>
                <dd className="text-muted-foreground">{authState.email}</dd>
              </div>
              <div className="flex gap-2">
                <dt className="font-medium w-36 shrink-0">Roles</dt>
                <dd className="text-muted-foreground">{authState.roles?.join(', ')}</dd>
              </div>
              <div className="flex gap-2">
                <dt className="font-medium w-36 shrink-0">Email verified</dt>
                <dd className="text-muted-foreground">
                  {authState.verificationState === 'verified' ? 'Yes' : 'Pending'}
                </dd>
              </div>
              {authState.providerLinks && authState.providerLinks.length > 0 && (
                <div className="flex gap-2">
                  <dt className="font-medium w-36 shrink-0">Linked providers</dt>
                  <dd className="text-muted-foreground">
                    {authState.providerLinks.map(p => p.provider).join(', ')}
                  </dd>
                </div>
              )}
            </dl>
          </div>
        ) : (
          <div className="rounded-lg border p-6">
            <p className="text-sm text-muted-foreground">Loading profile…</p>
          </div>
        )}

        {/* Fork: replace this placeholder with your actual dashboard content */}
        <div className="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">
          Your dashboard content goes here.
        </div>
      </div>
    </div>
  )
}
