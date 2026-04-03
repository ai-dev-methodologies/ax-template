import { create } from 'zustand';
import { authClient, UserProfile } from '../api/authClient';

interface AuthState {
  user: UserProfile | null;
  accessToken: string | null;
  isLoading: boolean;
  error: string | null;
}

interface AuthActions {
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  signup: (email: string, password: string) => Promise<void>;
  fetchMe: () => Promise<void>;
  clearError: () => void;
}

export const useAuthStore = create<AuthState & AuthActions>((set, get) => ({
  user: null,
  accessToken: null,
  isLoading: false,
  error: null,

  signup: async (email, password) => {
    set({ isLoading: true, error: null });
    try {
      await authClient.signup({ email, password });
      set({ isLoading: false });
    } catch (e: any) {
      set({ isLoading: false, error: e.message });
      throw e;
    }
  },

  login: async (email, password) => {
    set({ isLoading: true, error: null });
    try {
      const res = await authClient.login({ email, password });
      set({ accessToken: res.accessToken, isLoading: false });
      await get().fetchMe();
    } catch (e: any) {
      set({ isLoading: false, error: e.message });
      throw e;
    }
  },

  logout: async () => {
    try { 
      await authClient.logout(); 
    } catch (e) {
      console.error(e);
    }
    set({ user: null, accessToken: null, error: null });
  },

  fetchMe: async () => {
    const token = get().accessToken;
    if (!token) return;
    try {
      const user = await authClient.me(token);
      set({ user });
    } catch (e: any) {
      set({ error: e.message });
    }
  },

  clearError: () => set({ error: null }),
}));
