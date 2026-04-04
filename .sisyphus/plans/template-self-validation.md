# Template Self-Validation — ax-template 소비자 검증

## TL;DR

> **Quick Summary**: ax-template의 spec/contract 파일만 복사해서 새 프로젝트에서 처음부터 구현하고, ASVS 검증 피드백 루프가 실제로 동작하는지 셀프 검증한다. 진짜 산출물은 코드가 아니라 **GAP-REPORT.md** — 템플릿의 이식성과 부족한 점을 발견하는 것이 목적.
>
> **Deliverables**:
> - `/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test/` — 더미 프로젝트 (검증 후 폐기 가능)
> - `GAP-REPORT.md` — 이식성 분석: Portable ✅ / Partially Portable ⚠️ / Not Portable ❌ / Missing ❓
> - 6개 ASVS 항목의 RED → GREEN → VIOLATION 사이클 증거
>
> **Estimated Effort**: Medium (1-2d)
> **Parallel Execution**: YES — 2 waves
> **Critical Path**: Scaffold → RED tests → GREEN impl → VIOLATION → GAP-REPORT

---

## Context

### Original Request
ax-template을 만들었지만 아무도 "소비자"로 써본 적 없음. specs + contracts만 복사해서 새 프로젝트에서 검증 피드백 루프가 동작하는지 테스트.

### Key Insight (Metis)
- ASVS spec YAML은 이식 가능 (요구사항 목록)
- OpenAPI contract은 이식 가능 (API 형태)
- **JUnit 테스트는 이식 불가** — ax-template의 MockMvc + 패키지 구조에 결합
- 검증의 핵심: "규칙은 이식 가능한가? 검증은 이식 가능한가?"
- 복사해야 할 파일이 3개: specs + contracts + **blueprints/auth-manifest.yaml** (정책 값 포함)

### Metis Review
- black-box HTTP 테스트 사용 (MockMvc X, RestAssured O)
- 6개 ASVS 항목으로 스코프 제한 (전체 23개 불필요)
- 3개 엔드포인트만 구현 (signup, login, logout)
- 스펙을 읽다가 ax-template 소스를 봐야 했던 순간 = 갭 증거

---

## Work Objectives

### Core Objective
**spec 파일만 보고** auth를 구현했을 때, ASVS 검증이 기계적으로 pass/fail을 판정할 수 있는지 증명.

### Concrete Deliverables
- `workspace/ax-validation-test/` — Spring Boot 프로젝트
- 6개 ASVS @Tag 테스트 (black-box HTTP)
  - 4개 엔드포인트 (signup, login, logout, /auth/me 관측용)
- `GAP-REPORT.md` — 핵심 산출물

### Definition of Done
- [ ] `cd /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test && ./gradlew testAsvs` exits 0 — 6개 ASVS 테스트 통과
- [ ] VIOLATION 테스트: 비밀번호 규칙 약화 → testAsvs 실패 (피드백 루프 증명)
- [ ] GAP-REPORT.md 완성 — Portable/Partially/Not/Missing 분류

### Must Have
- TDD: RED → GREEN → VIOLATION 사이클
- Black-box HTTP 테스트 (`@SpringBootTest(webEnvironment = RANDOM_PORT)` + RestAssured)
- ASVS @Tag 컨벤션 재사용 (`@Tag("ASVS")` + `@Tag("ASVS-V2.1.1")`)
- spec 해석 중 발생한 모든 모호함 기록

### Must NOT Have (Guardrails)
- **G1**: ax-template 파일 수정 금지 — read-only 참조
- **G2**: ax-template에서 Java 코드/테스트 코드 복사 금지 — spec 파일만
- **G3**: 4개 초과 엔드포인트 구현 금지 (signup, login, logout + `/auth/me` 관측용으로 4개 허용)
- **G4**: 프론트엔드 코드 금지 — 백엔드만
- **G5**: CI/CD, 문서화, "프로덕션 품질" 인프라 금지 — 폐기 가능한 검증 프로젝트
- **G6**: MockMvc 사용 금지 — black-box HTTP만 (이식성 검증의 핵심)

---

## Verification Strategy

> **핵심: 3단계 검증 사이클**
> 1. RED — spec만 보고 테스트 작성 → 구현 없으므로 실패
> 2. GREEN — 최소 구현 → 테스트 통과
> 3. VIOLATION — 의도적으로 규칙 위반 → 테스트가 잡아내는지 확인

### ASVS Items in Scope (6개)

| ID | 요구사항 | 선정 이유 |
|---|---|---|
| V2.1.1 | password min 12 chars | 가장 단순한 spec→test 경로 |
| V2.1.2 | password 64 허용, 129 거부 | 경계값 테스트 |
| V2.1.4 | unicode/emoji 허용 | 비자명 요구사항 |
| V2.2.1 | rate limiting 5/15min | 상태 기반 동작 + manifest 정책값 필요 |
| V3.3.1 | logout 세션 무효화 | 다른 챕터(세션관리) 검증. `/auth/me` 관측 엔드포인트로 무효화 확인 |
| V2.1.9 | 비밀번호 조합 규칙 없음 (all lowercase 12자 허용) | api_test 타입 — spec 해석 용이성 검증 |

### Test Framework
- `RestAssured` + `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- 실제 HTTP 요청으로 테스트 (MockMvc 아님)

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Scaffold — sequential):
├── Task 1: Init validation project + copy spec artifacts [quick]
└── Task 2: Write 6 ASVS tests from spec only (RED) [deep]

Wave 2 (Implementation — sequential):
├── Task 3: Implement signup + login + logout (GREEN) [deep]
├── Task 4: VIOLATION test + GAP-REPORT.md [deep]

Wave FINAL:
└── Task F1: Validate testAsvs works end-to-end [quick]
```

### Dependency Matrix

| Task | Depends On | Blocks | Wave |
|------|-----------|--------|------|
| 1 | — | 2 | 1 |
| 2 | 1 | 3 | 1 |
| 3 | 2 | 4 | 2 |
| 4 | 3 | F1 | 2 |
| F1 | 4 | — | FINAL |

---

## TODOs

- [x] 1. Init validation project + copy spec artifacts

  **What to do**:
  - **프로젝트 위치**: `/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test/` (이하 모든 명령에서 이 절대경로 사용)
  - `mkdir -p /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test`
  - Spring Boot 3.2.x 프로젝트 초기화: `build.gradle.kts`, Application.java, application.yml
  - 의존성: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, H2, RestAssured, JUnit5
  - `testAsvs` Gradle task 등록 (ax-template 패턴 재사용)
  - Gradle wrapper 생성
  - ax-template에서 **3개 파일만** 복사 (원본은 현재 repo 루트 기준):
    - `cp ../ax-template/specs/auth-asvs-l1.yaml specs/auth-asvs-l1.yaml`
    - `cp ../ax-template/contracts/auth-openapi.yaml contracts/auth-openapi.yaml`
    - `cp ../ax-template/blueprints/auth-manifest.yaml blueprints/auth-manifest.yaml`
    - (원본 절대경로: `/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template/`)
  - `./gradlew build` 성공 확인
  **Must NOT do**: ax-template에서 Java 코드 복사 금지. 코드는 빈 프로젝트.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: []

  **Parallelization**: Wave 1 | Blocks: 2 | Blocked By: —

  **References**:
  - `/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template/specs/auth-asvs-l1.yaml` — 복사 원본
  - `/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template/contracts/auth-openapi.yaml` — 복사 원본
  - `/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template/blueprints/auth-manifest.yaml` — 복사 원본

  **Acceptance Criteria**:
  - [ ] `/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test/` 디렉토리 존재
  - [ ] `cd /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test && ./gradlew build` exits 0
  - [ ] specs/, contracts/, blueprints/ 에 복사된 파일 존재
  - [ ] `testAsvs` task가 build.gradle.kts에 등록됨

  **QA Scenarios**:
  ```
  Scenario: Project compiles and spec files present
    Tool: Bash
    Steps:
      1. cd /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test && ./gradlew build 2>&1 | tail -5
      2. ls specs/auth-asvs-l1.yaml contracts/auth-openapi.yaml blueprints/auth-manifest.yaml
      3. grep "testAsvs" build.gradle.kts
    Expected Result: build 성공, 3개 파일 존재, testAsvs task 등록됨
    Evidence: .sisyphus/evidence/val-task-1-scaffold.txt
  ```

  **Commit**: YES | Message: `chore: init validation project with spec artifacts from ax-template`

- [x] 2. Write 6 ASVS tests from spec only (RED)

  **What to do**:
  - `specs/auth-asvs-l1.yaml`과 `contracts/auth-openapi.yaml`만 읽고 테스트 작성
  - **ax-template의 Java 소스 코드를 절대 보지 않음** — spec이 충분한지 검증하는 것이 목적
  - RestAssured + `@SpringBootTest(webEnvironment = RANDOM_PORT)` 사용
  - **5개 테스트 작성** (V3.3.1은 UserEntity가 필요하므로 Task 3 GREEN에서 작성):

  ```java
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
  class AvsvPasswordTest {
      @LocalServerPort int port;

      @Test @Tag("ASVS") @Tag("ASVS-V2.1.1")
      void asvs_V2_1_1_passwordMinLength12_rejectsShorter() {
          given().port(port).contentType(ContentType.JSON)
              .body(Map.of("email", "test@test.com", "password", "short11"))
          .when().post("/api/auth/email/signup")
          .then().statusCode(400);
      }
      // ... V2.1.2, V2.1.4
  }

  class AvsvRateLimitTest {
      @Test @Tag("ASVS") @Tag("ASVS-V2.2.1")
      void asvs_V2_2_1_rateLimitAfter5FailedAttemptsIn15Min() { ... }
  }

  class AvsvSessionTest {
      @Test @Tag("ASVS") @Tag("ASVS-V3.3.1")
      void asvs_V3_3_1_logoutInvalidatesSession() { ... }
  }

  class AvsvCompositionTest {
      @Test @Tag("ASVS") @Tag("ASVS-V2.1.9")
      void asvs_V2_1_9_noCompositionRulesEnforced() {
          // all lowercase, no numbers, no special chars — should be accepted if >= 12 chars
          given().port(port).contentType(ContentType.JSON)
              .body(Map.of("email", "comp@test.com", "password", "alllowercaseonly"))
          .when().post("/api/auth/email/signup")
          .then().statusCode(201);
      }
  }
  ```

  - **매 테스트 작성 시 기록**: spec이 충분했는지, 모호한 점은 무엇이었는지, manifest를 참조해야 했는지
  - 전체 테스트 실행 → **모두 FAIL** 확인 (구현이 없으므로)
  **Must NOT do**: ax-template 소스 코드 참조 금지. spec/contract/manifest 3개 파일만 참조.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 1 | Blocks: 3 | Blocked By: 1

  **References**:
  - `ax-validation-test/specs/auth-asvs-l1.yaml` — 테스트 작성의 유일한 가이드
  - `ax-validation-test/contracts/auth-openapi.yaml` — 엔드포인트/스키마 참조
  - `ax-validation-test/blueprints/auth-manifest.yaml` — 정책값 (rate limit, token expiry 등)

  **Acceptance Criteria**:
  - [ ] 5개 ASVS @Tag 테스트 존재 (V2.1.1, V2.1.2, V2.1.4, V2.1.9, V2.2.1)
  - [ ] `./gradlew testAsvs` → 5개 테스트 실행, **모두 FAIL** (RED)
  - [ ] 각 테스트가 black-box HTTP (RestAssured) 사용 (MockMvc 아님)
  - [ ] V3.3.1은 Task 3에서 UserEntity 생성 후 작성 예정 (여기서는 제외)
  - [ ] spec 해석 메모 기록됨

  **QA Scenarios**:
  ```
  Scenario: All 6 tests exist and FAIL (RED phase)
    Tool: Bash
    Steps:
      1. cd /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test && ./gradlew testAsvs 2>&1 | tail -15
      2. grep -rc '@Tag("ASVS-V' src/test/java/ | awk -F: '{s+=$2}END{print s}'
    Expected Result: BUILD FAILED, 5개 테스트 실행 + 실패 (V3.3.1은 Task 3에서 추가)
    Evidence: .sisyphus/evidence/val-task-2-red-phase.txt
  ```

  **Commit**: YES | Message: `test(red): 6 ASVS tests from spec only — all fail (no implementation)`

- [x] 3. Implement signup + login + logout (GREEN)

  **What to do**:
  - OpenAPI contract 기반으로 4개 엔드포인트 구현:
    - `POST /api/auth/email/signup` — 비밀번호 검증 (min 12, max 128, unicode 허용, 조합 규칙 없음)
    - `POST /api/auth/email/login` — JWT 발급 + rate limiting (5/15min)
    - `POST /api/auth/logout` — 세션 무효화
    - `GET /api/auth/me` — 관측 엔드포인트 (Bearer JWT 필요)
  - **Verified-user bootstrap**: 테스트에서 `UserRepository.save()`로 `emailVerified=true`인 유저를 직접 시드. verify-email 엔드포인트는 구현하지 않음. `@BeforeEach`에서 `UserEntity`를 DB에 직접 넣어 verified 상태로 생성.
  - **코드 패턴 제약 (VIOLATION 테스트를 위해 필수)**:
    - SignupRequest에 `@Size(min = 12, max = 128)` 어노테이션을 반드시 사용
    - Rate limiter에 `private static final int MAX_ATTEMPTS = 5;` 상수를 반드시 사용
    - 이 제약은 Task 4의 VIOLATION 시나리오가 `sed`로 값을 변경할 수 있게 하기 위함
  - UserEntity, UserRepository, PasswordEncoder, JwtTokenService 구현 (최소한)
  - `GET /api/auth/me` — 관측 엔드포인트 (V3.3.1 logout 검증에 필요)
  - **V3.3.1 테스트 추가**: UserEntity가 존재하는 이 시점에서 `@Tag("ASVS-V3.3.1")` 테스트 작성. `@BeforeEach`에서 `userRepository.save()`로 verified user 시드 → login → access token 획득 → logout → `/auth/me`에 같은 토큰으로 요청 → 401 기대
  - **구현 중 기록**: spec만으로 충분했는지, contract의 어떤 부분이 유용했는지, 모호했던 부분
  - `./gradlew testAsvs` → **모두 PASS** 확인
  **Must NOT do**: 4개 초과 엔드포인트 구현 금지. verify-email, refresh 등 불필요.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 2 | Blocks: 4 | Blocked By: 2

  **References**:
  - `ax-validation-test/contracts/auth-openapi.yaml` — 엔드포인트 스키마
  - `ax-validation-test/blueprints/auth-manifest.yaml` — 정책값
  - `ax-validation-test/specs/auth-asvs-l1.yaml` — 검증 기준

  **Acceptance Criteria**:
  - [ ] 4개 엔드포인트 동작 (signup, login, logout, /auth/me)
  - [ ] `./gradlew testAsvs` exits 0 — **6개** ASVS 테스트 전부 PASS (5개 from Task 2 + V3.3.1 from Task 3)
  - [ ] 구현 중 발견한 갭 기록됨

  **QA Scenarios**:
  ```
  Scenario: All 6 ASVS tests PASS (GREEN phase)
    Tool: Bash
    Steps:
      1. cd /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test && ./gradlew testAsvs 2>&1 | tail -10
    Expected Result: BUILD SUCCESSFUL, 6개 테스트 통과
    Evidence: .sisyphus/evidence/val-task-3-green-phase.txt
  ```

  **Commit**: YES | Message: `feat: signup + login + logout — 6 ASVS tests pass (GREEN)`

- [x] 4. VIOLATION test + GAP-REPORT.md

  **What to do**:
  - **VIOLATION 테스트** (피드백 루프 증명):
    1. 비밀번호 minLength를 12에서 4로 변경 → `./gradlew testAsvs` 실행 → V2.1.1 FAIL 확인
    2. Rate limit를 제거 → `./gradlew testAsvs` 실행 → V2.2.1 FAIL 확인
    3. 변경 revert
  - **GAP-REPORT.md** 작성:

  ```markdown
  # GAP REPORT — ax-template Self-Validation

  ## Summary
  - Spec files copied: 3 (specs/auth-asvs-l1.yaml, contracts/auth-openapi.yaml, blueprints/auth-manifest.yaml)
  - ASVS items tested: 6/23
  - Endpoints implemented: 3 (signup, login, logout)
  - Feedback loop: [WORKS / PARTIALLY / BROKEN]

  ## Portable ✅
  - [list what transferred cleanly]

  ## Partially Portable ⚠️
  - [list what needed interpretation]

  ## Not Portable ❌
  - [list what couldn't be used]

  ## Missing from Spec ❓
  - [list what was needed but not in spec/contract/manifest]

  ## Spec Interpretation Decisions
  - [every moment the developer had to guess or interpret ambiguity]

  ## Moments Where ax-template Source Was Needed
  - [every moment the developer wanted to look at ax-template's Java code]

  ## Recommendations for ax-template
  - [concrete improvements to make the template more useful]
  ```

  **Must NOT do**: ax-template 파일 수정 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: []

  **Parallelization**: Wave 2 | Blocks: F1 | Blocked By: 3

  **References**:
  - T2에서 기록한 spec 해석 메모
  - T3에서 기록한 구현 중 갭 메모

  **Acceptance Criteria**:
  - [ ] VIOLATION: password min 4로 변경 시 V2.1.1 FAIL 확인 (스크린샷/로그)
  - [ ] VIOLATION: rate limit 제거 시 V2.2.1 FAIL 확인
  - [ ] 변경 revert 후 testAsvs 다시 PASS
  - [ ] GAP-REPORT.md 완성 (4개 섹션 + 해석 결정 + 추천사항)

  **QA Scenarios**:
  ```
  Scenario: VIOLATION V2.1.1 — password min length weakened
    Tool: Bash
    Steps:
      1. PROJ=/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test
      2. SIGNUP_FILE=$(grep -rl "@Size" $PROJ/src/main/java/ | grep -i signup | head -1)
      3. sed -i '' 's/@Size(min = 12/@Size(min = 4/' "$SIGNUP_FILE"
      4. cd $PROJ && ./gradlew testAsvs 2>&1 | tee /tmp/val-violation-v211.txt | grep -E "FAIL|asvs_V2_1_1"
      5. sed -i '' 's/@Size(min = 4/@Size(min = 12/' "$SIGNUP_FILE"
      6. cd $PROJ && ./gradlew testAsvs 2>&1 | tail -5
    Expected Result: Step 4 — asvs_V2_1_1 FAIL. Step 6 — BUILD SUCCESSFUL
    Evidence: .sisyphus/evidence/val-task-4-violation-v211.txt

  Scenario: VIOLATION V2.2.1 — rate limit disabled
    Tool: Bash
    Steps:
      1. PROJ=/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test
      2. RATE_FILE=$(grep -rl "MAX_ATTEMPTS" $PROJ/src/main/java/ | head -1)
      3. cp "$RATE_FILE" /tmp/rate-backup.java
      4. sed -i '' 's/MAX_ATTEMPTS = 5/MAX_ATTEMPTS = 9999/' "$RATE_FILE"
      5. cd $PROJ && ./gradlew testAsvs 2>&1 | tee /tmp/val-violation-v221.txt | grep -E "FAIL|asvs_V2_2_1"
      6. cp /tmp/rate-backup.java "$RATE_FILE"
      7. cd $PROJ && ./gradlew testAsvs 2>&1 | tail -5
    Expected Result: Step 5 — asvs_V2_2_1 FAIL. Step 7 — BUILD SUCCESSFUL
    Evidence: .sisyphus/evidence/val-task-4-violation-v221.txt
  ```

  **Commit**: YES | Message: `docs: GAP-REPORT — template self-validation findings`

---

## Final Verification Wave

- [x] F1. **End-to-end validation check** — `quick`
  ```
  Tool: Bash
  Steps:
    1. cd /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test && ./gradlew testAsvs 2>&1 | tail -10
    2. cat GAP-REPORT.md | head -20
    3. grep -c "✅\|⚠️\|❌\|❓" GAP-REPORT.md
  Expected: testAsvs PASS, GAP-REPORT 존재 + 4개 카테고리 모두 사용됨
  ```

---

## Commit Strategy

| Phase | Commit | Files |
|-------|--------|-------|
| Scaffold | `chore: init validation project with spec artifacts from ax-template` | build.gradle.kts, specs/, contracts/, blueprints/ |
| RED | `test(red): 6 ASVS tests from spec only — all fail` | src/test/java/** |
| GREEN | `feat: signup + login + logout — 6 ASVS tests pass` | src/main/java/**, src/test/** |
| Report | `docs: GAP-REPORT — template self-validation findings` | GAP-REPORT.md |

---

## Success Criteria

### Verification Commands
```bash
cd /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test && ./gradlew testAsvs  # Expected: 6 ASVS tests pass
cat /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-validation-test/GAP-REPORT.md  # Expected: structured gap report
```

### Final Checklist
- [ ] 6 ASVS tests pass via `./gradlew testAsvs`
- [ ] VIOLATION test proves feedback loop works
- [ ] GAP-REPORT.md documents all findings
- [ ] Zero files modified in ax-template/
