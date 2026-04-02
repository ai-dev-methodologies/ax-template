# Auth Blueprint Verification Checklist

## 1. Auth Boundary & OpenAPI Contract
- [ ] `contracts/auth-openapi.yaml` acts as the single source of truth for auth endpoints.
- [ ] Implementation precisely matches the OpenAPI schema for requests, responses, and error definitions.
- [ ] The `/auth/me` endpoint returns minimal UI state (userId, email, roles, providerLinks, verificationState).
- [ ] Controller layer is free of business logic.

## 2. Security Defaults (CSRF & CORS)
- [ ] CSRF protection is explicitly enabled.
- [ ] CORS explicitly allows designated frontend origins and requires `Allow-Credentials`.
- [ ] Hardcoded secrets are rejected; secure environment variables are required.
- [ ] Default Spring Security OAuth2/JWT mechanisms are used (no custom JWT filters as the default).

## 3. Token Management & Refresh
- [ ] Access token is delivered via response body or memory (not URL or local storage).
- [ ] Refresh token is delivered via an `HttpOnly`, `Secure`, `SameSite=Strict` cookie.
- [ ] Refresh tokens are tracked statefully on the server.
- [ ] Refresh token rotation includes a grace window to handle concurrent requests.

## 4. Provider Fallback & Account Linking
- [ ] Disabled or misconfigured providers return a `403 ProviderDisabledError` without altering the schema.
- [ ] Account linking is strictly explicit; automatic email merging is disabled.
- [ ] Attempting to link an already-linked account returns a `409 AccountLinkConflictError`.

## 5. Verification & Unverified State
- [ ] Unverified users are kept in a separated state distinct from verified identities.
- [ ] Email verification logic is idempotent.
- [ ] Rate limits are applied to `/auth/email/resend-verification`.
- [ ] Expiry rules are enforced on verification links.

## 6. Rate Limiting
- [ ] Login endpoint is rate-limited by IP and identifier.
- [ ] Over-limit requests return a `429 RateLimitError` containing a `retryAfter` value.

## 7. Testing Baseline
- [ ] Backend integration tests cover auth endpoints, filters, and standard error states.
- [ ] Frontend tests validate auth state transitions (login, refresh, logout, unverified).
- [ ] Key flow E2E tests pass for the core signup-to-login lifecycle.
- [ ] Real OAuth browser E2E tests are explicitly excluded for V1.

## 8. Verify Triplet
- [ ] Security verify step passes (confirms CSRF, CORS, HttpOnly cookies).
- [ ] Contract verify step passes (confirms implementation matches OpenAPI).
- [ ] RBAC verify step passes (confirms role enforcement).
