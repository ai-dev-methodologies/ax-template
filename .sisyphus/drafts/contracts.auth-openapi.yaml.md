# Draft File: contracts/auth-openapi.yaml

```yaml
openapi: 3.0.3
info:
  title: ax-template Auth API
  version: 0.1.0
servers:
  - url: /api
paths:
  /auth/email/signup:
    post:
      summary: Sign up with email
  /auth/email/login:
    post:
      summary: Login with email
  /auth/email/resend-verification:
    post:
      summary: Resend email verification
  /auth/verify-email:
    get:
      summary: Verify email token
  /auth/refresh:
    post:
      summary: Refresh access token
  /auth/logout:
    post:
      summary: Logout current session
  /auth/me:
    get:
      summary: Get minimal auth UI state
  /auth/link-account:
    post:
      summary: Explicitly link another provider to current identity
components:
  schemas:
    UserRole:
      type: string
      enum: [admin, manager, member]
    VerificationState:
      type: string
      enum: [verified, unverified, expired]
    ProviderLink:
      type: object
    AuthState:
      type: object
      required: [userId, email, roles, verificationState]
    ErrorResponse:
      type: object
      required: [code, message]
    ProviderDisabledError:
      allOf:
        - $ref: '#/components/schemas/ErrorResponse'
    RateLimitError:
      allOf:
        - $ref: '#/components/schemas/ErrorResponse'
    AccountLinkRequest:
      type: object
    AccountLinkResponse:
      type: object
    RefreshResponse:
      type: object
      required: [accessToken, expiresIn]
```

## Mandatory endpoint rules
- `/auth/me`는 profile/settings를 반환하지 않음
- provider disabled는 structured error 반환
- refresh race는 explicit error/response semantics 가짐
- role management endpoint 포함 금지
- user profile editing endpoint 포함 금지
- account linking은 explicit endpoint로만 처리

## Error conditions that must be represented
- provider disabled
- invalid credentials
- unverified user
- expired verification token
- rate limit hit
- refresh denied / session invalid
- account linking conflict
