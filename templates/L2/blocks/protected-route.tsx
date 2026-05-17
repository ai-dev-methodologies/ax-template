/*
---
template_id: L2/blocks/protected-route
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js Middleware — authentication pattern"
    url: "https://nextjs.org/docs/app/building-your-application/routing/middleware"
  - source_type: internal
    rationale: "L2 auth block — guards children behind isAuthenticated prop; redirect logic stays in L4."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
import * as React from 'react'

export interface ProtectedRouteProps {
  /** True when the current session is authenticated */
  isAuthenticated: boolean
  /** Content to render when authenticated */
  children: React.ReactNode
  /** Fallback rendered when NOT authenticated (default: null → caller redirects) */
  fallback?: React.ReactNode
}

/**
 * ProtectedRoute wraps content that requires authentication.
 *
 * Per PRD §4.11: auth redirect logic lives in L4 middleware / server components.
 * This block only guards the render tree — it does NOT call router.push() itself.
 *
 * L4 usage:
 *   <ProtectedRoute isAuthenticated={!!session} fallback={<Redirect to="/login" />}>
 *     <DashboardContent />
 *   </ProtectedRoute>
 */
export default function ProtectedRoute({
  isAuthenticated,
  children,
  fallback = null,
}: ProtectedRouteProps) {
  if (!isAuthenticated) {
    return <>{fallback}</>
  }
  return <>{children}</>
}
