# Auth OAuth Expansion — Google, Naver, Kakao SNS 로그인

## TL;DR

> **Quick Summary**: Email-only auth 템플릿을 Google, Naver, Kakao OAuth 로그인으로 확장한다. 각 provider별 ASVS 검증 항목 + OpenAPI 계약 + 구현 + 테스트를 포함한다.
>
> **Deliverables**:
> - OAuth 엔드포인트 3개 provider (Google, Naver, Kakao)
> - Provider linking/unlinking (이메일 계정에 SNS 연결)
> - ASVS OAuth 검증 테스트
> - 업데이트된 spec/contract/manifest
>
> **Estimated Effort**: Large (2-3d)
> **Parallel Execution**: YES — 3 waves
> **Critical Path**: Spec update → OAuth infra → Provider 구현 → 통합 테스트

---

## Context

### Current State
- Email auth 완료: signup, login, verify, refresh, logout, /me, password-reset/change
- 23 ASVS L1 항목 전부 통과
- Self-validation 완료: 피드백 루프 동작 증명
- GAP-REPORT 보강 완료: JWT, 세션, CORS, 테스트시딩 정책 추가

### What's Missing
- OAuth2 Authorization Code Flow (Google, Naver, Kakao)
- Provider linking: 기존 email 계정에 SNS 계정 연결
- SNS 전용 ASVS 검증 항목

---

## Work Objectives

### Core Objective
3개 SNS provider (Google, Naver, Kakao) OAuth2 로그인을 TDD로 구현하고, 모든 검증 항목이 `./gradlew testAsvs` 한 줄로 검증되게 만든다.

### Must Have
- OAuth2 Authorization Code Flow (all 3 providers)
- Provider config를 application.yml에서 관리 (client-id, client-secret 환경변수)
- 기존 email 계정에 SNS provider 연결/해제
- /auth/me에 linked providers 표시
- Spring Security OAuth2 Client 사용 (직접 HTTP 구현 금지)
- ASVS OAuth 관련 테스트

### Must NOT Have
- 실제 OAuth provider 연동 (테스트에서는 mock)
- MFA
- 회원 탈퇴
- 프론트엔드 변경 (백엔드만)
- 별도 user profile 관리

---

## TODOs

- [x] 1. Spec/Contract/Manifest 업데이트 — OAuth 엔드포인트 + provider 정책 추가

  **What to do**:
  - `contracts/auth-openapi.yaml`에 OAuth 엔드포인트 추가:
    - `GET /auth/oauth/{provider}/authorize` — OAuth 인가 URL 리다이렉트
    - `GET /auth/oauth/{provider}/callback` — OAuth 콜백 처리 (code→token→user)
    - `POST /auth/oauth/link` — 기존 계정에 SNS 연결 (Bearer JWT 필요)
    - `DELETE /auth/oauth/unlink/{provider}` — SNS 연결 해제 (Bearer JWT 필요)
  - `blueprints/auth-manifest.yaml` 업데이트:
    - `provider_policy.enabled_candidates: [email, google, naver, kakao]`
    - 각 provider별 config: client_id source, user_info endpoint, scope
  - `specs/auth-asvs-l1.yaml`에 OAuth 관련 ASVS 항목 추가:
    - `ASVS-V2.8.1` — OAuth state parameter CSRF 방지
    - `ASVS-V2.8.2` — OAuth 리다이렉트 URL 검증
    - `ASVS-V2.8.3` — OAuth token 안전 저장 (client_secret 노출 금지)
  - YAML 유효성 검증
  **Must NOT do**: 백엔드 코드 변경 금지. spec 파일만.

  **Recommended Agent Profile**: `quick`
  **Parallelization**: Wave 1 | Blocks: 2,3 | Blocked By: —

  **Acceptance Criteria**:
  - [ ] OpenAPI에 4개 OAuth 엔드포인트 존재
  - [ ] Manifest에 google/naver/kakao provider 정책 존재
  - [ ] ASVS spec에 3개 OAuth 항목 추가
  - [ ] YAML 문법 유효

  **Commit**: `docs(specs): add OAuth endpoints and ASVS items for Google/Naver/Kakao`

- [x] 2. OAuth 인프라 — Spring Security OAuth2 Client 설정 + ProviderUser 엔티티

  **What to do**:
  - `build.gradle.kts`에 `spring-boot-starter-oauth2-client` 추가
  - `application.yml`에 3개 provider 등록 (client-id/secret은 환경변수):
    ```yaml
    spring.security.oauth2.client.registration:
      google:
        client-id: ${GOOGLE_CLIENT_ID:dummy}
        client-secret: ${GOOGLE_CLIENT_SECRET:dummy}
        scope: openid,email,profile
      naver:
        client-id: ${NAVER_CLIENT_ID:dummy}
        client-secret: ${NAVER_CLIENT_SECRET:dummy}
        authorization-grant-type: authorization_code
        redirect-uri: "{baseUrl}/api/auth/oauth/naver/callback"
        client-name: Naver
      kakao:
        client-id: ${KAKAO_CLIENT_ID:dummy}
        client-secret: ${KAKAO_CLIENT_SECRET:dummy}
        authorization-grant-type: authorization_code
        redirect-uri: "{baseUrl}/api/auth/oauth/kakao/callback"
        client-name: Kakao
    ```
  - Naver/Kakao provider 정보 수동 등록 (Spring Boot에 기본 미포함):
    ```yaml
    spring.security.oauth2.client.provider:
      naver:
        authorization-uri: https://nid.naver.com/oauth2.0/authorize
        token-uri: https://nid.naver.com/oauth2.0/token
        user-info-uri: https://openapi.naver.com/v1/nid/me
        user-name-attribute: response
      kakao:
        authorization-uri: https://kauth.kakao.com/oauth/authorize
        token-uri: https://kauth.kakao.com/oauth/token
        user-info-uri: https://kapi.kakao.com/v2/user/me
        user-name-attribute: id
    ```
  - `ProviderLink` 엔티티: userId (FK), provider (GOOGLE/NAVER/KAKAO), providerUserId, email, linkedAt
  - `ProviderLinkRepository`: findByUserAndProvider, existsByProviderAndProviderUserId
  - SecurityConfig에 OAuth2 login 추가
  - `./gradlew build` 성공 확인
  **Must NOT do**: 비즈니스 로직 구현 금지. 인프라만.

  **Recommended Agent Profile**: `unspecified-high`
  **Parallelization**: Wave 2 | Blocks: 3 | Blocked By: 1

  **Acceptance Criteria**:
  - [ ] `./gradlew build` exits 0
  - [ ] ProviderLink 엔티티 + Repository 존재
  - [ ] application.yml에 3개 provider 등록
  - [ ] build.gradle.kts에 oauth2-client 의존성 존재

  **Commit**: `build(auth): add OAuth2 Client deps, provider config, ProviderLink entity`

- [x] 3. TDD: OAuth 콜백 + 로그인 + 연결/해제

  **What to do**:
  - **RED**: ASVS 테스트 먼저:
    - `@Tag("ASVS-V2.8.1")` OAuth state parameter 검증
    - `@Tag("ASVS-V2.8.2")` 리다이렉트 URL 검증
    - `@Tag("ASVS-V2.8.3")` client_secret 노출 금지
    - OAuth 로그인 성공 시 JWT 발급 테스트
    - Provider link/unlink 테스트
  - **GREEN**: OAuthController, OAuthService 구현
    - `GET /api/auth/oauth/{provider}/authorize` — 인가 URL로 리다이렉트
    - `GET /api/auth/oauth/{provider}/callback` — code→token 교환, 신규/기존 유저 처리, JWT 발급
    - `POST /api/auth/oauth/link` — 인증된 유저에 SNS 연결
    - `DELETE /api/auth/oauth/unlink/{provider}` — SNS 연결 해제
  - OAuth provider는 테스트에서 WireMock으로 모킹
  - /auth/me 응답에 linkedProviders 추가
  **Must NOT do**: 실제 OAuth provider 호출 (테스트에서는 WireMock mock)

  **Recommended Agent Profile**: `deep`
  **Parallelization**: Wave 2 | Blocks: 4 | Blocked By: 2

  **Acceptance Criteria**:
  - [ ] 4개 OAuth 엔드포인트 동작
  - [ ] 3개 ASVS OAuth 테스트 통과
  - [ ] Provider link/unlink 동작
  - [ ] /auth/me에 linkedProviders 포함
  - [ ] `./gradlew testAsvs` exits 0 (기존 23개 + 새로운 3개 = 26개)

  **Commit**: `feat(auth): OAuth login + link/unlink for Google/Naver/Kakao [ASVS V2.8.x]`

- [x] 4. E2E + ASVS 리포트 업데이트

  **What to do**:
  - E2E: OAuth 로그인 → /auth/me에 provider 표시 → unlink → /auth/me에서 제거
  - `specs/auth-asvs-l1.yaml`에 OAuth 항목 추가된 상태에서 compliance report 재생성
  - `specs/auth-asvs-l1-report.md` 업데이트 (26개 항목)
  **Must NOT do**: 프론트엔드 변경 금지

  **Recommended Agent Profile**: `deep`
  **Parallelization**: Wave 3 | Blocks: F1 | Blocked By: 3

  **Acceptance Criteria**:
  - [ ] E2E OAuth 테스트 통과
  - [ ] ASVS report: 26/26 covered
  - [ ] `./gradlew testAsvs` exits 0

  **Commit**: `test: OAuth E2E + ASVS report update (26 items)`

---

## Final Verification Wave

- [x] F1. **Full ASVS check** — `./gradlew testAsvs` → 26개 항목 전부 PASS
- [x] F2. **Scope check** — OAuth 엔드포인트 4개 + 기존 10개 = 14개 계약 일치

---

## Success Criteria
```bash
cd backend && ./gradlew testAsvs   # Expected: 26 ASVS tests pass
cd backend && ./gradlew test       # Expected: all tests pass
```
