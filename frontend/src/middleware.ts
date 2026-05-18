import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Edge middleware — auth guard for protected routes.
 * Runs before page render. Checks for accessToken cookie.
 * If missing, redirects to /login.
 *
 * NOTE: Backend OAuth redirect URIs need updating from :5173 → :3000
 * in backend/src/main/resources/application.yml (NOT modified in SP1 scope).
 * Provider consoles (Google/Naver/Kakao) also need the callback URL updated.
 */
export function middleware(request: NextRequest) {
  const accessToken = request.cookies.get('accessToken')?.value;
  const { pathname } = request.nextUrl;

  // Protected paths — redirect to /login if no token cookie
  if (!accessToken) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('from', pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/dashboard', '/dashboard/:path*'],
};
