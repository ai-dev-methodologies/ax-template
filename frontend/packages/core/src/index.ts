// @ax/core — shared front-end infrastructure for every per-persona app.
//
// Auth store + token cookie, the auth API client, the TanStack QueryClient
// factory, and the authed-fetch base. Per-persona apps consume these instead of
// redefining their own session/store/fetch plumbing.

export { useAuthStore } from './auth/authStore';
export type { AuthState } from './auth/store';
export { refreshMutex, tryRefresh } from './auth/refresh-mutex';
export {
  setAccessTokenCookie,
  clearAccessTokenCookie,
  readAccessTokenCookie,
} from './auth/token-cookie';

export { authClient } from './api/authClient';
export type {
  SignupRequest,
  SignupResponse,
  LoginRequest,
  LoginResponse,
  UserProfile,
  VerifyEmailRequest,
  PasswordResetRequest,
  PasswordResetConfirm,
  PasswordChangeRequest,
} from './api/authClient';

export { apiFetch, apiDownload } from './api/authedFetch';
export type { ApiError, ProblemDetail, RequestOptions } from './api/authedFetch';

export { getQueryClient } from './query-client';
