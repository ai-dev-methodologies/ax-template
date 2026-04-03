import React from 'react';
import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
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
  cleanup();
  server.resetHandlers();
  useAuthStore.setState({ user: null, accessToken: null, isLoading: false, error: null });
});
afterAll(() => server.close());

// Helper to render with router
function renderWithRouter(ui: React.ReactElement, { route = '/' } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      {ui}
    </MemoryRouter>
  );
}

describe('SignupPage', () => {
  it('renders signup form', () => {
    renderWithRouter(<SignupPage />);
    expect(screen.getByRole('heading', { name: /sign up/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('rejects password < 12 chars', () => {
    const alertMock = vi.spyOn(window, 'alert').mockImplementation(() => {});
    renderWithRouter(<SignupPage />);
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'short' } });
    fireEvent.click(screen.getByRole('button', { name: /sign up/i }));
    
    expect(alertMock).toHaveBeenCalledWith('Password must be at least 12 characters');
    alertMock.mockRestore();
  });

  it('shows success message after signup', async () => {
    renderWithRouter(<SignupPage />);
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'securepassword12' } });
    fireEvent.click(screen.getByRole('button', { name: /sign up/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/check your email/i)).toBeInTheDocument();
    });
  });
});

describe('LoginPage', () => {
  it('renders login form', () => {
    renderWithRouter(<LoginPage />);
    expect(screen.getByRole('heading', { name: /login/i })).toBeInTheDocument();
  });

  it('shows error on failed login', async () => {
    const { http, HttpResponse } = await import('msw');
    server.use(
      http.post('/api/auth/email/login', () =>
        HttpResponse.json({ message: 'Invalid credentials' }, { status: 401 })
      )
    );
    renderWithRouter(<LoginPage />);
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'bad@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'wrongpassword' } });
    fireEvent.click(screen.getByRole('button', { name: /login/i }));
    await waitFor(() => {
      expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
    });
  });
});

describe('VerifyPage', () => {
  it('shows verifying state initially and then success', async () => {
    renderWithRouter(<VerifyPage />, { route: '/verify?token=test-token' });
    expect(screen.getByText(/verifying/i)).toBeInTheDocument();
    
    await waitFor(() => {
      expect(screen.getByText(/email verified successfully/i)).toBeInTheDocument();
    });
  });
});

describe('DashboardPage', () => {
  it('shows not logged in when no user', () => {
    renderWithRouter(<DashboardPage />);
    expect(screen.getByText(/not logged in/i)).toBeInTheDocument();
  });

  it('shows user email when logged in', () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'user@example.com', roles: ['USER'], verificationState: 'VERIFIED', providerLinks: [] },
      accessToken: 'mock-token',
      isLoading: false,
      error: null
    });
    renderWithRouter(<DashboardPage />);
    expect(screen.getByText(/user@example.com/i)).toBeInTheDocument();
  });
});
