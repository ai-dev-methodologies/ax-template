/*
---
template_id: L4/feature-flags/middleware
layer: L4
domain: feature-flags
provenance_class: internal_design
spec_ref: "specs/feature-flags-frontend-l0.yaml#FF-FE-005"
backend_operation_id: isFeatureFlagActive
evidence:
  - source_type: external
    citation: "Next.js Docs — Middleware: matching paths and redirect"
    url: "https://nextjs.org/docs/app/building-your-application/routing/middleware"
usage: |
  Server-side feature flag evaluation in Next.js middleware.
  Configure FLAGGED_ROUTES below with your flag → route mapping.
  The backend URL must be accessible from the Next.js server process.
  Replace 'YOUR_API_BASE' with your backend URL (e.g. http://localhost:8080).
---
*/
import { NextRequest, NextResponse } from 'next/server'

/**
 * Map of { route_prefix → flag_name }.
 * The middleware checks each flagged route on every request.
 *
 * Example: '/new-checkout' is gated behind 'new-checkout' flag.
 * Add entries as new feature-flagged routes are introduced.
 */
const FLAGGED_ROUTES: Record<string, string> = {
  '/new-checkout': 'new-checkout',
  // Add more route → flag mappings here
}

const API_BASE = process.env.BACKEND_API_BASE ?? 'http://localhost:8080'

/**
 * Evaluate a feature flag server-side.
 * Fail-closed: returns false on network error or non-OK response.
 *
 * spec_ref: FF-FE-005, FF-EVAL-002 (fail-closed)
 */
async function isFlagActive(flagName: string): Promise<boolean> {
  try {
    const res = await fetch(
      `${API_BASE}/api/v1/feature-flags/${encodeURIComponent(flagName)}/active`,
      { cache: 'no-store' },
    )
    if (!res.ok) return false
    const data = (await res.json()) as { active: boolean }
    return data.active
  } catch {
    return false // fail-closed on network error
  }
}

/**
 * Next.js middleware — feature flag gating.
 *
 * For each flagged route, evaluates the backend flag at request time.
 * Redirects to /not-found if the flag is inactive.
 *
 * spec_ref: specs/feature-flags-frontend-l0.yaml#FF-FE-005
 * blueprint_ref: blueprints/feature-flags-ui-manifest.yaml#middleware
 */
export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl

  for (const [routePrefix, flagName] of Object.entries(FLAGGED_ROUTES)) {
    if (pathname.startsWith(routePrefix)) {
      const active = await isFlagActive(flagName)
      if (!active) {
        return NextResponse.redirect(new URL('/not-found', request.url))
      }
    }
  }

  return NextResponse.next()
}

export const config = {
  // Match only flagged route prefixes — avoid running on static assets
  matcher: Object.keys(FLAGGED_ROUTES).map((prefix) => `${prefix}/:path*`),
}
