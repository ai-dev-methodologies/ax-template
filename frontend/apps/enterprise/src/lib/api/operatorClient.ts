/**
 * Operator identity client — GET /api/auth/me.
 *
 * The live backend returns a SINGLE `role` string ({userId, email, role,
 * emailVerified, linkedProviders}) — this now matches the @ax/core
 * `UserProfile` shape (canonicalized in P1-73). This per-app client remains so the
 * ADMIN gate has an app-scoped type rather than depending on the shared shape
 * directly. The token lifecycle (login/logout/cookie) stays with the shared @ax/core authStore.
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
