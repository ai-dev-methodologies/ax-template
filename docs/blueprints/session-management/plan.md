# Session Management Blueprint — Plan (R33 retrofit)

> Retrofit notice: authored after-the-fact at R37. Future domains MUST do this at S1.

## Domain
Explicit per-login session records bound to JWT jti — multi-device list / revoke /
forced logout + SessionRevocationCheck SPI for JWT auth filter integration.

## Scope
**In v1**: register/list/get/revoke/heartbeat/admin-force-logout endpoints +
SessionRevocationCheck SPI (fail-closed default) + IpAddressMasker (privacy posture)
+ UserAgentSummarizer + max-active-sessions cap with auto-revoke + expiresAt past
rejection (iter1 closure).

**Out of scope (`not_for`)**: pure stateless JWT-only, server-side session replacing
JWT, cross-domain SSO, multi-tenant, raw IP/UA in DTOs.

**Deferred v2**: automatic JWT chain integration, geo IP enrichment, suspicious-login
detection, session pagination.

## External standards anchored
- OWASP ASVS V3 — Session Management
- RFC 7519 §4.1.7 — JWT jti claim
- Stripe API session list pattern, GitHub active sessions UI

## Acceptance gates
1. `./gradlew testSessionManagement` exits 0 (23 tests including 6 violation proof)
2. verify-completion exit 0
3. dogfood loop produces real bug findings (R33-iter1 yielded 2: max-sessions, expiresAt past) ✓
4. S7 audit produces decisions.md (see decisions.md)
