/**
 * Operator identity client. The shared auth store's `UserProfile` type predates
 * the live backend shape, so the enterprise console reads the real
 * `GET /api/auth/me` projection here to gate ADMIN-only surfaces.
 *
 * Live response (observed):
 *   { userId, email, role: "ADMIN" | "MANAGER" | "MEMBER",
 *     emailVerified: boolean, linkedProviders: string[] }
 */
import { apiFetch } from './enterpriseHttp';

export type OperatorRole = 'ADMIN' | 'MANAGER' | 'MEMBER';

export interface OperatorIdentity {
  userId: string;
  email: string;
  role: OperatorRole;
  emailVerified: boolean;
  linkedProviders: string[];
}

export const operatorClient = {
  me: () => apiFetch<OperatorIdentity>('/auth/me'),
};
