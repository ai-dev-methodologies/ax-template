/**
 * Shared HTTP helper for the enterprise operations console clients.
 *
 * Every call carries the in-memory JWT from {@link useAuthStore} as a Bearer
 * token and surfaces RFC 9457 (problem+json) `detail`/`code`/`title` fields as a
 * readable Error so screens can render a real message instead of a raw status.
 */
import { useAuthStore } from '@/lib/auth/authStore';

const API_BASE = '/api';

/** An Error enriched with the HTTP status and the parsed problem body. */
export interface ApiError extends Error {
  status: number;
  code?: string;
  problem?: ProblemDetail;
}

/** RFC 9457 problem+json shape (Spring `ProblemDetail`). */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  [key: string]: unknown;
}

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

interface RequestOptions {
  method?: string;
  body?: unknown;
  /** Extra query params; undefined/empty values are dropped. */
  query?: Record<string, string | number | undefined>;
  signal?: AbortSignal;
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  if (!query) return `${API_BASE}${path}`;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === '') continue;
    params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${API_BASE}${path}?${qs}` : `${API_BASE}${path}`;
}

/** Perform an authed JSON request and parse the response (or problem) body. */
export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, signal } = options;
  const headers: Record<string, string> = { ...bearer() };
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

/** Authed binary download (used by report-export). Returns a Blob + filename. */
export async function apiDownload(path: string): Promise<{ blob: Blob; filename: string }> {
  const res = await fetch(buildUrl(path), { headers: { ...bearer() } });
  if (!res.ok) {
    const problem = (await res.json().catch(() => null)) as ProblemDetail | null;
    throw toApiError(res.status, problem, `다운로드에 실패했습니다 (${res.status})`);
  }
  const disposition = res.headers.get('Content-Disposition') ?? '';
  const match = /filename\*?=(?:UTF-8'')?["']?([^"';]+)/i.exec(disposition);
  const filename = match ? decodeURIComponent(match[1]) : 'export';
  const blob = await res.blob();
  return { blob, filename };
}
