/// <reference types="@testing-library/jest-dom/vitest" />
import React from 'react';
import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { setupServer } from 'msw/node';
import { handlers } from '../src/mocks/handlers';
import { MemoryRouter } from 'react-router-dom';
import { SignupPage } from '../src/pages/SignupPage';
import { LoginPage } from '../src/pages/LoginPage';
import { VerifyPage } from '../src/pages/VerifyPage';
import { DashboardPage } from '../src/pages/DashboardPage';
import { useAuthStore } from '../src/lib/auth/authStore';

const server = setupServer(...handlers);
beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }));
afterEach(() => {
  server.resetHandlers();
  useAuthStore.setState({ user: null, accessToken: null, isLoading: false, error: null, meError: null });
});
afterAll(() => server.close());

function renderWithRouter(ui: React.ReactElement, { route = '/' } = {}) {
  return render(<MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>);
}

describe('SignupPage', () => {
  it('renders signup form', () => {
    renderWithRouter(<SignupPage />);
    expect(screen.getByRole('heading', { name: /회원가입/i })).toBeInTheDocument();
    // email + password inputs exist
    const inputs = screen.getAllByRole('textbox');
    expect(inputs.length).toBeGreaterThanOrEqual(1);
  });

  it('shows success after signup', async () => {
    renderWithRouter(<SignupPage />);
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
  it('renders OAuth buttons', () => {
    renderWithRouter(<LoginPage />);
    expect(screen.getByText(/Google 로그인/i)).toBeInTheDocument();
    expect(screen.getByText(/Kakao 로그인/i)).toBeInTheDocument();
    expect(screen.getByText(/Naver 로그인/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /이메일 로그인/i })).toBeInTheDocument();
  });

  it('has email login form', () => {
    renderWithRouter(<LoginPage />);
    expect(screen.getAllByRole('button', { name: /이메일 로그인/i })[0]).toBeInTheDocument();
    const emailInput = screen.getAllByRole('textbox')[0];
    expect(emailInput).toBeInTheDocument();
    const passwordInput = document.querySelector('input[type="password"]');
    expect(passwordInput).toBeTruthy();
  });
});

describe('VerifyPage', () => {
  it('shows verification heading', () => {
    renderWithRouter(<VerifyPage />, { route: '/verify?token=test-token' });
    expect(screen.getByRole('heading', { name: /verification/i })).toBeInTheDocument();
  });
});

describe('DashboardPage', () => {
  it('shows login prompt when not authenticated', () => {
    renderWithRouter(<DashboardPage />);
    expect(screen.getByText(/로그인이 필요합니다/i)).toBeInTheDocument();
  });

  it('shows dashboard when authenticated', () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'user@example.com', roles: ['MEMBER'], verificationState: 'verified', providerLinks: [] },
      accessToken: 'mock-token',
      isLoading: false,
      error: null,
      meError: null,
    });
    renderWithRouter(<DashboardPage />);
    expect(screen.getAllByText('Dashboard')[0]).toBeInTheDocument();
    expect(screen.getAllByText(/MEMBER/)[0]).toBeInTheDocument();
  });
});
