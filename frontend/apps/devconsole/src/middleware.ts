import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Edge middleware — server-side auth guard for the console routes.
 *
 * Mirrors the client guard in (console)/layout.tsx: if the shared accessToken
 * cookie (written by @ax/core on login) is absent, redirect to /login with a
 * `from` param so the developer lands back where they started after signing in.
 * Every devconsole API surface is authenticated() (JWT Bearer); the webhook
 * admin surface additionally requires ROLE_ADMIN on the backend (the demo
 * account is ADMIN).
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

// Guard every console route. Exclude /login, Next internals, and the API proxy.
export const config = {
  matcher: ['/((?!login|api|_next/static|_next/image|favicon.ico).*)'],
};
