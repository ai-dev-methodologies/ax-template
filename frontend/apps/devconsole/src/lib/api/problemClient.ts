/**
 * Problem-details client — /api/problem-demo/*. Backend: ProblemDemoController.
 * Auth: authenticated() (JWT). Each endpoint deliberately triggers one RFC 9457
 * problem+json shape. Domain client (NOT a UI primitive) — composes the console's
 * rawFetch with `tolerateError` so the 4xx/5xx problem body is captured + rendered
 * (the whole point of this explorer).
 *
 * Curl-verified (2026-06-05, demo@ax.dev):
 *   POST /api/problem-demo/insufficient-funds -> 402 (type, code, trace_id, balance, accounts)
 *   POST /api/problem-demo/validate {bad}     -> 400 (errors[] with RFC 6901 pointers)
 *   POST /api/problem-demo/boom               -> 500 (no stack in detail, has trace_id)
 */
import { rawFetch, type HttpExchange } from './rawFetch';

export type ProblemKind = 'insufficient-funds' | 'validate' | 'boom';

/** A deliberately-malformed transfer body for the `validate` path (400 errors[]). */
const VALIDATE_BAD_BODY = { fromAccount: '', amount: -5 } as const;

export const problemClient = {
  trigger: (kind: ProblemKind): Promise<HttpExchange<unknown>> => {
    if (kind === 'validate') {
      return rawFetch<unknown>('/problem-demo/validate', {
        method: 'POST',
        headers: { 'Accept-Language': 'ko' },
        body: VALIDATE_BAD_BODY,
        tolerateError: true,
      });
    }
    return rawFetch<unknown>(`/problem-demo/${kind}`, {
      method: 'POST',
      headers: { 'Accept-Language': 'ko' },
      tolerateError: true,
    });
  },
};
