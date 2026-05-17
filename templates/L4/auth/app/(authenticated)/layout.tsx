/*
---
template_id: L4/auth/app/(authenticated)/layout
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — authenticated route group layout; wraps children in L2 ProtectedRoute for client-side guard."
  - source_type: external
    citation: "Next.js Middleware — server-side auth guard pattern"
    url: "https://nextjs.org/docs/app/building-your-application/routing/middleware"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import React, { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import ProtectedRoute from '../../../../L2/blocks/protected-route'

/**
 * AuthenticatedLayout — defensive client-side guard for protected routes.
 *
 * Primary auth guard is middleware.ts (server-side cookie check).
 * This layout handles the edge case where the cookie passed middleware
 * but the client-side session store says the user is logged out after hydration.
 *
 * L4 fork instructions:
 *   1. Replace the `isAuthenticated` detection with your auth hook
 *      (e.g., useAuthStore, useSession from next-auth, etc.)
 *   2. The ProtectedRoute block handles the render gate; redirection lives here
 *   3. Add a loading skeleton for the `isLoading` interim state
 *
 * L2 ProtectedRoute contract:
 *   - isAuthenticated=true  → renders children
 *   - isAuthenticated=false → renders fallback (null here, redirect is triggered)
 *   - Redirect logic lives in this L4 component, NOT inside ProtectedRoute
 */
export default function AuthenticatedLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  // Replace this with your auth store selector
  // e.g., const { accessToken, isLoading } = useAuthStore()
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    // Fork: replace with your session hydration logic
    // Example with Zustand: const token = useAuthStore.getState().accessToken
    const token = typeof document !== 'undefined'
      ? document.cookie.split('; ').find(r => r.startsWith('accessToken='))?.split('=')[1] ?? null
      : null
    setAccessToken(token)
    setIsLoading(false)
  }, [])

  useEffect(() => {
    if (!isLoading && !accessToken) {
      router.replace('/login')
    }
  }, [accessToken, isLoading, router])

  if (isLoading) {
    // Replace with a proper skeleton/spinner from your design system
    return (
      <div className="flex min-h-svh items-center justify-center">
        <span
          aria-hidden="true"
          className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary"
        />
      </div>
    )
  }

  return (
    <ProtectedRoute
      isAuthenticated={!!accessToken}
      fallback={null}
    >
      {children}
    </ProtectedRoute>
  )
}
