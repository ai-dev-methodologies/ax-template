const API_BASE = '/api';

export interface SignupRequest { email: string; password: string; }
export interface SignupResponse { userId: string; verificationRequired: boolean; }
export interface LoginRequest { email: string; password: string; }
export interface LoginResponse { accessToken: string; expiresIn: number; }
/**
 * P1-73 — matches GET /api/auth/me byte-for-byte (AuthSessionController#me,
 * UserProfileResponse{userId, email, role, emailVerified, linkedProviders}; see
 * contracts/auth-openapi.yaml `AuthState`). `role` is a SINGLE string, never a roles
 * array; `linkedProviders` is a flat array of provider names, never provider-link objects.
 */
export interface UserProfile {
  userId: string;
  email: string;
  role: string;
  emailVerified: boolean;
  linkedProviders: string[];
}
export interface VerifyEmailRequest { token: string; }
export interface PasswordResetRequest { email: string; }
export interface PasswordResetConfirm { token: string; newPassword: string; }
export interface PasswordChangeRequest { currentPassword: string; newPassword: string; }

async function request<T>(path: string, options: RequestInit = {}, isRetry = false): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: res.statusText }));
    throw Object.assign(new Error(error.message || 'Request failed'), { status: res.status, data: error });
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export const authClient = {
  signup: (body: SignupRequest) => request<SignupResponse>('/auth/email/signup', { method: 'POST', body: JSON.stringify(body) }),
  login: (body: LoginRequest) => request<LoginResponse>('/auth/email/login', { method: 'POST', body: JSON.stringify(body) }),
  verifyEmail: (body: VerifyEmailRequest) => request<{ message: string }>('/auth/email/verify-email', { method: 'POST', body: JSON.stringify(body) }),
  resendVerification: (body: { email: string }) => request<{ message: string }>('/auth/email/resend-verification', { method: 'POST', body: JSON.stringify(body) }),
  refresh: () => request<LoginResponse>('/auth/refresh', { method: 'POST' }),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
  me: (token: string) => request<UserProfile>('/auth/me', { headers: { Authorization: `Bearer ${token}` } }),
  passwordResetRequest: (body: PasswordResetRequest) => request<{ message: string }>('/auth/email/password-reset-request', { method: 'POST', body: JSON.stringify(body) }),
  passwordReset: (body: PasswordResetConfirm) => request<{ message: string }>('/auth/email/password-reset', { method: 'POST', body: JSON.stringify(body) }),
  passwordChange: (body: PasswordChangeRequest, token: string) => request<{ message: string }>('/auth/email/password-change', { method: 'POST', body: JSON.stringify(body), headers: { Authorization: `Bearer ${token}` } }),
};
