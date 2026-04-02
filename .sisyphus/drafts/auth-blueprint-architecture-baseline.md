# Draft: Auth Blueprint Architecture Baseline

목적: auth blueprint의 실제 구현이 어떤 구조 규칙을 따라야 하는지 고정한다.

## Core Rules
1. OpenAPI is source of truth
   - backend와 frontend는 계약에서 출발한다
   - provider flag는 contract shape를 바꾸지 않는다

2. Spring Security built-in JWT flow only
   - custom JWT filter를 기본값으로 쓰지 않는다
   - resource server / standard security config를 우선한다

3. Stateful refresh token
   - refresh token은 HttpOnly cookie + server-side state
   - rotation은 grace window 포함
   - refresh race는 명시적으로 다룬다

4. `/auth/me` minimal UI state only
   - userId
   - email
   - roles
   - providerLinks
   - verificationState
   - profile/settings/admin scope 금지

5. Explicit account linking
   - 자동 email merge 금지
   - linking conflict는 명시적 에러와 flow로 처리

6. CSRF/CORS mandatory
   - cookie auth를 선택한 이상 기본 포함
   - missing defaults는 reject_if 대상

7. Provider flags are runtime-only
   - config/property 기반
   - disabled provider는 structured error
   - schema branching 금지

8. Rate limits are policy, not note
   - login / resend limits는 manifest 정책 필드로 유지
   - OpenAPI는 그 정책을 구현하는 계약일 뿐

## Boundary Rules
### backend/
- controller는 transport boundary만 담당
- business logic in controller 금지
- auth/security/user boundary 분리

### frontend/
- auth state는 `/auth/me` 기반
- refresh queue/mutex는 auth boundary 안에만 존재
- 전역 상태에 server state를 밀어넣지 않는다

### verify/
- manifest를 읽고 contract/security/rbac를 검사
- fail-open 금지

## Auth State Flow
1. Email signup 또는 provider login 요청이 시작점이다.
2. provider login에서 provider가 disabled면 즉시 structured error로 종료한다.
3. identity resolution 후 기존 사용자와 신규 사용자 생성을 구분한다.
4. provider identity가 다른 기존 계정과 충돌하면 account link conflict로 종료하고, 자동 merge는 하지 않는다.
5. Email signup 결과가 unverified user면 verification completion 전까지 제한 상태를 유지한다.
6. verification 완료 또는 허용된 login 성공 후에만 access/refresh 발급 단계로 진행한다.
7. UI auth state는 `/auth/me`를 canonical read model로 사용한다.
8. protected route 진입 판단은 `/auth/me` 응답과 role-aware state만 기준으로 한다.
9. access token 만료 시 frontend는 refresh queue/mutex 안에서 refresh를 직렬화한다.
10. refresh 성공 시 `/auth/me`를 다시 읽어 state를 복구한다.
11. refresh denied / invalid session이면 인증 상태를 종료하고 login 필요 상태로 되돌린다.
12. 이 flow에서 provider disabled, unverified user, refresh failure, account link conflict는 모두 명시적 분기여야 한다.

## Failure Paths that must exist
- provider disabled
- invalid credentials
- unverified user
- expired verification token
- refresh denied / invalid session
- refresh race
- account link conflict
- rate limit hit
