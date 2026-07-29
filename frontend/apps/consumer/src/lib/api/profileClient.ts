/**
 * Profile client — GET /api/auth/me.
 *
 * The live backend returns a SINGLE `role` string ({userId, email, role,
 * emailVerified, linkedProviders}) — this now matches the @ax/core
 * `UserProfile` shape (canonicalized in P1-73). This per-app client remains so the shell
 * can greet the user by email. The token lifecycle (login/logout/cookie) stays
 * with the shared @ax/core authStore.
 * Domain client — composes the shared @ax/core authed fetch.
 */
import { apiFetch } from '@ax/core';

export interface ConsumerProfile {
  userId: string;
  email: string;
  role: string;
  emailVerified: boolean;
  linkedProviders: string[];
}

export const profileClient = {
  me: (): Promise<ConsumerProfile> => apiFetch<ConsumerProfile>('/auth/me'),
};
