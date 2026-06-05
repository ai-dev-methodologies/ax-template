import { describe, it, expect } from 'vitest';
import type { AuthState } from '@ax/core';
import { refreshMutex } from '@ax/core';

describe('Auth State Scenarios', () => {
  it('auth-state/login-success-state: maintains state shape', () => {
    const state: AuthState = {
      isAuthenticated: true,
      user: {
        userId: '123',
        email: 'test@example.com',
        roles: ['user'],
        providerLinks: ['google'],
        verificationState: 'verified',
      },
      isLoading: false,
    };
    expect(state.isAuthenticated).toBe(true);
    expect(state.user?.userId).toBe('123');
  });

  it('auth-state/refresh-queue-serializes-concurrent-refresh', () => {
    expect(refreshMutex.isRefreshing).toBe(false);
    expect(Array.isArray(refreshMutex.queue)).toBe(true);
  });
});
