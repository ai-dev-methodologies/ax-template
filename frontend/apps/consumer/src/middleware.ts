import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Edge middleware — server-side auth guard for the app routes.
 *
 * Mirrors the client guard in (app)/layout.tsx: if the shared accessToken
 * cookie (written by @ax/core on login) is absent, redirect to /login with a
 * `from` param so the user lands back where they started after signing in.
 * Unlike the operator console there is no role gate — any authenticated user
 * may use the consumer feed (the backend endpoints are all `authenticated()`).
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

// Guard every app route. Exclude /login, Next internals, and the API proxy.
export const config = {
  matcher: ['/((?!login|api|_next/static|_next/image|favicon.ico).*)'],
};
