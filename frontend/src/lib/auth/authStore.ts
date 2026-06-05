import { create } from 'zustand';
import { authClient, UserProfile } from '../api/authClient';
import {
  clearAccessTokenCookie,
  readAccessTokenCookie,
  setAccessTokenCookie,
} from './token-cookie';

interface AuthState {
  user: UserProfile | null;
  accessToken: string | null;
  isLoading: boolean;
  error: string | null;
  meError: string | null;
  hydrated: boolean;
}

interface AuthActions {
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  signup: (email: string, password: string) => Promise<void>;
  fetchMe: () => Promise<void>;
  /** Rehydrate the in-memory store from the persisted cookie on app start. */
  hydrate: () => void;
  clearError: () => void;
}

/** authClient throws `Error & { status }`; narrow it without `any`. */
function errorMessage(e: unknown): string {
  return e instanceof Error ? e.message : 'Unexpected error';
}

function errorStatus(e: unknown): number | undefined {
  if (e instanceof Error && 'status' in e) {
    const { status } = e as { status: unknown };
    if (typeof status === 'number') return status;
  }
  return undefined;
}

/** An expired/invalid token (401/403) means the session is no longer valid. */
function isUnauthorized(e: unknown): boolean {
  const status = errorStatus(e);
  return status === 401 || status === 403;
}

export const useAuthStore = create<AuthState & AuthActions>((set, get) => ({
  user: null,
  accessToken: null,
  isLoading: false,
  error: null,
  meError: null as string | null,
  hydrated: false,

  signup: async (email, password) => {
    set({ isLoading: true, error: null });
    try {
      await authClient.signup({ email, password });
      set({ isLoading: false });
    } catch (e: unknown) {
      set({ isLoading: false, error: errorMessage(e) });
      throw e;
    }
  },

  login: async (email, password) => {
    set({ isLoading: true, error: null });
    try {
      const res = await authClient.login({ email, password });
      setAccessTokenCookie(res.accessToken);
      set({ accessToken: res.accessToken, isLoading: false });
      await get().fetchMe();
    } catch (e: unknown) {
      set({ isLoading: false, error: errorMessage(e) });
      throw e;
    }
  },

  logout: async () => {
    try {
      await authClient.logout();
    } catch (e: unknown) {
      console.error(errorMessage(e));
    }
    clearAccessTokenCookie();
    set({ user: null, accessToken: null, error: null, meError: null });
  },

  fetchMe: async () => {
    const token = get().accessToken;
    if (!token) return;
    set({ meError: null });
    try {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 5000);
      const user = await authClient.me(token);
      clearTimeout(timeout);
      set({ user, meError: null });
    } catch (e: unknown) {
      // Expired/invalid token: drop the session so the route guards redirect
      // to /login instead of leaving the user stuck on a broken page.
      if (isUnauthorized(e)) {
        await get().logout();
        return;
      }
      set({ meError: errorMessage(e) });
    }
  },

  hydrate: () => {
    if (get().hydrated) return;
    const token = readAccessTokenCookie();
    if (token) {
      set({ accessToken: token, hydrated: true });
      void get().fetchMe();
    } else {
      set({ hydrated: true });
    }
  },

  clearError: () => set({ error: null }),
}));
