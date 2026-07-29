/**
 * Profile client — GET /api/auth/me.
 *
 * The live backend returns a SINGLE `role` string ({userId, email, role,
 * emailVerified, linkedProviders}) — this now matches the @ax/core
 * `UserProfile` shape (canonicalized in P1-73). This per-app client remains so the shell
 * can greet the editor by email and gate tag-definition writes on ADMIN.
 * The token lifecycle (login/logout/cookie) stays with the shared @ax/core
 * authStore. Domain client — composes the shared @ax/core authed fetch.
 *
 * Curl-verified (2026-06-05, demo@ax.dev):
 *   GET /api/auth/me -> 200 { userId, email, role:"ADMIN", emailVerified, linkedProviders }
 */
import { apiFetch } from '@ax/core';

export interface StudioProfile {
  userId: string;
  email: string;
  role: string;
  emailVerified: boolean;
  linkedProviders: string[];
}

export const profileClient = {
  me: (): Promise<StudioProfile> => apiFetch<StudioProfile>('/auth/me'),
};
