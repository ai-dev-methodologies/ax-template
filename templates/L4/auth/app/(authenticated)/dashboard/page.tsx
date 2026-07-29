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
import DashboardView, { type AuthState } from './dashboard-view'

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? '/api'

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

  return <DashboardView authState={authState} onLogout={handleLogout} />
}
