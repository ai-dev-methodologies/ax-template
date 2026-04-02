# Auth Blueprint Preflight

## 1. Current State
현재 프로젝트는 `draft` 상태이며 핵심 설계 방향이 수립되었습니다. 구현에 앞서 backend/frontend/verify 작업자들이 동일한 기준으로 작업할 수 있도록, 기초가 되는 버전, 정책, 계약을 고정하는 `foundations-trio` 작업이 즉시 필요합니다.

## 2. First Session Order
Wave 1 첫 구현 세션은 반드시 다음 순서를 따릅니다:
1. **Fix toolchain versions**: `blueprints/pinned-versions.yaml` 작성 (의존성 및 빌드 도구 버전 고정)
2. **Freeze manifest policies**: `blueprints/auth-manifest.yaml` 작성 (provider, token, RBAC, rate limit 정책 고정)
3. **Derive static contract**: `contracts/auth-openapi.yaml` 작성 (manifest 기반 schema-first OpenAPI 스펙 고정)
4. **Align verify schema**: `verify/manifest.schema.json` placeholder 작성 (검증 엔진이 기대하는 최소 필드 정의)

## 3. Open Micro-Decisions
이번 세션에서 확정해야 할 4가지 잔여 마이크로 결정과 권장 기본값입니다:
1. **Backend Build Tool**: `gradle-kotlin` vs `maven`
   - *Recommended Default*: `gradle-kotlin`
2. **Disabled Provider Error Shape**: 비활성화된 소셜 제공자 접근 시 응답 구조
   - *Recommended Default*: HTTP 400 Bad Request, `{ "code": "PROVIDER_DISABLED" }`
3. **Unverified State Representation**: 이메일 미인증 상태 처리 방식
   - *Recommended Default*: HTTP 403 Forbidden, `{ "code": "UNVERIFIED_EMAIL" }` (또는 제한된 스코프의 임시 토큰 발급)
4. **Refresh Race Condition Response**: 동시 Refresh 요청 경합 시 처리 방식
   - *Recommended Default*: HTTP 401 Unauthorized (Strict deny 처리 후 안전하게 재로그인 유도)

## 4. Locked Decisions (Do not decide again)
다음 항목은 이미 결정되었으므로 다시 논의하거나 변경하지 마십시오:
- **OpenAPI Version**: `3.0.3` 고정 (Provider 변경에 따라 schema가 동적으로 바뀌지 않음)
- **Token Delivery**: Spring Security built-in JWT flow 사용
- **Refresh Token**: Stateful 스토리지 관리
- **Rate Limits**: Login (5회/15분/IP+Identifier), Resend (3회/10분/Email)
- **Account Linking**: Implicit merge 금지, 반드시 Explicit linking flow 요구
- **`/auth/me` Scope**: 프로필 정보 배제, UI 라우팅/렌더링 제어를 위한 최소 상태(minimal UI state)만 반환

## 5. Success Signal for Session 1
- `pinned-versions.yaml`, `auth-manifest.yaml`, `auth-openapi.yaml`, `manifest.schema.json` (초안) 생성이 완료됨.
- 후속 Backend, Frontend, Verify 작업자가 도구 버전, 보안 정책, API 스펙에 대해 추가적인 설계 결정 없이 즉시 코드를 작성할 수 있음.