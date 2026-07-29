// Minimal auth state driven by /auth/me.
// P1-73 — canonicalized to the real GET /api/auth/me shape (UserProfileResponse):
// a single `role` string, a boolean `emailVerified`, and `linkedProviders` as a flat
// array of provider-name strings.
export interface AuthState {
  isAuthenticated: boolean;
  user: {
    userId: string;
    email: string;
    role: string;
    linkedProviders: string[];
    emailVerified: boolean;
  } | null;
  isLoading: boolean;
}
