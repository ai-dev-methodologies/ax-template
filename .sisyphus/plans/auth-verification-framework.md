# Auth Verification Framework — Email Vertical Slice

## TL;DR

> **Quick Summary**: 거버넌스 무한루프를 끊고, OWASP ASVS L1 기준의 기계적 검증 체계 + 실제 동작하는 Email Auth 구현을 TDD로 만든다. 검증 항목마다 JUnit `@Tag("ASVS-V2.x.x")` 테스트가 pass/fail 판정.
>
> **Deliverables**:
> - ASVS L1 검증 항목 YAML (pinned, ~23 items)
> - Spring Boot Email Auth 전체 엔드포인트 (signup, login, verify, refresh, logout, /me, password-reset)
> - ASVS 태그 JUnit 테스트 전체 통과
> - React 최소 Auth UI (signup, login, verify, dashboard-stub)
> - E2E smoke test (full user journey)
>
> **Estimated Effort**: Large (3-5d)
> **Parallel Execution**: YES — 4 waves, backend/frontend 병렬
> **Critical Path**: Cleanup → Bootstrap → Signup endpoint → Login → Verify → Refresh → Logout → /me → E2E

---

## Context

### Original Request
기존 30+ 문서와 18 세션을 소비했지만 동작하는 코드 0줄인 프로젝트를 구제. 규칙을 정의하고, 그 규칙대로 구현하고, 그 규칙으로 기계적 검증하는 프로세스를 만든다.

### Interview Summary
- **템플릿 범위**: auth만 우선. 구조 검증 후 확장
- **OWASP 수준**: ASVS Level 1 — 기본부터 시작
- **기술 스택**: Spring Boot 3.2.x + React 고정
- **규칙 포맷**: 실행 가능한 테스트 코드 (JUnit @Tag) + YAML 스펙 문서 교차검증
- **워크플로우**: 규칙 정의 → TDD 구현 → 기계적 검증 → 피드백 루프
- **기존 자산**: OpenAPI contract + auth-manifest 유지, 거버넌스 전부 archive

### Metis Review (gaps addressed)
- **DB 없음**: build.gradle.kts에 JPA/H2 의존성 0개. → H2 + Testcontainers 추가
- **OpenAPI-ASVS 충돌**: password minLength 8→12, maxLength 미정의, password-reset 엔드포인트 누락 → 계약 먼저 수정
- **CSRF+SPA 충돌**: Spring CSRF 기본설정이 React POST를 전부 차단 → CookieCsrfTokenRepository 적용
- **Frontend 빌드 불가**: Vite/빌드도구 없음 → 부트스트랩에서 해결
- **공통 규칙 계층 시기상조**: 템플릿 1개만 존재 → 추상화 DEFER, auth 구체 테스트만 작성
- **Placeholder 코드**: 전부 삭제 후 재작성 (리팩토링 X)

---

## Work Objectives

### Core Objective
OWASP ASVS L1 기준의 Email Auth를 TDD로 구현하고, 모든 검증 항목이 `./gradlew test --tests "*ASVS*"` 한 줄로 pass/fail 판정되게 만든다.

### Concrete Deliverables
- `specs/auth-asvs-l1.yaml` — 적용 가능한 ASVS 항목 pinned 목록
- Backend: 7개 엔드포인트 전체 구현 (signup, login, verify-email, refresh, logout, /me, password-reset)
- Backend: ASVS 항목당 1개 JUnit 테스트 (`@Tag("ASVS-Vx.x.x")`)
- Frontend: Auth store + API client + 4개 페이지 (signup, login, verify, dashboard-stub)
- E2E: signup→verify→login→me→logout 전체 경로 smoke test
- ASVS compliance summary (자동 생성)

### Definition of Done
- [ ] `cd backend && ./gradlew test` exits 0 — 모든 ASVS 태그 테스트 통과
- [ ] `cd frontend && npm run test` exits 0 — frontend 테스트 통과
- [ ] `cd frontend && npm run build` exits 0 — frontend 빌드 성공
- [ ] E2E smoke test: signup→verify→login→/me→logout 전체 경로 통과 (토큰 자동 추출)
- [ ] `cd backend && ./gradlew testAsvs` — ASVS 태그 테스트만 실행 가능 (또는 `./gradlew test --tests "*asvs_*"`)
- [ ] `curl` 기반 security header 검증 통과 (HttpOnly, Secure, SameSite)

### Must Have
- TDD: 실패하는 테스트 먼저 → 구현 → 통과 (RED→GREEN→REFACTOR)
- ASVS 항목별 @Tag 테스트 — 기계적 검증의 핵심
- OpenAPI 계약과 구현의 일치 — 계약이 source of truth
- 실제 Spring Security 필터 체인 통과 테스트 — MockMvc with real filters

### Must NOT Have (Guardrails)
- **G1**: ZERO 거버넌스 문서 생성 — docs/governance/에 새 파일 금지
- **G2**: `@WithMockUser` 또는 `addFilters=false` 사용 금지 — 실제 시큐리티 파이프라인 테스트
- **G3**: "검증 프레임워크" 추상화 금지 — 구체적 JUnit 테스트만
- **G4**: Placeholder 리팩토링 금지 — 삭제 후 재작성
- **G5**: OAuth/소셜 로그인, 계정 연결, MFA 구현 금지 — Email만
- **G6**: CI/CD 파이프라인 구축 금지 — 로컬 검증만
- **G7**: `assertTrue(true)` 같은 빈 assertion 금지
- **G8**: 별도 Python verification 스크립트 금지 — JUnit/Vitest가 검증 수단

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — 모든 검증은 테스트 코드로 자동 판정

### Test Decision
- **Infrastructure**: 신규 구축 (기존 placeholder 전부 교체)
- **Automated tests**: TDD (RED→GREEN→REFACTOR)
- **Backend framework**: JUnit 5 + MockMvc + Testcontainers
- **Frontend framework**: Vitest + Testing Library + MSW

### ASVS Verification Method
- 각 ASVS 항목 = 1개 `@Tag("ASVS")` + `@Tag("ASVS-Vx.x.x")` JUnit 테스트
- 테스트 명명: `asvs_V2_1_1_passwordMinLength12_rejectsShorter()` — 반드시 `asvs_` prefix
- **Gradle tag 실행**: `build.gradle.kts`에 별도 task 등록:
  ```kotlin
  tasks.register<Test>("testAsvs") {
      useJUnitPlatform { includeTags("ASVS") }
  }
  ```
  실행: `cd backend && ./gradlew testAsvs`
- **이름 기반 실행** (fallback): `cd backend && ./gradlew test --tests "*asvs_*"` (method name prefix 매칭)
- 교차검증: `specs/auth-asvs-l1.yaml`의 항목 ID와 테스트 @Tag 1:1 대응 확인
- **주의**: Task 4 (backend bootstrap)에서 `testAsvs` Gradle task를 반드시 등록할 것

### QA Policy
- Backend: MockMvc 테스트가 실제 SecurityFilterChain 통과
- Frontend: MSW mock → OpenAPI 계약 기반 응답
- E2E: full user journey smoke test
- Evidence: 테스트 실행 로그 자체가 evidence

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately — cleanup + specs):
├── Task 1: Archive governance + delete placeholders [quick]
├── Task 2: Fix OpenAPI contract for ASVS alignment [quick]
└── Task 3: Create ASVS verification checklist YAML [quick]

Wave 2 (After Wave 1 — bootstrap both stacks):
├── Task 4: Backend bootstrap (deps + compile + health check) [unspecified-high]
└── Task 5: Frontend bootstrap (Vite + deps + compile) [visual-engineering]

Wave 3 (After Wave 2 — backend TDD + frontend parallel):
│
│ Backend Track (sequential):
├── Task 6: TDD: User entity + password hashing + repository [deep]
├── Task 7: TDD: Signup endpoint [ASVS V2.1.x] [deep]
├── Task 8: TDD: Login endpoint + rate limiting [ASVS V2.2.x] [deep]
├── Task 9: TDD: Email verification flow [deep]
├── Task 10: TDD: Password reset flow [deep]
├── Task 11: TDD: Refresh token rotation [ASVS V3.x] [deep]
├── Task 12: TDD: Logout + session invalidation [ASVS V3.3.1] [deep]
├── Task 13: TDD: /auth/me + access control [ASVS V4.x] [deep]
├── Task 14: TDD: Security baseline — cookie flags, CSRF, CORS [ASVS V3.4.x] [deep]
│
│ Frontend Track (parallel, after Task 2 contract fixed):
├── Task 15: Frontend auth store + API client + MSW mocks [visual-engineering]
├── Task 16: Frontend auth pages (signup, login, verify, dashboard) [visual-engineering]
└── Task 17: Frontend Vitest tests [visual-engineering]

Wave 4 (After ALL — integration + summary):
├── Task 18: E2E integration smoke test [deep]
└── Task 19: ASVS compliance summary generation [quick]

Wave FINAL (After ALL tasks — 4 parallel reviews, then user okay):
├── Task F1: ASVS compliance audit (oracle)
├── Task F2: Code quality review (unspecified-high)
├── Task F3: Security baseline QA (unspecified-high)
└── Task F4: Scope fidelity check (deep)
-> Present results -> Get explicit user okay
```

### Dependency Matrix

| Task | Depends On | Blocks | Wave |
|------|-----------|--------|------|
| 1 | — | 4,5 | 1 |
| 2 | — | 4,5,7-14,15 | 1 |
| 3 | — | 7-14 | 1 |
| 4 | 1,2 | 6-14 | 2 |
| 5 | 1,2 | 15-17 | 2 |
| 6 | 4 | 7-14 | 3 |
| 7 | 3,6 | 8 | 3 |
| 8 | 7 | 9 | 3 |
| 9 | 7 | 10 | 3 |
| 10 | 7 | 18 | 3 |
| 11 | 8 | 12 | 3 |
| 12 | 8 | 18 | 3 |
| 13 | 8 | 18 | 3 |
| 14 | 6,8 | 18 | 3 |
| 15 | 2,5 | 16 | 3 |
| 16 | 15 | 17 | 3 |
| 17 | 16 | 18 | 3 |
| 18 | 7,8,9,10,11,12,13,14,17 | 19 | 4 |
| 19 | 18 | F1-F4 | 4 |

### Agent Dispatch Summary

- **Wave 1**: **3** — T1→`quick`, T2→`quick`, T3→`quick`
- **Wave 2**: **2** — T4→`unspecified-high`, T5→`visual-engineering`
- **Wave 3 Backend**: **9** — T6-T14→`deep` (sequential)
- **Wave 3 Frontend**: **3** — T15-T17→`visual-engineering`
- **Wave 4**: **2** — T18→`deep`, T19→`quick`
- **FINAL**: **4** — F1→`oracle`, F2→`unspecified-high`, F3→`unspecified-high`, F4→`deep`

---

## Auto-Resolved Decisions

| Decision | Default Applied | Rationale |
|----------|----------------|-----------|
| Database | H2 in-memory (dev/test) + Testcontainers PostgreSQL (integration) | Spring boring default |
| Email | Console-logged tokens for v1 | 실제 SMTP 불필요 |
| Password hashing | bcrypt via Spring Security | ASVS V2.4.1 compliant default |
| JWT signing | In-memory generated RSA key (dev) | 환경변수 기반은 deploy 시점 |
| Archive method | `git mv docs/governance/* docs/archive/governance/` | git history 보존 |
| Refresh token grace window | 30 seconds | concurrent tab 처리 |
| Email verification token expiry | 24 hours | 표준 관행 |
| Rate limit storage | In-memory (Spring Security default) | Redis는 v2 |
| "Common rules" layer | DEFER — 두 번째 템플릿 등장 시 추출 | 시기상조 추상화 방지 |
| CSRF strategy | **Stateless API 엔드포인트(`/api/auth/**`)는 CSRF 비활성화** (JWT Bearer 토큰 기반이므로 CSRF 불필요). Cookie-only 세션 엔드포인트가 있는 경우에만 CookieCsrfTokenRepository 적용. Spring Security에서 `csrf.ignoringRequestMatchers("/api/**")` 설정. | JWT + SPA 표준 패턴 |

---

## TODOs

- [x] 1. Archive governance docs + delete placeholder files

  **What to do**:
  - `git mv docs/governance/* docs/archive/governance/`
  - `git mv docs/TEMPLATE-GOVERNANCE.md docs/archive/`
  - `git mv ACTIVE-LOOP.md docs/archive/`
  - backend/src/ 내 모든 `*Placeholder*.java`, `*Skeleton*.java` 파일 삭제
  - frontend/src/ 내 빈 barrel export 파일들 삭제
  - frontend/tests/ 내 기존 placeholder 테스트 삭제
  - verify/scripts/ 내 placeholder 스크립트 삭제 (manifest.schema.json은 유지)
  **Must NOT do**: contracts/, blueprints/, verify/manifest.schema.json 삭제 금지. docs/designs/는 참고용으로 유지.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`git-master`] — git mv로 history 보존 이동

  **Parallelization**: Wave 1 | Blocks: 4,5 | Blocked By: —

  **References**:
  - `docs/governance/` — archive 대상 디렉토리
  - `docs/TEMPLATE-GOVERNANCE.md` — 391줄 거버넌스 문서
  - `backend/src/main/java/com/ax/template/authblueprint/` — Placeholder 파일들
  - `frontend/tests/` — placeholder 테스트들

  **Acceptance Criteria**:
  - [ ] `ls docs/governance/` → 빈 디렉토리 또는 삭제됨
  - [ ] `find backend/src -name "*Placeholder*" -o -name "*Skeleton*"` → 결과 0건
  - [ ] `docs/archive/` 아래에 이동된 파일들 존재
  - [ ] contracts/, blueprints/ 그대로 존재

  **QA Scenarios**:
  ```
  Scenario: Governance files archived
    Tool: Bash
    Steps:
      1. ls docs/archive/governance/ — 이동된 파일 존재 확인
      2. ls docs/governance/ — 빈 디렉토리 확인
      3. cat contracts/auth-openapi.yaml | head -1 — 파일 건재 확인
    Expected Result: archive에 파일 존재, governance 비어있음, contracts 건재
    Evidence: .sisyphus/evidence/task-1-archive-cleanup.txt

  Scenario: No placeholder files remain
    Tool: Bash
    Steps:
      1. find backend/src -name "*Placeholder*" -o -name "*Skeleton*" | wc -l
      2. find frontend/tests -name "*.placeholder.*" | wc -l
    Expected Result: 두 명령 모두 0 출력
    Evidence: .sisyphus/evidence/task-1-placeholder-removal.txt
  ```

  **Commit**: YES | Message: `chore: archive governance docs, delete placeholder files` | Files: docs/archive/**, backend/src/**, frontend/**

- [x] 2. Fix OpenAPI contract for ASVS alignment

  **What to do**:
  - `contracts/auth-openapi.yaml` 수정:
    - password minLength: 8 → 12
    - password maxLength: 128 추가
    - `POST /auth/email/password-reset-request` 엔드포인트 추가
    - `POST /auth/email/password-reset` 엔드포인트 추가
    - `POST /auth/email/password-change` 엔드포인트 추가 (현재 비밀번호 + 새 비밀번호 필수)
    - `GET /auth/verify-email` → `POST /api/auth/email/verify-email`로 변경 (token을 body로 전달)
    - `/auth/link-account` 엔드포인트 제거 (scope OUT — OAuth 없음)
    - 에러 응답에 generic message 패턴 반영 (account enumeration 방지)
    - **Base path 정리**: 현재 OpenAPI의 `servers` 값이 `/api`이므로, 엔드포인트 path는 서버 base 없이 작성. 즉 실제 URL = `servers` + `path`.
      - Email 인증 엔드포인트: `/auth/email/signup`, `/auth/email/login`, `/auth/email/verify-email`, `/auth/email/password-reset-request`, `/auth/email/password-reset`, `/auth/email/password-change`, `/auth/email/resend-verification`
      - 세션 엔드포인트 (email에 종속되지 않음): `/auth/refresh`, `/auth/logout`, `/auth/me`
      - 실제 URL 예시: `http://localhost:8080/api/auth/email/signup`, `http://localhost:8080/api/auth/refresh`
  - `blueprints/auth-manifest.yaml` 업데이트:
    - password policy 반영 (minLength 12, maxLength 128)
    - **providers를 email-only로 축소** (google, kakao 제거 또는 `enabled: false`로 변경)
    - **account linking 제거** (scope OUT)
    - manifest와 OpenAPI 계약이 동일한 email-only scope를 반영하도록 통일
  **Must NOT do**: OAuth/소셜 관련 엔드포인트 추가 금지.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**: Wave 1 | Blocks: 4,5,7-14,15 | Blocked By: —

  **References**:
  - `contracts/auth-openapi.yaml` — 현재 354줄, password minLength:8이 ASVS와 충돌
  - OWASP ASVS V2.1.1 (min 12), V2.1.2 (support ≥64, deny >128)
  - OWASP ASVS V2.1.6 (password change requires current+new)
  - `blueprints/auth-manifest.yaml` — password policy 섹션

  **Acceptance Criteria**:
  - [ ] password minLength가 12로 변경됨
  - [ ] password maxLength가 128로 설정됨
  - [ ] password-reset-request, password-reset, password-change 엔드포인트 존재
  - [ ] verify-email이 POST 메서드로 변경됨
  - [ ] `/auth/link-account` 엔드포인트 제거됨
  - [ ] OpenAPI path: email 엔드포인트는 `/auth/email/*`, 세션 엔드포인트는 `/auth/*` (refresh, logout, me). servers는 `/api`
  - [ ] OpenAPI YAML이 유효한 문법

  **QA Scenarios**:
  ```
  Scenario: ASVS-aligned password rules
    Tool: Bash
    Steps:
      1. grep "minLength" contracts/auth-openapi.yaml — 12 확인
      2. grep "maxLength" contracts/auth-openapi.yaml — 128 확인
      3. grep "password-reset" contracts/auth-openapi.yaml — 엔드포인트 존재
      4. grep -c "link-account" contracts/auth-openapi.yaml — 0 확인 (제거됨)
      5. grep "verify-email" contracts/auth-openapi.yaml — POST 메서드 확인
    Expected Result: minLength: 12, maxLength: 128, password-reset 3개 경로 존재, link-account 제거, verify-email POST
    Evidence: .sisyphus/evidence/task-2-openapi-asvs.txt

  Scenario: OpenAPI valid syntax
    Tool: Bash
    Steps:
      1. python3 -c "import yaml; yaml.safe_load(open('contracts/auth-openapi.yaml'))"
    Expected Result: 문법 오류 0건
    Evidence: .sisyphus/evidence/task-2-openapi-valid.txt
  ```

  **Commit**: YES | Message: `fix(contract): align OpenAPI with ASVS L1 (password rules, add reset endpoint)` | Files: contracts/auth-openapi.yaml, blueprints/auth-manifest.yaml

- [x] 3. Create ASVS L1 verification checklist YAML

  **What to do**:
  - `specs/auth-asvs-l1.yaml` 생성 — email vertical slice에 적용 가능한 ASVS 항목만 pinned
  - 각 항목: ID, 요구사항 텍스트, 테스트 메서드명, 검증 방법(API test/integration test/static analysis), 적용 여부
  - **ID 포맷 규칙 (CRITICAL)**: `id` 값은 반드시 `ASVS-V{x}.{y}.{z}` 형식 (예: `ASVS-V2.1.1`). 이 값은 JUnit `@Tag("ASVS-V2.1.1")`과 정확히 일치해야 함. Task 19/F1의 diff 검증이 이 일치에 의존.
  - Email-only에 N/A인 항목(MFA, 생체인식, SSO)은 `applicable: false`로 명시
  - `specs/auth-asvs-l1.docs.md` — 사람이 읽을 수 있는 교차검증 문서
  **Must NOT do**: 검증 프레임워크/엔진 코드 작성 금지. YAML 정의만.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**: Wave 1 | Blocks: 7-14 | Blocked By: —

  **References**:
  - OWASP ASVS v4.0.3: V2 (13항목), V3 (8항목), V4 (5항목), CheatSheet (3항목)
  - Email-only slice에 N/A: ~6-8 items (MFA, biometric, SSO)
  - 실제 적용 대상: ~~23 items

  **Acceptance Criteria**:
  - [ ] `specs/auth-asvs-l1.yaml` 파일 존재
  - [ ] 모든 항목에 `id`, `requirement`, `test_method`, `verification_type`, `applicable` 필드
  - [ ] `applicable: true` 항목이 ~23개 (Tasks 7-14에서 정의한 @Tag 수와 정확히 일치해야 함)
  - [ ] `specs/auth-asvs-l1.docs.md` 교차검증 문서 존재

  **QA Scenarios**:
  ```
  Scenario: ASVS spec completeness
    Tool: Bash
    Steps:
      1. python3 -c "import yaml; d=yaml.safe_load(open('specs/auth-asvs-l1.yaml')); print(len([i for i in d['items'] if i['applicable']]))"
    Expected Result: ~23 출력 (Tasks 7-14의 @Tag 수와 일치)
    Evidence: .sisyphus/evidence/task-3-asvs-spec.txt

  Scenario: All required fields present
    Tool: Bash
    Steps:
      1. python3 -c "import yaml; d=yaml.safe_load(open('specs/auth-asvs-l1.yaml')); missing=[i['id'] for i in d['items'] if not all(k in i for k in ['id','requirement','test_method','verification_type','applicable'])]; print(f'Missing fields: {len(missing)}'); [print(f'  {m}') for m in missing]"
    Expected Result: "Missing fields: 0" 출력
    Evidence: .sisyphus/evidence/task-3-asvs-fields.txt
  ```

  **Commit**: YES | Message: `docs(specs): pin ASVS L1 verification items for email auth` | Files: specs/auth-asvs-l1.yaml, specs/auth-asvs-l1.docs.md

- [x] 4. Backend bootstrap — dependencies + compile + health check

  **What to do**:
  - **Gradle wrapper 생성**: `backend/` 디렉토리에서 `gradle wrapper --gradle-version 8.5` 실행하여 `backend/gradlew`, `backend/gradlew.bat`, `backend/gradle/wrapper/` 생성 (현재 repo에 wrapper 없음). **모든 후속 Task의 `./gradlew` 명령은 `backend/` 디렉토리에서 실행한다.**
  - `build.gradle.kts`에 의존성 추가: spring-boot-starter-data-jpa, H2, spring-boot-starter-validation, **spring-boot-starter-actuator**, jjwt (또는 spring-security-oauth2-jose), spring-boot-starter-test, testcontainers
  - `application.yaml` 설정: H2 콘솔, JPA ddl-auto, JWT 설정, `management.endpoints.web.exposure.include=health,mappings` (F4 검증용)
  - `build.gradle.kts`에 `testAsvs` Gradle task 등록: `tasks.register<Test>("testAsvs") { useJUnitPlatform { includeTags("ASVS") } }`
  - 기존 SecurityConfig 수정: CookieCsrfTokenRepository.withHttpOnlyFalse() 적용
  - `./gradlew build` 성공 확인
  - health check endpoint (`/actuator/health`) 응답 확인
  **Must NOT do**: 비즈니스 로직 구현 금지. 컴파일+부팅만 확인.
  **Prerequisites**: system에 Gradle이 설치되어 있어야 wrapper 생성 가능. 없으면 `brew install gradle` 먼저 실행.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**: Wave 2 | Blocks: 6-14 | Blocked By: 1,2

  **References**:
  - `backend/build.gradle.kts` — 현재 JPA/H2 의존성 없음
  - `backend/src/main/java/.../SecurityConfig.java` — CSRF 설정 수정 필요
  - Metis 지적: CSRF 기본설정이 React POST 전부 차단

  **Acceptance Criteria**:
  - [ ] `backend/gradlew` 파일 존재 + 실행 권한 (`chmod +x`)
  - [ ] `cd backend && ./gradlew build` exits 0
  - [ ] `cd backend && ./gradlew bootRun` 후 `curl localhost:8080/actuator/health` → `{"status":"UP"}`
  - [ ] build.gradle.kts에 JPA, H2, Testcontainers, **actuator** 의존성 존재

  **QA Scenarios**:
  ```
  Scenario: Backend compiles and boots
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew build
      2. cd backend && ./gradlew bootRun &
      3. sleep 15 && curl -s localhost:8080/actuator/health | jq .status
      4. kill %1
    Expected Result: build 성공, health check "UP"
    Evidence: .sisyphus/evidence/task-4-backend-bootstrap.txt

  Scenario: Required dependencies present
    Tool: Bash
    Steps:
      1. grep "spring-boot-starter-data-jpa" backend/build.gradle.kts
      2. grep "h2" backend/build.gradle.kts
      3. grep "testcontainers" backend/build.gradle.kts
      4. grep "actuator" backend/build.gradle.kts
    Expected Result: 4개 모두 존재
    Evidence: .sisyphus/evidence/task-4-deps-check.txt
  ```

  **Commit**: YES | Message: `build(backend): add JPA, H2, Testcontainers, fix CSRF for SPA` | Files: backend/build.gradle.kts, backend/src/main/resources/application.yaml, backend/src/**/SecurityConfig.java

- [x] 5. Frontend bootstrap — Vite + deps + compile

  **What to do**:
  - `frontend/package.json`에 의존성 추가: vite, @vitejs/plugin-react, typescript, @testing-library/react, @testing-library/jest-dom, msw
  - `frontend/vite.config.ts` 생성 (React plugin, proxy to backend)
  - `frontend/tsconfig.json` 생성/수정
  - **Vite entry files 생성** (현재 repo에 없음):
    - `frontend/index.html` — Vite entry HTML (`<div id="root">` + `<script type="module" src="/src/main.tsx">`)
    - `frontend/src/main.tsx` — React root render (`createRoot(document.getElementById('root'))`)
    - `frontend/src/App.tsx` — 최소 App 컴포넌트 (빈 router placeholder)
  - `npm run build` 스크립트 추가
  - `npm run dev` 스크립트 추가
  - `npm run build` 성공 확인
  **Must NOT do**: 페이지/컴포넌트 구현 금지. 빌드 가능한 최소 entry만 생성. 기존 vitest.config.ts는 유지/업데이트.

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: [`vercel-react-best-practices`]

  **Parallelization**: Wave 2 | Blocks: 15-17 | Blocked By: 1,2

  **References**:
  - `frontend/package.json` — 현재 Vite 의존성 없음, 빌드 불가
  - `frontend/vitest.config.ts` — 기존 vitest 설정 유지
  - Metis 지적: frontend 빌드 도구 완전 부재

  **Acceptance Criteria**:
  - [ ] `npm --prefix frontend run build` exits 0
  - [ ] `npm --prefix frontend run dev` 로 dev server 기동 가능
  - [ ] package.json에 vite, @vitejs/plugin-react 존재

  **QA Scenarios**:
  ```
  Scenario: Frontend builds successfully
    Tool: Bash
    Steps:
      1. cd frontend && npm install && npm run build
    Expected Result: exit 0, dist/ 디렉토리 생성
    Evidence: .sisyphus/evidence/task-5-frontend-build.txt

  Scenario: Dev server starts
    Tool: Bash
    Steps:
      1. cd frontend && npm run dev &
      2. sleep 5 && curl -s localhost:5173 | head -5
      3. kill %1
    Expected Result: HTML 응답 수신
    Evidence: .sisyphus/evidence/task-5-frontend-dev.txt
  ```

  **Commit**: YES | Message: `build(frontend): add Vite, testing-library, MSW, tsconfig` | Files: frontend/package.json, frontend/vite.config.ts, frontend/tsconfig.json

- [x] 6. TDD: User entity + password hashing + repository

  **What to do**:
  - **RED**: `UserEntity` 생성 테스트 작성 — email, hashedPassword, role, emailVerified, createdAt 필드 검증
  - **RED**: bcrypt 해싱 테스트 — raw password → hash → verify roundtrip
  - **GREEN**: `UserEntity` JPA 엔티티 구현, `UserRepository` 인터페이스, `PasswordEncoder` Bean 등록
  - **REFACTOR**: 불필요한 코드 제거
  - H2 in-memory DB로 테스트 실행
  **Must NOT do**: 컨트롤러/서비스 레이어 구현 금지. 엔티티+리포지토리만.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 3 Backend | Blocks: 7-14 | Blocked By: 4

  **References**:
  - `contracts/auth-openapi.yaml` — User 스키마 정의
  - `blueprints/auth-manifest.yaml` — password policy, token strategy
  - ASVS V2.4.1: bcrypt/Argon2id 사용 요구

  **Acceptance Criteria**:
  - [ ] `UserEntity` JPA 엔티티 존재, 필수 필드 포함
  - [ ] `UserRepository` extends JpaRepository 존재
  - [ ] bcrypt PasswordEncoder Bean 등록
  - [ ] `./gradlew test --tests "*User*"` 통과

  **QA Scenarios**:
  ```
  Scenario: User entity persistence
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*UserEntityTest*"
    Expected Result: 테스트 통과 — user 저장 후 조회 시 필드 일치
    Evidence: .sisyphus/evidence/task-6-user-entity.txt

  Scenario: Password hashing roundtrip
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*PasswordEncoder*"
    Expected Result: raw → hash → matches(raw, hash) == true
    Evidence: .sisyphus/evidence/task-6-password-hash.txt
  ```

  **Commit**: YES | Message: `feat(auth): User entity, repository, bcrypt password encoder` | Files: backend/src/main/java/**/User*.java, backend/src/test/java/**/User*.java

- [x] 7. TDD: Signup endpoint with ASVS password validation

  **What to do**:
  - **RED**: ASVS 테스트 먼저:
    - `@Tag("ASVS-V2.1.1")` asvs_V2_1_1_passwordMinLength12_rejectsShorter()
    - `@Tag("ASVS-V2.1.2")` asvs_V2_1_2_password64CharsAllowed_129Rejected()
    - `@Tag("ASVS-V2.1.3")` asvs_V2_1_3_passwordNotTruncated()
    - `@Tag("ASVS-V2.1.4")` asvs_V2_1_4_unicodeAndEmojiAllowed()
    - `@Tag("ASVS-V2.1.9")` asvs_V2_1_9_noCompositionRulesEnforced()
    - Account enumeration 방지: 존재하는 email로 signup 시 generic 응답
  - **GREEN**: `POST /api/auth/email/signup` 구현 — AuthService, AuthController
  - **REFACTOR**: validation 로직 정리
  - MockMvc with real SecurityFilterChain — `@WithMockUser` 금지
  **Must NOT do**: login/verify/refresh 등 다른 엔드포인트 구현 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 3 Backend | Blocks: 8,9,10 | Blocked By: 3,6

  **References**:
  - `contracts/auth-openapi.yaml` — POST /auth/email/signup 스키마
  - `specs/auth-asvs-l1.yaml` — V2.1.x 항목들
  - ASVS V2.1.1-V2.1.9 구체적 요구사항

  **Acceptance Criteria**:
  - [ ] `POST /api/auth/email/signup` 엔드포인트 동작
  - [ ] 5개 ASVS @Tag 테스트 전부 통과
  - [ ] `cd backend && ./gradlew test --tests "*asvs_V2_1*"` exits 0
  - [ ] Account enumeration 방지 — 동일 email 재등록 시 generic 에러

  **QA Scenarios**:
  ```
  Scenario: ASVS V2.1.x password validation
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*asvs_V2_1*"
    Expected Result: 5개 테스트 모두 PASS
    Evidence: .sisyphus/evidence/task-7-asvs-v2-1.txt

  Scenario: Signup happy path
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew bootRun &
      2. sleep 15
      3. curl -s -X POST localhost:8080/api/auth/email/signup -H "Content-Type: application/json" -d '{"email":"test@test.com","password":"securepassword12"}' | jq .userId
      4. kill %1
    Expected Result: UUID 형식 userId 반환
    Evidence: .sisyphus/evidence/task-7-signup-happy.txt
  ```

  **Commit**: YES | Message: `feat(auth): signup endpoint with ASVS V2.1.x password validation` | Files: backend/src/**/Auth*.java, backend/src/test/**/ASVS*.java

- [x] 8. TDD: Login endpoint + rate limiting

  **What to do**:
  - **RED**: ASVS 테스트:
    - `@Tag("ASVS-V2.2.1")` asvs_V2_2_1_rateLimitAfter5FailedAttemptsIn15Min() — ASVS V2.2.1은 max 100/h이지만, auth-manifest는 더 엄격한 5/15min 정의. 엄격한 기준(manifest) 적용.
    - Generic error message 테스트 — 존재하지 않는 user vs 잘못된 password 동일 응답
    - Timing attack 방지 — 응답 시간 편차 < 10ms
  - **GREEN**: `POST /api/auth/email/login` 구현 — JWT access token + refresh token cookie 발급
  - Access token은 응답 body, refresh token은 HttpOnly cookie
  - Rate limiting: in-memory counter (5 attempts / 15 min per email)
  **Must NOT do**: 복잡한 rate limit 인프라(Redis 등) 구축 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 3 Backend | Blocks: 11,12,13 | Blocked By: 7

  **References**:
  - `contracts/auth-openapi.yaml` — POST /auth/email/login 스키마
  - `specs/auth-asvs-l1.yaml` — V2.2.1
  - `blueprints/auth-manifest.yaml` — token_strategy, rate_limit

  **Acceptance Criteria**:
  - [ ] `POST /api/auth/email/login` 동작 — access token + refresh cookie 발급
  - [ ] ASVS V2.2.1 rate limiting 테스트 통과 (5 failed attempts / 15min per email, manifest 기준)
  - [ ] Generic error message 테스트 통과 (account enumeration 방지)
  - [ ] `cd backend && ./gradlew test --tests "*asvs_V2_2*"` exits 0

  **QA Scenarios**:
  ```
  Scenario: ASVS V2.2.1 rate limiting
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*asvs_V2_2*"
    Expected Result: rate limit 테스트 PASS
    Evidence: .sisyphus/evidence/task-8-asvs-v2-2.txt

  Scenario: Login returns tokens (JUnit integration test — does NOT depend on Task 9)
    Tool: Bash
    Note: Login QA uses JUnit MockMvc tests that seed a pre-verified user directly in DB (userRepository.save with emailVerified=true), bypassing the verify-email endpoint. This avoids circular dependency with Task 9.
    Steps:
      1. cd backend && ./gradlew test --tests "*asvs_V2_2*" --tests "*LoginIntegration*"
    Expected Result: all login tests PASS including rate limiting, token issuance, generic error messages
    Evidence: .sisyphus/evidence/task-8-login-tokens.txt
  ```

  **Commit**: YES | Message: `feat(auth): login endpoint with rate limiting [ASVS V2.2.1]` | Files: backend/src/**/Auth*.java, backend/src/test/**/ASVS*.java

- [x] 9. TDD: Email verification flow + resend

  **What to do**:
  - **RED**: 테스트 — 유효 토큰으로 verify 성공, 만료 토큰(24h) 거부, 이미 사용된 토큰 거부, 존재하지 않는 토큰 거부
  - `@Tag("ASVS-V2.7.2")` asvs_V2_7_2_verificationTokenExpiresAfter24h()
  - `@Tag("ASVS-V2.7.3")` asvs_V2_7_3_verificationTokenSingleUse()
  - Resend 테스트: 이전 토큰 무효화 후 새 토큰 발급, rate limit (3 attempts/10min)
  - **GREEN**:
    - `POST /api/auth/email/verify-email` 구현 — token 검증, user 상태 변경
    - `POST /api/auth/email/resend-verification` 구현 — 새 토큰 발급, 이전 토큰 무효화, rate limit
  - Signup 시 verification token 생성 + **구조화된 console 로깅**: `[AUTH-TOKEN] type=VERIFY email=user@test.com token=<uuid>` 형태로 출력. QA 스크립트가 `grep -F "[AUTH-TOKEN]"` + `awk`로 자동 추출 가능해야 함.
  - Resend 시에도 동일 형태로 로깅
  - Unverified user는 login 불가
  **Must NOT do**: 실제 SMTP 연동 금지. console 로깅으로 대체.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 3 Backend | Blocks: 10,18 | Blocked By: 7

  **References**:
  - `contracts/auth-openapi.yaml` — POST /auth/email/verify-email
  - `specs/auth-asvs-l1.yaml` — V2.7.2, V2.7.3
  - Auto-resolved: token expiry 24h, console logging

  **Acceptance Criteria**:
  - [ ] `POST /api/auth/email/verify-email` — 유효 토큰 verify 성공 → user emailVerified = true
  - [ ] 만료/사용된/존재하지 않는 토큰 거부
  - [ ] `POST /api/auth/email/resend-verification` — 새 토큰 발급, 이전 토큰 무효화
  - [ ] Resend rate limit (3/10min) 동작
  - [ ] `cd backend && ./gradlew test --tests "*asvs_V2_7*"` exits 0
  - [ ] Unverified user login 시 적절한 에러

  **QA Scenarios**:
  ```
  Scenario: ASVS V2.7.x token lifecycle
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*asvs_V2_7*"
    Expected Result: token expiry + single-use 테스트 PASS
    Evidence: .sisyphus/evidence/task-9-asvs-v2-7.txt

  Scenario: Unverified user cannot login
    Tool: Bash
    Preconditions: Backend started via `cd backend && ./gradlew bootRun > /tmp/auth-test.log 2>&1 &` and waited 15s.
    Steps:
      1. curl -s -X POST localhost:8080/api/auth/email/signup -H "Content-Type: application/json" -d '{"email":"unverified@test.com","password":"securepassword12"}'
      2. curl -s -o /dev/null -w "%{http_code}" -X POST localhost:8080/api/auth/email/login -H "Content-Type: application/json" -d '{"email":"unverified@test.com","password":"securepassword12"}'
      3. kill %1
    Expected Result: HTTP 403 반환
    Evidence: .sisyphus/evidence/task-9-unverified-login.txt
  ```

  **Commit**: YES | Message: `feat(auth): email verification flow [ASVS V2.7.2, V2.7.3]` | Files: backend/src/**/Verification*.java, backend/src/test/**/ASVS*.java

- [x] 10. TDD: Password reset + password change

  **What to do**:
  - **RED**: ASVS 테스트:
    - `@Tag("ASVS-V2.5.2")` asvs_V2_5_2_noSecurityQuestions() — secret questions 엔드포인트 부재 확인
    - `@Tag("ASVS-V2.5.3")` asvs_V2_5_3_resetDoesNotRevealCurrentPassword()
    - `@Tag("ASVS-V2.5.4")` asvs_V2_5_4_noDefaultAccounts() — admin/admin 등 거부
    - `@Tag("ASVS-V2.1.6")` asvs_V2_1_6_passwordChangeRequiresCurrentAndNew()
    - Reset request는 존재/비존재 email 모두 동일 응답 (enumeration 방지)
    - Reset token은 single-use, 시간 제한
    - Password change는 현재 비밀번호 검증 필수
  - **GREEN**:
    - `POST /api/auth/email/password-reset-request` — reset token 발급 (console 로깅)
    - `POST /api/auth/email/password-reset` — token으로 비밀번호 재설정
    - `POST /api/auth/email/password-change` — 인증된 사용자가 현재+새 비밀번호로 변경
  - Reset token → **구조화된 console 로깅**: `[AUTH-TOKEN] type=RESET email=user@test.com token=<uuid>` 형태. QA 스크립트가 자동 추출 가능.
  **Must NOT do**: 실제 이메일 발송 금지. 복잡한 token 저장소 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 3 Backend | Blocks: 18 | Blocked By: 7

  **References**:
  - `contracts/auth-openapi.yaml` — password reset 엔드포인트 (T2에서 추가)
  - `specs/auth-asvs-l1.yaml` — V2.5.2, V2.5.3, V2.5.4

  **Acceptance Criteria**:
  - [ ] `POST /api/auth/email/password-reset-request` → token 생성 (console 로깅)
  - [ ] `POST /api/auth/email/password-reset` → 유효 token으로 password 변경 성공
  - [ ] `POST /api/auth/email/password-change` → 현재 비밀번호 검증 후 변경 성공
  - [ ] ASVS V2.5.x + V2.1.6 테스트 전부 통과
  - [ ] Enumeration 방지 — 존재/비존재 email 동일 응답
  - [ ] Password change 시 현재 비밀번호 누락하면 400/401

  **QA Scenarios**:
  ```
  Scenario: ASVS V2.5.x password reset security
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*asvs_V2_5*"
    Expected Result: 3개 테스트 모두 PASS
    Evidence: .sisyphus/evidence/task-10-asvs-v2-5.txt

  Scenario: Reset token single-use
    Tool: Bash
    Preconditions: Backend running with log at /tmp/auth-test.log. User reset-test@test.com already signed up and verified (from ASVS test setup in JUnit, or provisioned via curl signup+verify sequence).
    Steps:
      1. curl -s -X POST localhost:8080/api/auth/email/password-reset-request -H "Content-Type: application/json" -d '{"email":"reset-test@test.com"}'
      2. RTOKEN=$(grep -F "[AUTH-TOKEN] type=RESET email=reset-test@test.com" /tmp/auth-test.log | tail -1 | awk '{print $NF}' | cut -d= -f2)
      3. curl -s -o /dev/null -w "%{http_code}" -X POST localhost:8080/api/auth/email/password-reset -H "Content-Type: application/json" -d "{\"token\":\"$RTOKEN\",\"newPassword\":\"newpassword1234\"}"
      4. curl -s -o /dev/null -w "%{http_code}" -X POST localhost:8080/api/auth/email/password-reset -H "Content-Type: application/json" -d "{\"token\":\"$RTOKEN\",\"newPassword\":\"anotherpassword1\"}"
    Expected Result: Step 3 → HTTP 200, Step 4 → HTTP 400 또는 410 (token expired/used)
    Evidence: .sisyphus/evidence/task-10-reset-single-use.txt
  ```

  **Commit**: YES | Message: `feat(auth): password reset flow [ASVS V2.5.x]` | Files: backend/src/**/PasswordReset*.java, backend/src/test/**/ASVS*.java

- [x] 11. TDD: Refresh token rotation with grace window

  **What to do**:
  - **RED**: ASVS 테스트:
    - `@Tag("ASVS-V3.2.1")` asvs_V3_2_1_newSessionTokenOnAuth() — login 시 새 토큰 발급
    - `@Tag("ASVS-V3.7.1")` asvs_V3_7_1_fullSessionRequiredForSensitiveOps()
    - Refresh rotation 테스트 — 새 access+refresh 발급, 이전 refresh 무효화
    - Grace window (30s) 테스트 — 동시 탭 처리
  - **GREEN**: `POST /api/auth/refresh` 구현 — token rotation, grace window 30s
  - Refresh token은 DB 저장 (session table)
  **Must NOT do**: Redis 기반 토큰 저장 금지. DB만 사용.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 3 Backend | Blocks: 12 | Blocked By: 8

  **References**:
  - `contracts/auth-openapi.yaml` — POST /auth/refresh
  - `specs/auth-asvs-l1.yaml` — V3.2.1, V3.7.1
  - `blueprints/auth-manifest.yaml` — rotation: grace_window

  **Acceptance Criteria**:
  - [ ] Refresh 시 새 access + refresh token 발급
  - [ ] 이전 refresh token 무효화 (grace window 내 제외)
  - [ ] `cd backend && ./gradlew test --tests "*asvs_V3_2*" --tests "*asvs_V3_7*"` exits 0

  **QA Scenarios**:
  ```
  Scenario: ASVS V3.x session management
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*asvs_V3*"
    Expected Result: session 관련 ASVS 테스트 PASS
    Evidence: .sisyphus/evidence/task-11-asvs-v3.txt
  ```

  **Commit**: YES | Message: `feat(auth): refresh token rotation with grace window [ASVS V3.x]` | Files: backend/src/**/RefreshToken*.java, backend/src/test/**/ASVS*.java

- [x] 12. TDD: Logout + session invalidation

  **What to do**:
  - **RED**: ASVS 테스트:
    - `@Tag("ASVS-V3.3.1")` asvs_V3_3_1_logoutInvalidatesSession() — logout 후 기존 토큰 사용 불가
    - Logout 후 refresh cookie 클리어 확인
  - **GREEN**: `POST /api/auth/logout` 구현 — refresh token DB 삭제, cookie 클리어
  **Must NOT do**: 전역 세션 관리 구축 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 3 Backend | Blocks: 18 | Blocked By: 8

  **References**:
  - `contracts/auth-openapi.yaml` — POST /auth/logout
  - `specs/auth-asvs-l1.yaml` — V3.3.1

  **Acceptance Criteria**:
  - [ ] Logout 후 기존 access token으로 /auth/me 접근 → 401
  - [ ] Logout 후 refresh cookie 클리어됨
  - [ ] `cd backend && ./gradlew test --tests "*asvs_V3_3*"` exits 0

  **QA Scenarios**:
  ```
  Scenario: ASVS V3.3.1 session invalidation
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*asvs_V3_3*"
    Expected Result: logout 후 token 무효화 PASS
    Evidence: .sisyphus/evidence/task-12-asvs-v3-3.txt
  ```

  **Commit**: YES | Message: `feat(auth): logout with session invalidation [ASVS V3.3.1]` | Files: backend/src/**/Auth*.java, backend/src/test/**/ASVS*.java

- [x] 13. TDD: /auth/me + access control

  **What to do**:
  - **RED**: ASVS 테스트:
    - `@Tag("ASVS-V4.1.1")` asvs_V4_1_1_accessControlOnTrustedLayer() — 인증 없이 접근 거부
    - `@Tag("ASVS-V4.1.5")` asvs_V4_1_5_accessControlFailsSecurely() — 예외 시에도 거부
    - `@Tag("ASVS-V4.2.1")` asvs_V4_2_1_noIDOR() — User A가 User B 데이터 접근 불가
    - `@Tag("ASVS-V3.1.1")` asvs_V3_1_1_noTokenInURL() — URL 파라미터에 토큰 노출 금지
  - **GREEN**: `GET /api/auth/me` 구현 — JWT에서 userId 추출, 자기 정보만 반환
  **Must NOT do**: 다른 사용자 정보 조회 API 구축 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 3 Backend | Blocks: 18 | Blocked By: 8

  **References**:
  - `contracts/auth-openapi.yaml` — GET /auth/me
  - `specs/auth-asvs-l1.yaml` — V4.1.1, V4.1.5, V4.2.1, V3.1.1

  **Acceptance Criteria**:
  - [ ] 인증된 요청 → 자기 프로필 반환
  - [ ] 미인증 요청 → 401
  - [ ] IDOR 시도 → 403/404
  - [ ] `cd backend && ./gradlew test --tests "*asvs_V4*" --tests "*asvs_V3_1*"` exits 0

  **QA Scenarios**:
  ```
  Scenario: ASVS V4.x access control
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*asvs_V4*"
    Expected Result: access control 테스트 전부 PASS
    Evidence: .sisyphus/evidence/task-13-asvs-v4.txt
  ```

  **Commit**: YES | Message: `feat(auth): /auth/me with access control [ASVS V4.x]` | Files: backend/src/**/Auth*.java, backend/src/test/**/ASVS*.java

- [x] 14. TDD: Security baseline — cookie flags, CSRF, CORS

  **What to do**:
  - **RED**: ASVS 테스트:
    - `@Tag("ASVS-V3.4.1")` asvs_V3_4_1_cookieSecureFlag()
    - `@Tag("ASVS-V3.4.2")` asvs_V3_4_2_cookieHttpOnlyFlag()
    - `@Tag("ASVS-V3.4.3")` asvs_V3_4_3_cookieSameSiteAttribute()
    - `@Tag("ASVS-V4.2.2")` asvs_V4_2_2_antiCSRF() — CSRF 보호 검증: `/api/auth/**`는 stateless JWT이므로 CSRF 비활성화됨을 확인. Bearer 토큰 자체가 CSRF 방지 역할 (custom header에 토큰을 보내므로 cross-site request로 자동 전송되지 않음). 테스트: CSRF 토큰 없이 Bearer 인증만으로 POST 요청 성공 확인.
    - JWT validation: algorithm none 거부, 잘못된 서명 거부, 만료 토큰 거부
  - **GREEN**: SecurityConfig 최종 조정 — cookie flags, CORS 정책, JWT validation
  **Must NOT do**: custom security framework 구축 금지. Spring Security 기본 기능만 활용.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 3 Backend | Blocks: 18 | Blocked By: 6, 8 (login 엔드포인트가 있어야 cookie flag 테스트 가능)

  **References**:
  - `specs/auth-asvs-l1.yaml` — V3.4.1-V3.4.3, V4.2.2
  - OWASP CheatSheet: JWT validation rules
  - `backend/src/**/SecurityConfig.java`

  **Acceptance Criteria**:
  - [ ] Set-Cookie: HttpOnly; Secure; SameSite=Strict 확인
  - [ ] Stateless JWT 엔드포인트에서 CSRF 비활성화 확인 — Bearer 토큰만으로 POST 성공
  - [ ] algorithm:none JWT → 401
  - [ ] `cd backend && ./gradlew test --tests "*asvs_V3_4*" --tests "*asvs_V4_2*"` exits 0

  **QA Scenarios**:
  ```
  Scenario: Security headers verification
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew bootRun &
      2. sleep 15
      3. curl -sD /tmp/task14-headers.txt -X POST localhost:8080/api/auth/email/login -H "Content-Type: application/json" -d '...' | grep -i "set-cookie"
      4. kill %1
    Expected Result: HttpOnly; Secure; SameSite=Strict 포함
    Evidence: .sisyphus/evidence/task-14-security-headers.txt

  Scenario: JWT attack vectors rejected
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew test --tests "*jwt*" --tests "*asvs*"
    Expected Result: none algorithm, bad signature, expired token 모두 거부
    Evidence: .sisyphus/evidence/task-14-jwt-attacks.txt
  ```

  **Commit**: YES | Message: `feat(auth): security baseline — cookie flags, CSRF, JWT validation [ASVS V3.4.x, V4.2.2]` | Files: backend/src/**/SecurityConfig.java, backend/src/test/**/ASVS*.java

- [x] 15. Frontend: Auth store + API client + MSW mocks

  **What to do**:
  - OpenAPI 계약 기반 API client 생성 (fetch wrapper)
  - Auth store (zustand 또는 context) — user state, token 관리, login/logout actions
  - MSW handlers — OpenAPI 계약의 모든 엔드포인트에 대한 mock 응답
  - Vitest 테스트: store actions이 올바르게 동작하는지
  **Must NOT do**: UI 컴포넌트/페이지 구현 금지. 데이터 레이어만.

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: [`vercel-react-best-practices`]

  **Parallelization**: Wave 3 Frontend | Blocks: 16 | Blocked By: 2,5

  **References**:
  - `contracts/auth-openapi.yaml` — 엔드포인트 + 스키마 정의
  - `frontend/src/lib/auth/store.ts` — 기존 AuthState 인터페이스 (참고용)

  **Acceptance Criteria**:
  - [ ] API client 모듈 존재 (signup, login, verify, refresh, logout, me)
  - [ ] Auth store 존재 (user state, actions)
  - [ ] MSW handlers 존재 (모든 auth 엔드포인트)
  - [ ] `npm --prefix frontend test` — store 테스트 통과

  **QA Scenarios**:
  ```
  Scenario: Auth store with MSW
    Tool: Bash
    Steps:
      1. cd frontend && npx vitest run --reporter=verbose tests/auth-store.vitest.ts
    Expected Result: login/logout/signup store actions 테스트 PASS, exit 0
    Evidence: .sisyphus/evidence/task-15-auth-store.txt
  ```

  **Commit**: YES | Message: `feat(frontend): auth store, API client, MSW mocks` | Files: frontend/src/lib/**, frontend/src/mocks/**, frontend/tests/**

- [x] 16. Frontend: Auth pages (signup, login, verify, dashboard-stub)

  **What to do**:
  - Signup page: email + password form, validation (12자 이상), 에러 표시
  - Login page: email + password form, 에러 표시, rate limit 안내
  - Verify page: token parameter 처리, 성공/실패 표시
  - Dashboard stub: /auth/me 데이터 표시, logout 버튼
  - React Router 설정 (SPA routing)
  **Must NOT do**: 디자인 시스템 구축 금지. 기능 동작만 확인 가능한 최소 UI.

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: [`vercel-react-best-practices`]

  **Parallelization**: Wave 3 Frontend | Blocks: 17 | Blocked By: 15

  **References**:
  - `contracts/auth-openapi.yaml` — 요청/응답 스키마
  - Task 15 auth store — UI에서 사용

  **Acceptance Criteria**:
  - [ ] 4개 페이지 존재 (signup, login, verify, dashboard)
  - [ ] 라우팅 동작
  - [ ] `npm --prefix frontend run build` 성공

  **QA Scenarios**:
  ```
  Scenario: Pages render without error
    Tool: Bash
    Steps:
      1. cd frontend && npm run build
      2. cd frontend && npx vite preview --port 4173 &
      3. sleep 5 && curl -s localhost:4173 | grep -i "html"
      4. kill %1
    Expected Result: build 성공, HTML 서빙
    Evidence: .sisyphus/evidence/task-16-pages-build.txt
  ```

  **Commit**: YES | Message: `feat(frontend): signup, login, verify, dashboard pages` | Files: frontend/src/pages/**, frontend/src/App.tsx

- [x] 17. Frontend: Vitest tests

  **What to do**:
  - 각 페이지 컴포넌트 렌더 테스트 (Testing Library)
  - Form validation 테스트 (password 12자 미만 거부)
  - MSW와 연동한 API 호출 테스트
  - Error state 렌더링 테스트
  **Must NOT do**: E2E 테스트 작성 금지 (Task 18에서 처리). Playwright 사용 금지.

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: []

  **Parallelization**: Wave 3 Frontend | Blocks: 18 | Blocked By: 16

  **References**:
  - Task 15 MSW handlers
  - Task 16 페이지 컴포넌트

  **Acceptance Criteria**:
  - [ ] 각 페이지별 최소 1개 테스트
  - [ ] `npm --prefix frontend test` exits 0

  **QA Scenarios**:
  ```
  Scenario: Frontend tests pass
    Tool: Bash
    Steps:
      1. cd frontend && npm test
    Expected Result: all tests pass, exit 0
    Evidence: .sisyphus/evidence/task-17-frontend-tests.txt
  ```

  **Commit**: YES | Message: `test(frontend): component + API integration tests` | Files: frontend/tests/**

- [x] 18. E2E integration smoke test

  **What to do**:
  - Backend + Frontend 동시 기동
  - Full user journey: signup → (console에서 token 추출) → verify-email → login → /auth/me 확인 → logout → /auth/me 401 확인
  - curl 또는 간단한 스크립트로 전체 경로 검증
  - Password change flow: login → change password → logout → login with new password
  **Must NOT do**: Playwright E2E 아님. API 레벨 통합 테스트. UI 렌더링 검증 불필요.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 4 | Blocks: 19 | Blocked By: 7,8,9,10,11,12,13,14,17 (모든 backend + frontend 완료 후)

  **References**:
  - 모든 backend 엔드포인트 (T7-T14)
  - `contracts/auth-openapi.yaml`

  **Acceptance Criteria**:
  - [ ] Full journey: signup→verify→login→me→logout→me(401) 전체 통과
  - [ ] Password change journey 통과
  - [ ] 단일 스크립트로 재현 가능

  **QA Scenarios**:
  ```
  Scenario: Full auth journey
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew bootRun > /tmp/e2e-auth.log 2>&1 &
      2. sleep 15
      3. SIGNUP: curl -s -X POST localhost:8080/api/auth/email/signup -H "Content-Type: application/json" -d '{"email":"e2e@test.com","password":"securepassword12"}' | jq -e .userId
      4. VTOKEN=$(grep -F "[AUTH-TOKEN] type=VERIFY email=e2e@test.com" /tmp/e2e-auth.log | awk '{print $NF}' | cut -d= -f2)
      5. VERIFY: curl -s -X POST localhost:8080/api/auth/email/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$VTOKEN\"}" | jq -e .message
      6. LOGIN: RESPONSE=$(curl -sD /tmp/e2e-cookies.txt -X POST localhost:8080/api/auth/email/login -H "Content-Type: application/json" -d '{"email":"e2e@test.com","password":"securepassword12"}'); TOKEN=$(echo $RESPONSE | jq -r .accessToken)
      7. ME: curl -s -H "Authorization: Bearer $TOKEN" localhost:8080/api/auth/me | jq -e .email
      8. LOGOUT: curl -s -X POST localhost:8080/api/auth/logout -H "Authorization: Bearer $TOKEN" -b /tmp/e2e-cookies.txt
      9. ME_AFTER: curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" localhost:8080/api/auth/me
      10. kill %1
    Expected Result: Steps 3-7 성공, Step 9 → HTTP 401
    Evidence: .sisyphus/evidence/task-18-e2e-smoke.txt

  Scenario: Password change journey (runs within same backend session as above)
    Tool: Bash
    Preconditions: Backend already running from "Full auth journey" scenario above. e2e@test.com already verified and logged in.
    Steps:
      1. LOGIN: RESPONSE=$(curl -s -X POST localhost:8080/api/auth/email/login -H "Content-Type: application/json" -d '{"email":"e2e@test.com","password":"securepassword12"}'); TOKEN=$(echo $RESPONSE | jq -r .accessToken)
      2. CHANGE: curl -s -o /dev/null -w "%{http_code}" -X POST localhost:8080/api/auth/email/password-change -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"currentPassword":"securepassword12","newPassword":"changedpassword1"}'
      3. LOGOUT: curl -s -X POST localhost:8080/api/auth/logout -H "Authorization: Bearer $TOKEN"
      4. RELOGIN: curl -s -o /dev/null -w "%{http_code}" -X POST localhost:8080/api/auth/email/login -H "Content-Type: application/json" -d '{"email":"e2e@test.com","password":"changedpassword1"}'
    Expected Result: Step 2 → 200, Step 4 → 200 (새 비밀번호로 로그인 성공)
    Evidence: .sisyphus/evidence/task-18-password-change.txt
  ```

  **Commit**: YES | Message: `test: E2E smoke — signup→verify→login→me→logout` | Files: backend/src/test/**/E2E*.java or scripts/e2e-smoke.sh

- [x] 19. ASVS compliance summary generation

  **What to do**:
  - `specs/auth-asvs-l1.yaml`의 모든 `applicable: true` 항목 순회
  - 각 항목의 @Tag 테스트 존재 여부 확인 (소스 코드 grep)
  - 테스트 실행 결과와 교차검증
  - `specs/auth-asvs-l1-report.md` 생성: 항목 ID | 요구사항 | 테스트명 | PASS/FAIL
  - 누락 항목 있으면 명시
  **Must NOT do**: 보고서 생성 도구/프레임워크 구축 금지. 단순 스크립트.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**: Wave 4 | Blocks: F1-F4 | Blocked By: 18

  **References**:
  - `specs/auth-asvs-l1.yaml` — pinned 항목 목록
  - backend/src/test/ — ASVS @Tag 테스트

  **Acceptance Criteria**:
  - [ ] `specs/auth-asvs-l1-report.md` 존재
  - [ ] 모든 applicable 항목에 대응하는 테스트명 기재
  - [ ] 누락 항목 0건

  **QA Scenarios**:
  ```
  Scenario: All ASVS items covered
    Tool: Bash
    Steps:
      1. SPEC_COUNT=$(python3 -c "import yaml; items=[i for i in yaml.safe_load(open('specs/auth-asvs-l1.yaml'))['items'] if i['applicable']]; print(len(items))")
      2. TAG_COUNT=$(grep -r '@Tag("ASVS-V' backend/src/test/java/ | sed 's/.*@Tag("//;s/").*//' | sort -u | wc -l | tr -d ' ')
      3. echo "Spec: $SPEC_COUNT, Unique ASVS-V tags: $TAG_COUNT"
      4. python3 -c "import yaml; [print(i['id']) for i in yaml.safe_load(open('specs/auth-asvs-l1.yaml'))['items'] if i['applicable']]" | sort > /tmp/t19-spec-ids.txt
      5. grep -r '@Tag("ASVS-V' backend/src/test/java/ | sed 's/.*@Tag("//;s/").*//' | sort -u > /tmp/t19-tag-ids.txt
      6. diff /tmp/t19-spec-ids.txt /tmp/t19-tag-ids.txt
    Expected Result: SPEC_COUNT == TAG_COUNT, diff 출력 0줄 (완전 1:1 대응)
    Evidence: .sisyphus/evidence/task-19-asvs-coverage.txt
  ```

  **Commit**: YES | Message: `docs: ASVS L1 compliance summary` | Files: specs/auth-asvs-l1-report.md

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.

- [ ] F1. **ASVS Compliance Audit** — `oracle`
  **QA Scenario**:
  ```
  Tool: Bash
  Steps:
    1. cd backend && ./gradlew testAsvs 2>&1 | tee /tmp/f1-asvs-results.txt
    2. TOTAL=$(grep -rc "@Tag.*ASVS-" backend/src/test/java/ | awk -F: '{s+=$2}END{print s}'); PASS=$(grep "tests successful" /tmp/f1-asvs-results.txt | awk '{print $1}')
    3. python3 -c "import yaml; items=[i for i in yaml.safe_load(open('specs/auth-asvs-l1.yaml'))['items'] if i['applicable']]; print(f'Spec items: {len(items)}')"
    4. grep -r '@Tag("ASVS-V' backend/src/test/java/ | sed 's/.*@Tag("//;s/").*//' | sort -u > /tmp/f1-tags.txt
    5. diff <(python3 -c "import yaml; [print(i['id']) for i in yaml.safe_load(open('specs/auth-asvs-l1.yaml'))['items'] if i['applicable']]" | sort) /tmp/f1-tags.txt
  Expected: diff 출력 0줄 (1:1 매핑), 모든 테스트 PASS
  Evidence: .sisyphus/evidence/f1-asvs-audit.txt
  ```
  Output: `ASVS items [N/N covered] | Tests [N pass/N fail] | VERDICT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  **QA Scenario**:
  ```
  Tool: Bash
  Steps:
    1. cd backend && ./gradlew build 2>&1 | tail -5
    2. cd frontend && npm run build 2>&1 | tail -5
    3. cd frontend && npm run test 2>&1 | tail -5
    4. grep -rn "as any\|@ts-ignore" frontend/src/ | wc -l
    5. grep -rn "@WithMockUser\|addFilters.*false\|assertTrue(true)" backend/src/test/ | wc -l
    6. find backend/src -name "*Placeholder*" -o -name "*Skeleton*" | wc -l
  Expected: Steps 1-3 exit 0, Steps 4-6 모두 0 출력
  Evidence: .sisyphus/evidence/f2-code-quality.txt
  ```
  Output: `Build [PASS/FAIL] | Tests [N/N] | Anti-patterns [N found] | VERDICT`

- [ ] F3. **Security Baseline QA** — `unspecified-high`
  **QA Scenario**:
  ```
  Tool: Bash
  Preconditions: cd backend && ./gradlew bootRun > /tmp/f3-app.log 2>&1 & (wait 15s)
  Steps:
    1. curl -s -X POST localhost:8080/api/auth/email/signup -H "Content-Type: application/json" -d '{"email":"sec-test@test.com","password":"securepassword12"}'
    2. VTOKEN=$(grep -F "[AUTH-TOKEN] type=VERIFY email=sec-test@test.com" /tmp/f3-app.log | awk '{print $NF}' | cut -d= -f2)
    3. curl -s -X POST localhost:8080/api/auth/email/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$VTOKEN\"}"
    4. curl -sD /tmp/f3-headers.txt -X POST localhost:8080/api/auth/email/login -H "Content-Type: application/json" -d '{"email":"sec-test@test.com","password":"securepassword12"}' > /dev/null
    5. grep -i "HttpOnly" /tmp/f3-headers.txt && echo "HttpOnly: PASS" || echo "HttpOnly: FAIL"
    6. grep -i "Secure" /tmp/f3-headers.txt && echo "Secure: PASS" || echo "Secure: FAIL"
    7. grep -i "SameSite" /tmp/f3-headers.txt && echo "SameSite: PASS" || echo "SameSite: FAIL"
    8. R1=$(curl -s -o /dev/null -w "%{http_code}" -X POST localhost:8080/api/auth/email/login -H "Content-Type: application/json" -d '{"email":"sec-test@test.com","password":"wrong"}')
    9. R2=$(curl -s -o /dev/null -w "%{http_code}" -X POST localhost:8080/api/auth/email/login -H "Content-Type: application/json" -d '{"email":"nonexistent@test.com","password":"wrong"}')
    10. [ "$R1" = "$R2" ] && echo "Enumeration: SAFE" || echo "Enumeration: LEAK"
    11. TOKEN=$(curl -s -X POST localhost:8080/api/auth/email/login -H "Content-Type: application/json" -d '{"email":"sec-test@test.com","password":"securepassword12"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
    12. CSRF_CHECK=$(curl -s -o /dev/null -w "%{http_code}" -X POST localhost:8080/api/auth/logout -H "Authorization: Bearer $TOKEN")
    13. [ "$CSRF_CHECK" = "200" ] && echo "CSRF: PASS (stateless JWT, no CSRF needed)" || echo "CSRF: FAIL"
    14. kill %1
  Expected: Steps 5-7 PASS, Step 10 SAFE (identical status codes)
  Evidence: .sisyphus/evidence/f3-security-baseline.txt
  ```
  Output: `Headers [N/N pass] | Enumeration [SAFE/LEAK] | CSRF [PASS/FAIL] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  **QA Scenario**:
  ```
  Tool: Bash
  Preconditions: cd backend && ./gradlew bootRun > /tmp/f4-app.log 2>&1 & (wait 15s)
  Steps:
    1. python3 -c "import yaml; paths=yaml.safe_load(open('contracts/auth-openapi.yaml'))['paths']; [print(p) for p in sorted(paths.keys())]" > /tmp/f4-contract-paths.txt
    2. curl -s localhost:8080/actuator/mappings | python3 -c "import sys,json; d=json.load(sys.stdin); [print(m.get('predicate','')) for m in d.get('contexts',{}).get('application',{}).get('mappings',{}).get('dispatcherServlets',{}).get('dispatcherServlet',[])]" | grep -o '/auth[^ ]*' | sed 's/{[^}]*}/:id/g' | sort -u > /tmp/f4-impl-paths.txt
    3. MISSING=$(comm -23 /tmp/f4-contract-paths.txt /tmp/f4-impl-paths.txt | wc -l | tr -d ' ')
    4. echo "Missing contract paths in impl: $MISSING"
    5. OUTOFSCOPE=$(grep -rn "link-account\|oauth\|mfa\|social" backend/src/main/java/ | wc -l | tr -d ' ')
    6. echo "Out-of-scope code: $OUTOFSCOPE"
    7. if [ -d docs/governance ]; then NEWGOV=$(find docs/governance/ -name "*.md" -newer .sisyphus/plans/auth-verification-framework.md | wc -l | tr -d ' '); else NEWGOV=0; fi
    8. echo "New governance docs: $NEWGOV"
    9. [ "$MISSING" = "0" ] && [ "$OUTOFSCOPE" = "0" ] && [ "$NEWGOV" = "0" ] && echo "F4: PASS" || echo "F4: FAIL (missing=$MISSING, outofscope=$OUTOFSCOPE, newgov=$NEWGOV)"
    10. kill %1
  Expected: Step 9 출력 "F4: PASS". MISSING=0 (contract 경로 전부 구현에 존재), OUTOFSCOPE=0 (scope 밖 코드 없음), NEWGOV=0 (새 거버넌스 문서 없음). 하나라도 0이 아니면 자동 FAIL.
  Evidence: .sisyphus/evidence/f4-scope-fidelity.txt
  ```
  Output: `Endpoints [N/N match] | Out-of-scope [CLEAN/N violations] | Governance [CLEAN/N new files] | VERDICT`

---

## Commit Strategy

| Phase | Commit Message | Files |
|-------|---------------|-------|
| Cleanup | `chore: archive governance docs, delete placeholder files` | docs/archive/**, backend/src/**, frontend/src/** |
| Contract | `fix(contract): align OpenAPI with ASVS L1 (password rules, add reset endpoint)` | contracts/auth-openapi.yaml |
| ASVS Spec | `docs(specs): pin ASVS L1 verification items for email auth` | specs/auth-asvs-l1.yaml |
| Backend Bootstrap | `build(backend): add JPA, H2, Testcontainers, security deps` | backend/build.gradle.kts, backend/src/** |
| Frontend Bootstrap | `build(frontend): add Vite, testing-library, MSW` | frontend/package.json, frontend/vite.config.ts |
| Per-endpoint | `feat(auth): {endpoint} with ASVS tests [Vx.x.x]` | backend/src/**/**, backend/src/test/** |
| Frontend | `feat(frontend): auth store + pages + tests` | frontend/src/**, frontend/tests/** |
| E2E | `test: E2E smoke — signup→verify→login→me→logout` | backend/src/test/** or e2e/** |
| Summary | `docs: ASVS L1 compliance summary` | specs/auth-asvs-l1-report.md |

---

## Success Criteria

### Verification Commands
```bash
cd backend && ./gradlew test            # Expected: BUILD SUCCESSFUL, all tests pass
cd backend && ./gradlew testAsvs                # Expected: ~23 ASVS tagged tests pass
cd frontend && npm run test             # Expected: all frontend tests pass
cd frontend && npm run build            # Expected: build successful
curl -s -X POST localhost:8080/api/auth/email/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"securepassword12"}' | jq .userId
                                        # Expected: returns UUID
curl -sD - -X POST localhost:8080/api/auth/refresh -b cookies.txt | grep -i set-cookie
                                        # Expected: HttpOnly; Secure; SameSite=Strict
```

### Final Checklist
- [ ] All "Must Have" present — TDD, ASVS tags, real security chain tests, OpenAPI compliance
- [ ] All "Must NOT Have" absent — no governance docs, no @WithMockUser, no placeholders, no OAuth
- [ ] All ASVS tests pass
- [ ] E2E smoke test passes
- [ ] Frontend builds and tests pass
