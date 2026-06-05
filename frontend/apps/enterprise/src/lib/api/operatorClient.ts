/**
 * Operator identity client — GET /api/auth/me.
 *
 * The live backend returns a SINGLE `role` string ({userId, email, role,
 * emailVerified, linkedProviders}), which differs from the @ax/core
 * `UserProfile` shape (roles[]). We read the real shape here so the ADMIN gate
 * is driven by the actual projection rather than a mismatched field. The token
 * lifecycle (login/logout/cookie) stays with the shared @ax/core authStore.
 * Domain client — composes the shared @ax/core authed fetch.
 */
import { apiFetch } from '@ax/core';

export interface OperatorIdentity {
  userId: string;
  email: string;
  role: string;
  emailVerified: boolean;
  // Backend UserProfileResponse returns List<String> (e.g. ["GOOGLE","NAVER"]), not objects.
  linkedProviders: string[];
}

export const operatorClient = {
  me: (): Promise<OperatorIdentity> => apiFetch<OperatorIdentity>('/auth/me'),
};
