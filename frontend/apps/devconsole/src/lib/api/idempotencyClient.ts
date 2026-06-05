/**
 * Idempotency client — /api/idempotency-demo/resources. Backend:
 * IdempotencyDemoController. Auth: authenticated() (JWT). The principal is the
 * dedup tenant. Domain client (NOT a UI primitive) — composes the console's
 * rawFetch and surfaces the `Idempotency-Replayed` response header so the
 * playground can prove first-seen (201, false) vs replayed (201, true) vs
 * payload-change (422). `tolerateError` so the 422 problem body RENDERS.
 *
 * Curl-verified (2026-06-05, demo@ax.dev):
 *   POST + Idempotency-Key, body B1 (first)   -> 201, Idempotency-Replayed: false
 *   POST + same key,        body B1 (replay)  -> 201, Idempotency-Replayed: true
 *   POST + same key,        body B2 (changed) -> 422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD
 */
import { rawFetch, type HttpExchange } from './rawFetch';

export const idempotencyClient = {
  /** POST a resource with the given Idempotency-Key. Errors are tolerated so the
   *  422 payload-mismatch problem body is captured + rendered, not thrown. */
  create: (key: string, body: unknown): Promise<HttpExchange<unknown>> =>
    rawFetch<unknown>('/idempotency-demo/resources', {
      method: 'POST',
      headers: { 'Idempotency-Key': key },
      body,
      tolerateError: true,
    }),
};
