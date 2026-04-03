import { http, HttpResponse } from 'msw';

const API = '/api';

export const handlers = [
  http.post(`${API}/auth/email/signup`, () =>
    HttpResponse.json({ userId: 'test-uuid-123', verificationRequired: true }, { status: 201 })
  ),
  http.post(`${API}/auth/email/login`, () =>
    HttpResponse.json({ accessToken: 'mock-access-token', expiresIn: 3600 })
  ),
  http.post(`${API}/auth/email/verify-email`, () =>
    HttpResponse.json({ message: 'Email verified successfully' })
  ),
  http.post(`${API}/auth/email/resend-verification`, () =>
    HttpResponse.json({ message: 'Verification email resent' })
  ),
  http.post(`${API}/auth/refresh`, () =>
    HttpResponse.json({ accessToken: 'new-mock-token', expiresIn: 3600 })
  ),
  http.post(`${API}/auth/logout`, () =>
    new HttpResponse(null, { status: 204 })
  ),
  http.get(`${API}/auth/me`, () =>
    HttpResponse.json({ 
      userId: 'test-uuid-123', 
      email: 'test@example.com', 
      roles: ['USER'], 
      verificationState: 'verified',
      providerLinks: [{ provider: 'email', connectedAt: new Date().toISOString() }]
    })
  ),
  http.post(`${API}/auth/email/password-reset-request`, () =>
    HttpResponse.json({ message: 'If the email exists, a reset link has been sent' })
  ),
  http.post(`${API}/auth/email/password-reset`, () =>
    HttpResponse.json({ message: 'Password reset successful' })
  ),
  http.post(`${API}/auth/email/password-change`, () =>
    HttpResponse.json({ message: 'Password changed successfully' })
  ),
];
