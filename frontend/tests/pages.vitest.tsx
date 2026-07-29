/// <reference types="@testing-library/jest-dom/vitest" />
import React from 'react';
import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { setupServer } from 'msw/node';
import { handlers } from '../src/mocks/handlers';
import { useAuthStore } from '@ax/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
// P1-73 — same golden the BE contract-parity test (AuthMeGoldenContractParityTest) and the
// /auth/me MSW mock (src/mocks/handlers.ts) consume, so the DashboardPage render assertions
// below can never silently tolerate a shape the backend does not actually emit.
import authMeGolden from './_fixtures/auth-me.golden.json';

// Mock next/navigation for Vitest (not available in jsdom)
const mockPush = vi.fn();
const mockReplace = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => '/',
}));

// Mock next/link — render as plain <a>
vi.mock('next/link', () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) =>
    React.createElement('a', { href }, children),
}));

const server = setupServer(...handlers);
beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }));
afterEach(() => {
  server.resetHandlers();
  useAuthStore.setState({ user: null, accessToken: null, isLoading: false, error: null, meError: null });
  mockPush.mockClear();
  mockReplace.mockClear();
});
afterAll(() => server.close());

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

// Dynamic imports of new Next.js pages (to avoid hoisting issues with vi.mock)
const getLoginPage = async () => (await import('../src/app/(auth)/login/page')).default;
const getSignupPage = async () => (await import('../src/app/(auth)/signup/page')).default;
const getDashboardPage = async () => (await import('../src/app/(authenticated)/dashboard/page')).default;

describe('SignupPage', () => {
  it('renders signup form', async () => {
    const SignupPage = await getSignupPage();
    renderWithQuery(<SignupPage />);
    expect(screen.getByRole('heading', { name: /회원가입/i })).toBeInTheDocument();
    const inputs = screen.getAllByRole('textbox');
    expect(inputs.length).toBeGreaterThanOrEqual(1);
  });

  it('shows success after signup', async () => {
    const SignupPage = await getSignupPage();
    renderWithQuery(<SignupPage />);
    const emailInput = screen.getAllByRole('textbox')[0];
    const passwordInput = document.querySelector('input[type="password"]')!;
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'securepassword12' } });
    fireEvent.click(screen.getAllByRole('button', { name: /회원가입/i })[0]);
    await waitFor(() => {
      expect(screen.getByText(/이메일을 확인/i)).toBeInTheDocument();
    });
  });
});

describe('LoginPage', () => {
  it('renders OAuth buttons', async () => {
    const LoginPage = await getLoginPage();
    renderWithQuery(<LoginPage />);
    expect(screen.getByText(/Google 로그인/i)).toBeInTheDocument();
    expect(screen.getByText(/Kakao 로그인/i)).toBeInTheDocument();
    expect(screen.getByText(/Naver 로그인/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /이메일 로그인/i })).toBeInTheDocument();
  });

  it('has email login form', async () => {
    const LoginPage = await getLoginPage();
    renderWithQuery(<LoginPage />);
    expect(screen.getAllByRole('button', { name: /이메일 로그인/i })[0]).toBeInTheDocument();
    const emailInput = screen.getAllByRole('textbox')[0];
    expect(emailInput).toBeInTheDocument();
    const passwordInput = document.querySelector('input[type="password"]');
    expect(passwordInput).toBeTruthy();
  });
});

describe('DashboardPage', () => {
  it('renders dashboard when authenticated', async () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'user@example.com', role: 'MEMBER', emailVerified: true, linkedProviders: [] },
      accessToken: 'mock-token',
      isLoading: false,
      error: null,
      meError: null,
    });
    const DashboardPage = await getDashboardPage();
    renderWithQuery(<DashboardPage />);
    expect(screen.getAllByText('Dashboard')[0]).toBeInTheDocument();
    expect(screen.getAllByText(/MEMBER/)[0]).toBeInTheDocument();
  });

  // P1-73 — behavioural proof off the real BE-emitted golden shape, not just a hand-picked
  // literal: kills the silent-empty class (role missing, verified always false, providers
  // never rendered) that a type-only fix would not catch.
  it('renders non-empty role, the verified badge, and every linked provider from the golden', async () => {
    useAuthStore.setState({
      user: { ...authMeGolden },
      accessToken: 'mock-token',
      isLoading: false,
      error: null,
      meError: null,
    });
    const DashboardPage = await getDashboardPage();
    renderWithQuery(<DashboardPage />);

    expect(screen.getByText(authMeGolden.role)).toBeInTheDocument();
    expect(screen.getByText('인증됨')).toBeInTheDocument();
    expect(authMeGolden.linkedProviders.length).toBeGreaterThan(0);
    for (const provider of authMeGolden.linkedProviders) {
      expect(screen.getByText(new RegExp(provider, 'i'))).toBeInTheDocument();
    }
  });
});
