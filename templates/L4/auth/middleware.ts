/*
---
template_id: L4/auth/middleware
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — Next.js edge middleware guards protected routes by checking accessToken cookie."
  - source_type: external
    citation: "Next.js Middleware — auth guard pattern with cookies()"
    url: "https://nextjs.org/docs/app/building-your-application/routing/middleware"
  - source_type: external
    citation: "OWASP Session Management Cheat Sheet — cookie-based session token"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

/**
 * Middleware — edge auth guard for protected routes.
 *
 * Runs before page render on matched paths.
 * Checks for accessToken cookie; redirects to /login if absent.
 *
 * Fork instructions:
 *   1. Update `matcher` to include all your protected route patterns
 *   2. Change the cookie name to match your auth implementation
 *   3. Add token validation logic if needed (JWT expiry check, etc.)
 *   4. Use `response.cookies.set()` to refresh short-lived tokens inline
 *
 * Security note (OWASP Session Management):
 *   - The accessToken cookie should be HttpOnly, Secure, SameSite=Strict
 *   - Set these attributes in your Spring Boot auth endpoints
 */
export function middleware(request: NextRequest) {
  const accessToken = request.cookies.get('accessToken')?.value
  const { pathname } = request.nextUrl

  if (!accessToken) {
    const loginUrl = new URL('/login', request.url)
    loginUrl.searchParams.set('from', pathname)
    return NextResponse.redirect(loginUrl)
  }

  return NextResponse.next()
}

export const config = {
  // Protect all authenticated routes — update this list for your app
  matcher: [
    '/dashboard',
    '/dashboard/:path*',
    '/settings',
    '/settings/:path*',
  ],
}
