import type { HttpExchange, HttpRequest, HttpResponse } from '@/lib/api/rawFetch';

/**
 * Pretty-printers that turn a captured {@link HttpExchange} into the exact text
 * the catalog CodeSnippet renders. Kept as a pure lib helper (no UI), so every
 * screen formats the live request/response identically — the persona's signature
 * "show the actual HTTP" surface.
 */

const PRETTY_INDENT = 2;

function prettyBody(body: unknown): string | null {
  if (body === undefined || body === null) return null;
  if (typeof body === 'string') return body;
  try {
    return JSON.stringify(body, null, PRETTY_INDENT);
  } catch {
    return String(body);
  }
}

function headerLines(headers: Record<string, string>): string {
  return Object.entries(headers)
    .map(([k, v]) => `${k}: ${v}`)
    .join('\n');
}

/** Render the request side as an HTTP request message (request line + headers + body). */
export function formatRequest(request: HttpRequest): string {
  const lines = [`${request.method} ${request.path} HTTP/1.1`];
  const headers = headerLines(request.headers);
  if (headers) lines.push(headers);
  const body = prettyBody(request.body);
  if (body !== null) {
    lines.push('');
    lines.push(body);
  }
  return lines.join('\n');
}

/** Render the response side as an HTTP response message (status line + headers + body). */
export function formatResponse(response: HttpResponse): string {
  const lines = [`HTTP/1.1 ${response.status} ${response.statusText}`.trimEnd()];
  const headers = headerLines(response.headers);
  if (headers) lines.push(headers);
  const body = prettyBody(response.body);
  if (body !== null) {
    lines.push('');
    lines.push(body);
  }
  return lines.join('\n');
}

/** Both sides of a round trip, separated — useful when a single snippet shows it all. */
export function formatExchange(exchange: HttpExchange): string {
  return `${formatRequest(exchange.request)}\n\n${'─'.repeat(40)}\n\n${formatResponse(
    exchange.response,
  )}`;
}
