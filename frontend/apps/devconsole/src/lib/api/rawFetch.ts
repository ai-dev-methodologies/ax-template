/**
 * rawFetch — the developer console's HTTP layer.
 *
 * This is a DOMAIN CLIENT concern (lib/api), NOT a UI primitive: it composes the
 * SHARED @ax/core authStore for the Bearer token, exactly like the catalog's own
 * `apiFetch`. The reason the console needs its own thin wrapper rather than
 * reusing `apiFetch` directly is the persona's signature requirement — every
 * screen must render the ACTUAL HTTP request line + headers and the ACTUAL
 * response status + headers + body in a CodeSnippet. `apiFetch` (correctly, for
 * its job) hides request/response headers and only returns the parsed JSON body;
 * ETags, If-Match, Idempotency-Key, Retry-After, trace_id headers, and the raw
 * status are precisely what this console exists to surface. So rawFetch captures
 * the full exchange while keeping the token + RFC 9457 parsing identical.
 *
 * It does NOT re-implement auth: the Bearer comes from useAuthStore, the same
 * source @ax/core's apiFetch reads. No session/store/refresh logic is forked.
 */
import { useAuthStore } from '@ax/core';
import type { ProblemDetail } from '@ax/core';

const API_BASE = '/api';

/** A captured HTTP request side, ready to pretty-print into a CodeSnippet. */
export interface HttpRequest {
  method: string;
  /** Path relative to /api, e.g. "/api-keys". */
  path: string;
  headers: Record<string, string>;
  /** Parsed JSON body if any (rendered pretty); undefined for bodiless requests. */
  body?: unknown;
}

/** A captured HTTP response side. */
export interface HttpResponse {
  status: number;
  statusText: string;
  headers: Record<string, string>;
  /** Parsed JSON when the body is JSON; the raw text otherwise; null on empty. */
  body: unknown;
}

/** The full round trip — what a screen renders as request + response snippets. */
export interface HttpExchange<T = unknown> {
  request: HttpRequest;
  response: HttpResponse;
  /** Convenience: the parsed response body typed by the caller. */
  data: T;
}

/** An Error enriched with the captured exchange so a screen can still show it. */
export interface ExchangeError extends Error {
  status: number;
  code?: string;
  problem?: ProblemDetail;
  exchange: HttpExchange;
}

export interface RawRequestOptions {
  method?: string;
  body?: unknown;
  /** Extra request headers (Idempotency-Key, If-Match, Accept-Language, …). */
  headers?: Record<string, string>;
  /** When true, a non-2xx status resolves instead of throwing (the console wants
   *  to RENDER 4xx/5xx problem bodies, not just catch them). */
  tolerateError?: boolean;
}

function bearer(): Record<string, string> {
  const token = useAuthStore.getState().accessToken;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/** Collapse a Headers iterable into a plain object, redacting the Bearer value. */
function headersToObject(headers: Headers, redactAuth: boolean): Record<string, string> {
  const out: Record<string, string> = {};
  headers.forEach((value, key) => {
    out[key] = redactAuth && key.toLowerCase() === 'authorization' ? 'Bearer ••••••' : value;
  });
  return out;
}

function isJson(contentType: string | null): boolean {
  return Boolean(contentType && /\bjson\b/i.test(contentType));
}

function toExchangeError(exchange: HttpExchange): ExchangeError {
  const { response } = exchange;
  const problem =
    response.body && typeof response.body === 'object'
      ? (response.body as ProblemDetail)
      : undefined;
  const message =
    problem?.detail ||
    problem?.title ||
    `요청에 실패했습니다 (${response.status})`;
  const error = new Error(message) as ExchangeError;
  error.status = response.status;
  if (problem?.code && typeof problem.code === 'string') error.code = problem.code;
  if (problem) error.problem = problem;
  error.exchange = exchange;
  return error;
}

/**
 * Perform an authed request and return the FULL captured exchange. On a non-2xx
 * status the promise rejects with an {@link ExchangeError} that still carries the
 * exchange (so a screen can render the problem body); pass `tolerateError` to get
 * the exchange back as a resolved value instead.
 */
export async function rawFetch<T = unknown>(
  path: string,
  options: RawRequestOptions = {},
): Promise<HttpExchange<T>> {
  const { method = 'GET', body, headers: extraHeaders = {}, tolerateError = false } = options;

  const requestHeaders: Record<string, string> = { ...bearer(), ...extraHeaders };
  if (body !== undefined && requestHeaders['Content-Type'] === undefined) {
    requestHeaders['Content-Type'] = 'application/json';
  }

  const url = `${API_BASE}${path}`;
  const res = await fetch(url, {
    method,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const contentType = res.headers.get('Content-Type');
  let responseBody: unknown = null;
  const text = await res.text();
  if (text.length > 0) {
    responseBody = isJson(contentType) ? safeParse(text) : text;
  }

  const exchange: HttpExchange<T> = {
    request: {
      method,
      path: url,
      // Redact the Bearer in the rendered request (never print a live token).
      headers: redactAuthValue(requestHeaders),
      body: body,
    },
    response: {
      status: res.status,
      statusText: res.statusText,
      headers: headersToObject(res.headers, false),
      body: responseBody,
    },
    data: responseBody as T,
  };

  if (!res.ok && !tolerateError) {
    throw toExchangeError(exchange);
  }
  return exchange;
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function redactAuthValue(headers: Record<string, string>): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [key, value] of Object.entries(headers)) {
    out[key] = key.toLowerCase() === 'authorization' ? 'Bearer ••••••' : value;
  }
  return out;
}
