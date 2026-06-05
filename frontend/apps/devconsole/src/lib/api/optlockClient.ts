/**
 * Optimistic-locking client — /api/optlock/resources. Backend: OptlockController.
 * Auth: authenticated() (JWT), owner-scoped. Strong ETags + If-Match precondition
 * on every mutation. Domain client (NOT a UI primitive) — composes the console's
 * rawFetch; the ETag arrives in the response header, so the demo reads it from the
 * captured exchange. `tolerateError` so the 428/412 problem bodies RENDER.
 *
 * Curl-verified (2026-06-05, demo@ax.dev):
 *   POST /api/optlock/resources {name,quantity}      -> 201 + ETag "optlock-resource-<id>-<ver>"
 *   GET  /api/optlock/resources/{id}                 -> 200 + ETag
 *   PUT  (no If-Match)                               -> 428 PRECONDITION_REQUIRED
 *   PUT  (correct If-Match)                          -> 200 + new ETag
 *   PUT  (stale If-Match)                            -> 412 PRECONDITION_FAILED (+ current_etag)
 */
import { rawFetch, type HttpExchange } from './rawFetch';

export interface OptlockResource {
  id: string;
  name: string;
  quantity: number;
  version: number;
}

export interface OptlockUpdateInput {
  name: string;
  quantity: number;
}

/** Read the strong ETag from a captured exchange's response headers. */
export function readEtag(exchange: HttpExchange<unknown>): string | undefined {
  const headers = exchange.response.headers;
  return headers['etag'] ?? headers['ETag'] ?? undefined;
}

export const optlockClient = {
  create: (
    body: OptlockUpdateInput,
  ): Promise<HttpExchange<OptlockResource>> =>
    rawFetch<OptlockResource>('/optlock/resources', { method: 'POST', body }),

  get: (id: string): Promise<HttpExchange<OptlockResource>> =>
    rawFetch<OptlockResource>(`/optlock/resources/${id}`),

  /** PUT with an optional If-Match. Omit `ifMatch` to demonstrate the 428 path.
   *  Errors tolerated so 428/412 problem bodies are captured + rendered. */
  update: (
    id: string,
    body: OptlockUpdateInput,
    ifMatch?: string,
  ): Promise<HttpExchange<OptlockResource>> =>
    rawFetch<OptlockResource>(`/optlock/resources/${id}`, {
      method: 'PUT',
      headers: ifMatch ? { 'If-Match': ifMatch } : {},
      body,
      tolerateError: true,
    }),
};
