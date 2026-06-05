/**
 * payFetch — the pay app's thin authed-fetch that adds CUSTOM REQUEST HEADERS.
 *
 * This is a DOMAIN CLIENT concern (lib/api), NOT a UI primitive. It composes the
 * SHARED @ax/core authStore for the Bearer token and reuses @ax/core's RFC 9457
 * (problem+json) parsing shape, exactly like the catalog's own `apiFetch`. The
 * only reason it exists rather than calling `apiFetch` directly is that the
 * payment domain REQUIRES per-request headers `apiFetch` does not expose:
 *
 *   - `Idempotency-Key` on every mutation (create / void / refund). This is the
 *     whole point of a money-handling UI: a double-submit must REPLAY (200, same
 *     payment) instead of double-charging (a second 201). The backend rejects a
 *     mutation without the header with 400.
 *
 * It does NOT fork auth: the Bearer comes from useAuthStore — the same source
 * @ax/core's apiFetch reads. No session / store / refresh logic is duplicated.
 */
import { useAuthStore } from '@ax/core';
import type { ApiError, ProblemDetail } from '@ax/core';

const API_BASE = '/api';

function bearer(): Record<string, string> {
  const token = useAuthStore.getState().accessToken;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function toApiError(status: number, body: ProblemDetail | null, fallback: string): ApiError {
  const message =
    body?.detail || body?.title || (typeof body?.message === 'string' ? body.message : '') || fallback;
  const error = new Error(message) as ApiError;
  error.status = status;
  if (body?.code) error.code = body.code;
  if (body) error.problem = body;
  return error;
}

function buildUrl(path: string, query?: Record<string, string | number | undefined>): string {
  if (!query) return `${API_BASE}${path}`;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === '') continue;
    params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${API_BASE}${path}?${qs}` : `${API_BASE}${path}`;
}

export interface PayRequestOptions {
  method?: string;
  body?: unknown;
  query?: Record<string, string | number | undefined>;
  /** Extra request headers (e.g. Idempotency-Key). */
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

/** Perform an authed JSON request with optional custom headers; parse the body. */
export async function payFetch<T>(path: string, options: PayRequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, headers: extra, signal } = options;
  const headers: Record<string, string> = { ...bearer(), ...extra };
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(buildUrl(path, query), {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });

  if (!res.ok) {
    const problem = (await res.json().catch(() => null)) as ProblemDetail | null;
    throw toApiError(res.status, problem, `요청에 실패했습니다 (${res.status})`);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

/**
 * Generate a fresh idempotency key for a mutation. Prefer the platform UUID;
 * fall back to a timestamp+random token where crypto.randomUUID is unavailable.
 */
export function newIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `idem-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
