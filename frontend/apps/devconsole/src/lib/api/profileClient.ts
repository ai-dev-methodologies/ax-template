/**
 * Profile client — GET /api/auth/me.
 *
 * The live backend returns a SINGLE `role` string ({userId, email, role,
 * emailVerified, linkedProviders}), which differs from the @ax/core
 * `UserProfile` shape (roles[]). We read the real projection here so the shell
 * can greet the developer by email and surface the ADMIN role (webhook admin
 * endpoints require ROLE_ADMIN). The token lifecycle (login/logout/cookie) stays
 * with the shared @ax/core authStore. Domain client — composes the shared
 * @ax/core authed fetch (no raw-exchange capture needed for the header).
 *
 * Curl-verified (2026-06-05, demo@ax.dev):
 *   GET /api/auth/me -> 200 { userId, email, role:"ADMIN", emailVerified, linkedProviders }
 */
import { apiFetch } from '@ax/core';

export interface ConsoleProfile {
  userId: string;
  email: string;
  role: string;
  emailVerified: boolean;
  linkedProviders: string[];
}

export const profileClient = {
  me: (): Promise<ConsoleProfile> => apiFetch<ConsoleProfile>('/auth/me'),
};
