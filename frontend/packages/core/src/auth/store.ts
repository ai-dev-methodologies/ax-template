// Minimal auth state driven by /auth/me
export interface AuthState {
  isAuthenticated: boolean;
  user: {
    userId: string;
    email: string;
    roles: string[];
    providerLinks: string[];
    verificationState: string;
  } | null;
  isLoading: boolean;
}
