import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { setupServer } from 'msw/node';
import { handlers } from '../src/mocks/handlers';
import { useAuthStore } from '@ax/core';
// P1-73 — the /auth/me MSW mock now serves this golden fixture verbatim; assert against
// it rather than a hand-picked literal so the test cannot silently drift from the mock.
import authMeGolden from './_fixtures/auth-me.golden.json';

const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  useAuthStore.setState({ user: null, accessToken: null, isLoading: false, error: null });
});
afterAll(() => server.close());

describe('Auth Store', () => {
  it('signup sets loading state and resolves', async () => {
    const store = useAuthStore.getState();
    await store.signup('test@example.com', 'securepassword12');
    expect(useAuthStore.getState().isLoading).toBe(false);
    expect(useAuthStore.getState().error).toBeNull();
  });

  it('login sets accessToken and fetches user', async () => {
    const store = useAuthStore.getState();
    await store.login('test@example.com', 'securepassword12');
    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('mock-access-token');
    expect(state.user).not.toBeNull();
    expect(state.user?.email).toBe(authMeGolden.email);
  });

  it('logout clears user and token', async () => {
    useAuthStore.setState({
      accessToken: 'mock-token',
      user: {
        userId: '1',
        email: 'a@b.com',
        role: 'MEMBER',
        emailVerified: true,
        linkedProviders: []
      }
    });
    await useAuthStore.getState().logout();
    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.accessToken).toBeNull();
  });

  it('login failure sets error', async () => {
    const { http, HttpResponse } = await import('msw');
    server.use(
      http.post('/api/auth/email/login', () =>
        HttpResponse.json({ message: 'Invalid credentials' }, { status: 401 })
      )
    );
    await expect(useAuthStore.getState().login('bad@email.com', 'wrongpass')).rejects.toThrow();
    expect(useAuthStore.getState().error).toBeTruthy();
  });
});
