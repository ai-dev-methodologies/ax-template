/**
 * Session-management client. Backend: SessionController (/api/sessions, own
 * sessions) + AdminSessionController (DELETE /api/admin/sessions/{id},
 * ROLE_ADMIN force-logout). Raw IP/User-Agent are masked server-side.
 * Domain client — composes the shared @ax/core authed fetch.
 */
import { apiFetch } from '@ax/core';

export type SessionStatus = 'ACTIVE' | 'REVOKED';

export interface SessionRecord {
  id: string;
  status: SessionStatus;
  jti: string;
  deviceLabel: string | null;
  ipAddressMasked: string | null;
  userAgentSummary: string | null;
  createdAt: string;
  lastSeenAt: string | null;
  expiresAt: string;
  revokedAt: string | null;
  revokedByUserId: string | null;
  expired: boolean;
}

export interface SessionListResponse {
  items: SessionRecord[];
  totalElements: number;
}

export const sessionClient = {
  list: (): Promise<SessionListResponse> => apiFetch<SessionListResponse>('/sessions'),

  /** ROLE_ADMIN force-logout. Returns 204 (no body). */
  forceRevoke: (id: string): Promise<void> =>
    apiFetch<void>(`/admin/sessions/${id}`, { method: 'DELETE' }),
};
