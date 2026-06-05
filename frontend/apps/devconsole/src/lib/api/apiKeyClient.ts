/**
 * API-key client — /api/api-keys. Backend: ApiKeyController.
 * Auth: every endpoint is JWT-only (authenticated()). The plaintext `value` is
 * returned ONCE on create + rotate, NEVER on list/get (KEY-AUTHN-001). The demo
 * account is ADMIN but these are per-principal keys (not admin-gated).
 * Domain client (NOT a UI primitive) — composes the console's rawFetch so each
 * call returns the captured HTTP exchange for the CodeSnippet surface.
 *
 * Curl-verified (2026-06-05, demo@ax.dev):
 *   POST   /api/api-keys {name,scopes[],expiresInDays} -> 201 CreateApiKeyResponse (incl. value)
 *   GET    /api/api-keys                               -> 200 { items[], totalElements }
 *   POST   /api/api-keys/{id}/rotate                   -> 201 CreateApiKeyResponse (new value)
 *   DELETE /api/api-keys/{id}                          -> 204
 */
import { rawFetch, type HttpExchange } from './rawFetch';

export type ApiKeyScope = 'READ' | 'WRITE';
export type ApiKeyStatus = 'ACTIVE' | 'REVOKED';

/** Read-side row (list/get) — NO plaintext value. */
export interface ApiKey {
  id: string;
  prefix: string;
  name: string;
  scopes: ApiKeyScope[];
  status: ApiKeyStatus;
  createdAt: string;
  expiresAt: string | null;
  lastUsedAt: string | null;
}

/** Create/rotate response — carries the one-time plaintext `value`. */
export interface ApiKeySecret {
  id: string;
  value: string;
  prefix: string;
  name: string;
  scopes: ApiKeyScope[];
  status: ApiKeyStatus;
  createdAt: string;
  expiresAt: string | null;
}

export interface ApiKeyList {
  items: ApiKey[];
  totalElements: number;
}

export interface CreateApiKeyInput {
  name: string;
  scopes: ApiKeyScope[];
  expiresInDays?: number;
}

export const apiKeyClient = {
  list: (): Promise<HttpExchange<ApiKeyList>> => rawFetch<ApiKeyList>('/api-keys'),

  create: (input: CreateApiKeyInput): Promise<HttpExchange<ApiKeySecret>> =>
    rawFetch<ApiKeySecret>('/api-keys', { method: 'POST', body: input }),

  rotate: (id: string): Promise<HttpExchange<ApiKeySecret>> =>
    rawFetch<ApiKeySecret>(`/api-keys/${id}/rotate`, { method: 'POST' }),

  revoke: (id: string): Promise<HttpExchange<unknown>> =>
    rawFetch<unknown>(`/api-keys/${id}`, { method: 'DELETE' }),
};
