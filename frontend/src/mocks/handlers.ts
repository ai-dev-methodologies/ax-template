import { http, HttpResponse } from 'msw';
// P1-73 — the SAME golden fixture the BE contract-parity test
// (AuthMeGoldenContractParityTest) serializes and compares against, so this mock can
// never drift from the real GET /api/auth/me wire shape.
import authMeGolden from '../../tests/_fixtures/auth-me.golden.json';

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
  http.get(`${API}/auth/me`, () => HttpResponse.json(authMeGolden)),
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
