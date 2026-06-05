/**
 * Client-readable access-token cookie helpers.
 *
 * The Zustand auth store is in-memory only, so a page reload would lose the
 * session AND desync from `src/middleware.ts`, which guards protected routes by
 * reading an `accessToken` cookie. These helpers keep the cookie in lockstep
 * with the store: set on login/signup/OAuth success, clear on logout, and read
 * on app start to rehydrate.
 *
 * SameSite=Lax + path=/ is correct for a first-party SPA: it survives reload and
 * top-level navigation (e.g. the OAuth redirect back from the provider) while
 * still blocking cross-site request forgery on unsafe methods. The cookie is
 * intentionally non-HttpOnly because the SPA must read it to rehydrate; the
 * refresh token stays server-side/HttpOnly (handled by the backend).
 */

const COOKIE_NAME = 'accessToken';

/** SSR-safe: cookies only exist in the browser. */
function hasDocument(): boolean {
  return typeof window !== 'undefined' && typeof document !== 'undefined';
}

export function setAccessTokenCookie(token: string): void {
  if (!hasDocument()) return;
  document.cookie = `${COOKIE_NAME}=${encodeURIComponent(token)}; path=/; SameSite=Lax`;
}

export function clearAccessTokenCookie(): void {
  if (!hasDocument()) return;
  document.cookie = `${COOKIE_NAME}=; path=/; SameSite=Lax; expires=Thu, 01 Jan 1970 00:00:00 GMT`;
}

export function readAccessTokenCookie(): string | null {
  if (!hasDocument()) return null;
  const match = document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${COOKIE_NAME}=`));
  if (!match) return null;
  const value = match.slice(COOKIE_NAME.length + 1);
  return value ? decodeURIComponent(value) : null;
}
