/*
---
template_id: L4/search/middleware.ts
layer: L4
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js Middleware — Runs on every matched request before the page renders. Use to enforce authentication on /search routes that require login."
    url: "https://nextjs.org/docs/app/building-your-application/routing/middleware"
imports_from: [L1, L2, L3]
imports_forbidden: []
---
*/
import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

/**
 * Search domain middleware.
 *
 * Guards:
 *   - /search/results: Redirects to /login if unauthenticated (when auth is enabled).
 *   - /search:         Public — no auth required for the palette landing.
 *
 * Fork instructions:
 *   1. Merge this matcher config into your app's root middleware.ts.
 *   2. Replace the auth check with your session/JWT validation logic.
 *   3. Remove the auth guard if search is public in your app.
 */
export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Guard: /search/results requires authentication
  if (pathname.startsWith('/search/results')) {
    // Replace with your actual auth check:
    //   const session = await getSession(request)
    //   if (!session) return NextResponse.redirect(new URL('/login', request.url))
    const authCookie = request.cookies.get('ax-session')
    if (!authCookie) {
      const loginUrl = new URL('/login', request.url)
      loginUrl.searchParams.set('callbackUrl', request.url)
      return NextResponse.redirect(loginUrl)
    }
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/search/:path*'],
}
