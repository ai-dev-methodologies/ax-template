import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Edge middleware — server-side auth guard for the studio routes.
 *
 * Mirrors the client guard in (studio)/layout.tsx: if the shared accessToken
 * cookie (written by @ax/core on login) is absent, redirect to /login with a
 * `from` param so the user lands back where they started after signing in.
 *
 * Every studio surface is authenticated() (JWT Bearer): /api/files/**,
 * /api/tags/**, /api/favorites/**, /api/activities/**. Files are owner-scoped by
 * the JWT on the backend; tag definition mutations additionally require
 * ROLE_ADMIN (the demo is ADMIN).
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

// Guard every studio route. Exclude /login, Next internals, and the API proxy.
export const config = {
  matcher: ['/((?!login|api|_next/static|_next/image|favicon.ico).*)'],
};
