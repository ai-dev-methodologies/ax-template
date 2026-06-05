import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Edge middleware — server-side auth guard for the ledger routes.
 *
 * Mirrors the client guard in (ledger)/layout.tsx: if the shared accessToken
 * cookie (written by @ax/core on login) is absent, redirect to /login with a
 * `from` param so the user lands back where they started after signing in.
 *
 * Every pay surface is authenticated() (JWT Bearer): /api/payments/**,
 * /api/subscriptions/**, /api/exports/**. The admin plan + admin billing
 * surfaces additionally require ROLE_ADMIN on the backend (the demo is ADMIN).
 */
export function middleware(request: NextRequest) {
  const accessToken = request.cookies.get('accessToken')?.value;
  if (!accessToken) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('from', request.nextUrl.pathname);
    return NextResponse.redirect(loginUrl);
  }
  return NextResponse.next();
}

// Guard every ledger route. Exclude /login, Next internals, and the API proxy.
export const config = {
  matcher: ['/((?!login|api|_next/static|_next/image|favicon.ico).*)'],
};
