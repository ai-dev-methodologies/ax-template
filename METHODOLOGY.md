# Blueprint Template Methodology

> contract-first, manifest-driven, verify-driven 템플릿을 어떤 도메인에든 적용하는 5단계 플레이북.
> 이 방법론은 ax-template의 auth blueprint에서 검증되었다.

## Overview

모든 blueprint 템플릿은 3개 spec 파일("Spec Trio")을 핵심으로 갖는다:

| # | Artifact | Format | Role |
|---|----------|--------|------|
| 1 | Compliance Spec | YAML | 검증 항목 목록. 각 항목은 @Tag 테스트와 1:1 대응 |
| 2 | API Contract | OpenAPI 3.0 | 엔드포인트의 source of truth |
| 3 | Policy Manifest | YAML | 정책값, 임계치, 설정. 구현이 참조하는 설정 문서 |

구현 코드는 이 Spec Trio의 **참조 구현**이다. 코드가 아니라 spec이 진실.

### 새 도메인은 plan-first (G6 forcing wire)

새 도메인은 코드부터 쓰지 않는다. 체인은 기계적으로 강제된다:
`/ax-scaffold <domain>` (빈 Spec Trio 스켈레톤, 각 spec에 `# TODO: Add` 마커) →
**`/ax-plan <domain>`** (interview → Spec Trio 채우기 → 1:1 RED `@Tag` 스텁) → 구현(RED→GREEN).
`spec_scaffold_unfilled_guard.sh` (hard gate [70]) 가 `# TODO: Add` 마커가 남은 spec이 하나라도
있으면 빌드를 BLOCK 한다 — domain_spec_trio / spec_item_verification_binding 두 게이트는 빈
스켈레톤을 vacuously 통과시키므로, 이 게이트가 "계획 없이 스캐폴드만 한" 도메인을 막는다.
아래 5-step (Step 1~5) 은 `/ax-plan` 이 채우는 산출물의 스키마다. 상세 산출물 목록은
`docs/NEW-DOMAIN-CHECKLIST.md` STEP 0 참조.

---

## Step 0: Public / Private 분류 (R26 — 코드 작성 전 필수)

새 도메인을 흡수하기 **전에** 불변식을 generic / fork-receiver-특화로 분류한다. ax-template은
public fork-base이므로 fork-receiver(회사·팀)의 특화·민감 정보는 트리에 들어가면 안 된다 (CLAUDE.md R26).

**SPI seam 패턴으로 경계를 강제한다:**

| 구분 | public (이 repo) | private (fork-receiver) |
|------|-----------------|----------------|
| 불변식 | generic, 외부 공개 표준·법률·RFC·EIP anchor | — |
| 외부 의존 | SPI **interface** + 결정론적 test-double | SPI **실제 구현** (벤더·체인·KYC·정산) |
| 식별자 | 회사·맥락 **0** | 회사명·전략·시크릿·고객데이터 |

**작성법:**
1. 외부 의존(체인·KYC·정산·벤더 API 등)은 도메인 안에 **interface**로 선언하고, repo에는
   결정론적 **test-double**(in-memory / allowlist)만 둔다.
2. fork-receiver는 그 interface를 자기 실체로 구현해 주입한다 (Spring `@Primary` / `@ConditionalOnMissingBean`).
3. 불변식 테스트는 test-double 위에서 binary로 성립 — 실체 없이도 correctness가 증명된다.

예 (tokenized-securities): `InvestorEligibility`(deny-by-default allowlist) / `HolderAuthorization`
(ownership) / `OnChainAnchor`(in-memory) interface는 public, 실제 ERC-3643·KYC·체인 adapter는 fork.

분류가 끝나야 Step 1로 간다. 불확실하면 generic interface로 두고 구현을 fork로 미룬다
(= "공통으로 만든 뒤 도입 후 보강").

---

## Step 1: Define Compliance Spec

**위치**: `specs/{domain}-{standard}.yaml`

도메인의 검증 기준을 YAML로 정의한다. 외부 표준이 있으면 내부 canonical 구조로 정규화한다.

### Schema Template

```yaml
# specs/{domain}-{standard}.yaml
version: "{standard-version}"
scope: "{what-this-spec-covers}"
stack: "{implementation-stack}"
items:
  - id: "{DOMAIN}-{ITEM-ID}"
    chapter: "{standard-chapter}"
    requirement: "{human-readable requirement text}"
    test_method: "{domain}_{itemId}_{description}"
    verification_type: "api_test"  # api_test | integration_test | code_review
    applicable: true               # false if N/A for current scope
    notes: "{concrete test scenario with expected HTTP status/behavior}"
    policy_ref: "blueprints/{domain}-manifest.yaml#{key}"  # optional
```

### Key Rules
- `id` format: `{DOMAIN}-{ITEM-ID}` — JUnit `@Tag` 값과 정확히 일치해야 함
- `test_method`: 테스트 메서드명과 일치 — traceability의 핵심
- `notes`: 구체적인 테스트 시나리오 기술 (모호하면 구현자가 해석해야 함 → GAP 발생)
- 정책값(임계치, 기간 등)은 spec이 아니라 manifest에 정의하고, `policy_ref`로 참조

---

## Step 2: Define API Contract

**위치**: `contracts/{domain}-openapi.yaml`

모든 엔드포인트의 계약을 OpenAPI 3.0으로 정의한다.

### Schema Template

```yaml
# contracts/{domain}-openapi.yaml
openapi: 3.0.3
info:
  title: "{project-name} {Domain} API"
  version: "0.1.0"
servers:
  - url: /api
paths:
  /{domain}/{action}:
    post:
      summary: "{action description}"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [{required-fields}]
              properties:
                # ... field definitions
      responses:
        '200':
          description: Success
        '400':
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ValidationErrorResponse'
        '401':
          description: Authentication error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

components:
  schemas:
    ErrorResponse:
      type: object
      required: [message]
      properties:
        message:
          type: string
        code:
          type: string
        timestamp:
          type: string
          format: date-time

    ValidationErrorResponse:
      type: object
      required: [message, errors]
      properties:
        message:
          type: string
        errors:
          type: array
          items:
            type: object
            properties:
              field:
                type: string
              message:
                type: string
```

### Key Rules
- `servers.url`이 base path — 엔드포인트 path는 상대 경로
- ErrorResponse + ValidationErrorResponse는 모든 도메인에서 재사용
- 계약이 source of truth — 구현이 계약에 맞춰야 함 (반대 아님)

---

## Step 3: Define Policy Manifest

**위치**: `blueprints/{domain}-manifest.yaml`

구현에 필요한 정책값, 임계치, 설정, 전략을 선언적으로 정의한다.

### Schema Template

```yaml
# blueprints/{domain}-manifest.yaml
template_id: "{domain}-blueprint"
version: "0.1.0"

# 이 템플릿의 적용 범위
when_to_use:
  - "{use case 1}"
  - "{use case 2}"

not_for:
  - "{anti-use case}"

# 정책값 (도메인별 설정)
# ... domain-specific policy sections ...

# 테스트 기준선
testing_baseline:
  unit: "{framework and approach}"
  integration: "{framework and approach}"
  e2e: "{framework and approach}"

# 검증 체크포인트
verification_checkpoints:
  - "{checkpoint 1}"
  - "{checkpoint 2}"

# 금지 사항
must_not:
  - "{prohibited pattern 1}"

# 거부 조건
reject_if:
  - "{rejection condition 1}"

# 참조 우선순위
source_precedence:
  - "{primary source}"
  - "{secondary source}"
```

### Key Rules
- `when_to_use` / `not_for`: 스코프를 명확히 잡아 scope creep 방지
- `must_not` / `reject_if`: 안티패턴을 선제적으로 차단
- 정책값은 compliance spec의 `notes`에 하드코딩하지 않고, manifest에 정의 → spec에서 `policy_ref`로 참조

---

## Step 4: Draft Portable Validation Tests

검증 테스트는 **black-box HTTP**로 작성한다. 프로젝트 내부 구조에 결합하지 않는다.

### Test Template

```java
// src/test/java/{package}/{Domain}ComplianceTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class {Domain}ComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    @Tag("{DOMAIN}")
    @Tag("{DOMAIN}-{ITEM-ID}")
    void {domain}_{itemId}_{description}() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("key", "value"))
        .when()
            .post("/api/{domain}/{action}")
        .then()
            .statusCode({expected-status});
    }
}
```

### Tag Convention
- `@Tag("{DOMAIN}")` — 도메인 전체 테스트 필터링 (e.g., `@Tag("PAYMENT")`, `@Tag("BILLING")`)
- `@Tag("{DOMAIN}-{ITEM-ID}")` — 개별 항목 추적 (e.g., `@Tag("PAYMENT-PCI-3.4")`, `@Tag("BILLING-TAX-001")`)
- Compliance spec의 `id` 값과 **정확히 일치**해야 함

### Key Rules
- `MockMvc` 금지 — black-box HTTP만 (이식성 핵심)
- `@WithMockUser` 금지 — 실제 security 파이프라인 통과
- `assertTrue(true)` 같은 빈 assertion 금지
- 테스트 데이터는 `@BeforeEach`에서 DB 직접 시딩

---

## Step 5: Configure Build Verification Loop

단일 명령으로 도메인의 전체 compliance를 검증할 수 있어야 한다.

### Gradle Task Template

```kotlin
// build.gradle.kts
tasks.register<Test>("test{Domain}") {
    useJUnitPlatform {
        includeTags("{DOMAIN}")
    }
}
```

### Verification Loop

```
RED:   테스트 작성 (spec 기반) → 구현 없으므로 FAIL
GREEN: 최소 구현 → 테스트 PASS
VIOLATION: 의도적 규칙 위반 → 테스트가 잡아냄 (피드백 루프 증명)

┌────────────────────────────┐
│ ./gradlew test{Domain}     │ ← 단일 명령
│                            │
│ PASS → 규칙 준수 확인       │
│ FAIL → 위반 항목 즉시 표시  │
└────────────────────────────┘
```

### Key Rules
- 단일 명령 (`./gradlew test{Domain}`)으로 전체 pass/fail 판정
- CI에서도 동일 명령 사용 가능
- VIOLATION 테스트: 규칙을 일부러 어겨서 테스트가 잡아내는지 확인 — 피드백 루프가 동작하는 증거

---

## Anti-Patterns

### ❌ 거버넌스 무한루프
문서만 생산하고 코드를 쓰지 않는 계획 수립. 승격 절차, 검증-위한-검증 문서.
**대신**: 코드를 먼저 쓰고, `./gradlew test{Domain}`으로 검증하고, 부족하면 spec을 보강.

### ❌ MockMvc 전용 테스트
프로젝트 패키지 구조에 결합된 테스트. 다른 프로젝트에 이식 불가.
**대신**: RestAssured black-box HTTP.

### ❌ Spec 없는 구현
compliance spec 없이 기능을 구현하면 "무엇을 검증해야 하는지" 정의가 없음.
**대신**: spec을 먼저 정의하고, 테스트를 먼저 작성.

---

## Appendix A: Worked Examples

### A.1 Auth Blueprint (origin)

auth 템플릿은 이 방법론을 최초로 적용한 사례다.

| Step | Auth에서의 적용 | 파일 |
|------|----------------|------|
| 1. Compliance Spec | OWASP ASVS L1 → 26개 항목 pinned | `specs/auth-asvs-l1.yaml` |
| 2. API Contract | 14개 엔드포인트 (email + OAuth) | `contracts/auth-openapi.yaml` |
| 3. Policy Manifest | JWT, 세션, CORS, provider 정책 | `blueprints/auth-manifest.yaml` |
| 4. Portable Tests | 26개 @Tag 테스트 (RestAssured) | `specs/portable-test-template/` |
| 5. Build Verification | `./gradlew testAsvs` | `backend/build.gradle.kts` |

**검증 결과**: 26/26 ASVS 항목 COVERED. VIOLATION 탐지 증명됨.
**자가검증**: 별도 프로젝트에서 spec만 복사 → 구현 → 피드백 루프 동작 확인 (`docs/GAP-REPORT.md` 참조)

### A.2 Rate-limit Blueprint (cross-domain generalization)

methodology가 auth-style 도메인을 넘어 protective cross-cutting concern에도 동작하는지 검증한 두 번째 사례. auth (identity) / CRUD (resource ops) 와 의도적으로 다른 axis 선택.

| Step | Rate-limit에서의 적용 | 파일 |
|------|----------------------|------|
| 1. Compliance Spec | RFC 6585 §4 + IETF draft → 4개 항목 (rejection / headers / isolation / window reset) | `specs/ratelimit-l0.yaml` |
| 2. API Contract | 1 reference endpoint `/api/ratelimit/ping` + 429 with Retry-After 헤더 schema | `contracts/ratelimit-openapi.yaml` |
| 3. Policy Manifest | window_millis, max_per_window, key_strategy, on_missing_key | `blueprints/ratelimit-manifest.yaml` |
| 4. Portable Tests | 4개 @Tag 테스트 (RestAssured, RANDOM_PORT, properties 주입) | `backend/src/test/.../ratelimit/RateLimitComplianceTest.java` |
| 5. Build Verification | `./gradlew testRateLimit` | `backend/build.gradle.kts` |

**검증 결과**: 4/4 RATELIMIT 항목 PASS. VIOLATION 탐지 증명됨 (`max_per_window: 5 → 9999` 변조 시 4/4 FAILED).
**일반화 신호**:
- spec → manifest 의 `policy_ref` 패턴이 auth와 동일하게 작동 (정책값을 spec notes 에 하드코딩하지 않음)
- RestAssured black-box 테스트가 auth와 별개 stack (Caffeine filter) 에서도 그대로 적용
- 단일 명령(`./gradlew test{Domain}`) 검증이 새 도메인에서도 즉시 성립

---

### A.3 Frontend Spec Trio (full-stack first instance)

The Frontend Spec Trio applies the backend "spec-before-code" discipline to UI surface
areas. Each frontend domain produces three schema artifacts that mirror the backend Spec
Trio one-for-one. The auth domain is the first concrete instance (produced in SP2).
(ADR: TD-2026-05-17-007)

| # | Artifact | File path | Role |
|---|----------|-----------|------|
| 1 | Page Compliance Spec | `specs/<domain>-frontend-l0.yaml` | UI verification items; each item links back to a backend spec item or a static file anchor |
| 2 | UI Contract | `contracts/<domain>-ui.yaml` | Route-level UI contract; each route binds to a backend `operationId` or a static source reference |
| 3 | UI Policy Manifest | `blueprints/<domain>-ui-manifest.yaml` | a11y rules, CWV thresholds, motion policy |

Implementation code is the **reference implementation** of this Frontend Spec Trio.
The specs, not the code, are the source of truth.

#### A.3.1 Page Compliance Spec — `specs/<domain>-frontend-l0.yaml`

```yaml
# specs/<domain>-frontend-l0.yaml
domain: <string>                          # e.g., "auth"
backend_spec_ref: <string|null>           # e.g., "specs/auth-asvs-l1.yaml"; null when frontend_only
frontend_required: true                   # marker; if false, no UI section is generated
items:
  - id: <DOMAIN-FE-NNN>                   # required, unique
    requirement: <string>                 # required, >= 20 chars
    backend_operation_id: <string|null>   # nullable. null ONLY when the page is
                                          # non-API-bound (frontend_only domain mode).
                                          # When null, the item MUST declare a non-empty
                                          # static_source_ref list.
    backend_spec_ref: <DOMAIN-NNN|null>   # required key; links to backend item; null when frontend_only
    static_source_ref: array<string>      # required when backend_operation_id is null.
                                          # Each entry MUST resolve to >= 1 existing
                                          # file in the repo (literal path OR glob
                                          # using shell-style "*"/"**" expansion).
                                          # Forbidden; guard MUST fail with exit code
                                          # non-zero and a distinct message when
                                          # backend_operation_id is non-null. This is
                                          # BINARY -- there is no soft mode.
    test_method: <playwright_test_name>   # required
    verification_type: <e2e_test|unit_test|a11y_test|cwv_test>  # required, enum-constrained
    policy_ref: <blueprints/...-ui-manifest.yaml#item>  # required
    coverage_threshold: <decimal>         # required, 0.0-1.0 -- used in trio_integrity_guard.sh
    backend_only_marker: false            # required boolean; if true, item is exempt from cross-trio
```

**Key rules:**
- `id` format: `<DOMAIN>-FE-<NNN>` — Playwright test name must match `test_method`.
- `backend_operation_id: null` is only legal when `static_source_ref` is non-empty.
  Both null simultaneously is BLOCKED by `trio_integrity_guard.sh`.
- Policy values (CWV thresholds, a11y contrast) are declared in the UI manifest and
  referenced via `policy_ref`, not hardcoded here.

#### A.3.2 UI Contract — `contracts/<domain>-ui.yaml`

```yaml
# contracts/<domain>-ui.yaml
domain: <string>
backend_contract_ref: <string|null>       # e.g., "contracts/auth-openapi.yaml";
                                          # null ONLY when the entire domain is
                                          # frontend_only (no backend OpenAPI binding).
routes:
  - path: <string>                        # e.g., "/login" or "/practices/<ruleId>"
    method: <GET|POST|PUT|DELETE>
    backend_operation_id: <string|null>   # nullable. null ONLY when accompanied by
                                          # a non-empty static_source_ref list
                                          # (frontend_only domain mode). Non-null
                                          # values MUST match an operationId in
                                          # backend_contract_ref.
    static_source_ref: array<string>      # required when backend_operation_id is null.
                                          # Each entry MUST resolve to >= 1 existing
                                          # file (literal path OR shell glob).
                                          # Forbidden; guard MUST fail with exit code
                                          # non-zero and a distinct message when
                                          # backend_operation_id is non-null. This is
                                          # BINARY -- there is no soft mode.
    params: { ... }
    query: { ... }
    states:
      loading: <slot_ref>
      error: <slot_ref>
      empty: <slot_ref|null>
    redirects:
      on_auth_required: <route_path>
      on_success: <route_path|null>
```

**Key rules:**
- `backend_contract_ref` is the source of truth for operationId resolution.
- A route with non-null `backend_operation_id` MUST NOT carry any `static_source_ref`
  entry; presence of both is BLOCKED (distinct guard error message).
- Schema-level mutual exclusion: both null and empty `static_source_ref` FAILS the guard.

#### A.3.3 UI Policy Manifest — `blueprints/<domain>-ui-manifest.yaml`

```yaml
# blueprints/<domain>-ui-manifest.yaml
domain: <string>
tokens_override: { ... }                  # design-token overrides
a11y:
  axe_rules: [<rule_id>, ...]             # required, >= 1 entry
  contrast_min: <decimal>                 # required, >= 4.5
cwv:
  lcp_ms: <int>                           # required, <= 2500
  inp_ms: <int>                           # required, <= 200
  cls: <decimal>                          # required, <= 0.1
motion:
  respect_prefers_reduced_motion: true    # required, must be true
  default_duration_ms: <int>
```

**Key rules:**
- `contrast_min: 4.5` is the WCAG 2.2 SC 1.4.3 minimum; values below 4.5 FAIL the guard.
- `cwv.*` thresholds are validated by `trio_integrity_guard.sh` at merge time.
- `respect_prefers_reduced_motion: true` is required; false or absent FAILS.

#### A.3.4 Cross-Trio integrity rule (backend <-> frontend mapping)

`trio_integrity_guard.sh` enforces that the Frontend Spec Trio is internally consistent
AND correctly anchored to the backend Spec Trio (in `full_trio` mode).

The guard is binary: `bash practices/evals/trio_integrity_guard.sh` exits 0 (PASS) or
non-zero (FAIL with a distinct machine-readable error message). No soft mode exists.

#### A.3.5 `domain_mode` enum — three classes (ADR: TD-2026-05-17-011)

```yaml
# practices/evals/trio_integrity_allowlist.yaml
schema_version: 2
domains:
  auth: full_trio       # backend + frontend Spec Trio both required
  crud: full_trio
  payment: full_trio
  practices: frontend_only    # static viewer; no backend OpenAPI binding
  ratelimit: backend_only     # no UI surface; frontend check skipped
  security: backend_only
  user: backend_only
```

| `domain_mode`    | Backend Spec Trio | Frontend Spec Trio | `backend_operation_id` | `static_source_ref` |
|------------------|-------------------|---------------------|------------------------|----------------------|
| `full_trio`      | REQUIRED          | REQUIRED            | non-null, must resolve | FORBIDDEN (guard fails if present) |
| `backend_only`   | REQUIRED          | SKIPPED entirely    | N/A                    | N/A                  |
| `frontend_only`  | NOT required      | REQUIRED            | MUST be null           | REQUIRED, non-empty, all entries resolve to >= 1 file |

**Semantics:**
- `full_trio` — applies to domains with both server invariants and user-facing UI.
  Every UI route must bind to a backend `operationId`. Coverage ratio must be 100%.
- `backend_only` — applies to backend-heavy domains with no user-facing UI.
  The frontend Spec Trio check is skipped entirely; no penalty for absent frontend files.
- `frontend_only` — applies to pure static viewers with no backend API to bind to.
  Every route and every page-compliance item must set `backend_operation_id: null`
  and supply a non-empty `static_source_ref` pointing to real files in the repo.
  Both null simultaneously is BLOCKED.

#### A.3.6 Worked example — `full_trio` (auth domain)
(ADR: TD-2026-05-17-001, TD-2026-05-17-006)

| Step | Auth frontend application | File |
|------|---------------------------|------|
| 1. Page Compliance Spec | 14 items; each `backend_spec_ref` resolves to an ASVS item | `specs/auth-frontend-l0.yaml` |
| 2. UI Contract | 14 routes; each `backend_operation_id` resolves in `contracts/auth-openapi.yaml` | `contracts/auth-ui.yaml` |
| 3. UI Policy Manifest | `contrast_min: 4.5`, `lcp_ms: 2500`, `inp_ms: 200`, `cls: 0.1`, `respect_prefers_reduced_motion: true` | `blueprints/auth-ui-manifest.yaml` |
| 4. Cross-Trio guard | `bash practices/evals/trio_integrity_guard.sh --domain auth` exits 0 | SP2 acceptance gate |

**Verification**: `bash practices/evals/trio_integrity_guard.sh --domain auth` exits 0 on auth.
TDD anchor: `frontend/tests/_fixtures/spec-trio-coverage-fail/` (deliberately-broken
fixture) causes the guard to exit 1 with `NULL_OPERATION_ID`.

#### A.3.7 Worked example — `frontend_only` (practices domain)

The `practices` domain is a static viewer that renders `practices/AGENTS.md` and
`practices/rules/**/*.md`. It has no backend API contract to bind to.

| Step | Practices frontend application | File |
|------|-------------------------------|------|
| 1. Page Compliance Spec | Items with `backend_operation_id: null` + `static_source_ref: ["practices/AGENTS.md", "practices/rules/**/*.md"]` | `specs/practices-frontend-l0.yaml` |
| 2. UI Contract | Routes with `backend_operation_id: null` + `static_source_ref` entries pointing to real files | `contracts/practices-ui.yaml` |
| 3. UI Policy Manifest | Same a11y + CWV + motion fields as `full_trio`; `backend_contract_ref: null` | `blueprints/practices-ui-manifest.yaml` |
| 4. Cross-Trio guard | `trio_integrity_guard.sh` validates `frontend_only` mode; zero-scan guard active | SP confirmed by trio guard exit 0 |

---

## Appendix B: New Template Dry-Run Checklist

새 도메인에 이 방법론을 적용할 때:

- [ ] 도메인의 외부 표준/규칙을 식별했는가? (PCI-DSS, GDPR, 사내 규정 등)
- [ ] `specs/{domain}-{standard}.yaml` — compliance spec 생성했는가?
- [ ] 각 항목에 `id`, `requirement`, `test_method`, `verification_type` 정의했는가?
- [ ] `contracts/{domain}-openapi.yaml` — API 계약 생성했는가?
- [ ] `blueprints/{domain}-manifest.yaml` — 정책 매니페스트 생성했는가?
- [ ] Spec의 `notes`에 정책값을 하드코딩하지 않고 `policy_ref`로 manifest를 참조하는가?
- [ ] 각 compliance item에 @Tag 테스트가 1:1 매핑되는가?
- [ ] 테스트가 RestAssured (black-box HTTP)를 사용하는가?
- [ ] `./gradlew test{Domain}` 한 줄로 전체 검증 가능한가?
- [ ] VIOLATION 테스트로 피드백 루프를 증명했는가?
- [ ] 자동 검증만 사용하는가? (승격 절차, 검증-위한-검증 문서 없이)

### Frontend Spec Trio additions (domain에 UI surface가 있을 때)

- [ ] `domain_mode` — `practices/evals/trio_integrity_allowlist.yaml`에 선언했는가?
      (`full_trio` / `backend_only` / `frontend_only` 중 하나)
- [ ] `specs/<domain>-frontend-l0.yaml` — page compliance spec 생성했는가?
      - 각 item: `id`(DOMAIN-FE-NNN), `requirement`(>=20자), `test_method`,
        `verification_type`, `policy_ref`, `coverage_threshold` 있는가?
      - `full_trio`: `backend_operation_id` + `backend_spec_ref` 채워져 있는가?
      - `frontend_only`: `backend_operation_id: null` + 비어있지 않은 `static_source_ref` 있는가?
- [ ] `contracts/<domain>-ui.yaml` — UI contract 생성했는가?
      - 모든 route가 `backend_operation_id`(full_trio) 또는 `static_source_ref`(frontend_only) 선언했는가?
- [ ] `blueprints/<domain>-ui-manifest.yaml` — UI policy manifest 생성했는가?
      - `a11y.contrast_min >= 4.5`, `cwv.lcp_ms <= 2500`, `cwv.inp_ms <= 200`,
        `cwv.cls <= 0.1`, `motion.respect_prefers_reduced_motion: true` 선언했는가?
- [ ] `bash practices/evals/trio_integrity_guard.sh --domain <domain>` exits 0인가?
- [ ] TDD anchor(의도적 깨진 fixture)가 guard를 exit 1으로 만드는가?
- [ ] schema mutual exclusion 검증: non-null `backend_operation_id`에 `static_source_ref` 없는가?
      both-null 이면 guard가 BLOCK하는가?

---

## Appendix C: Standard Procedure for Adding a New Domain

> The canonical 12-step playbook for adding a new domain blueprint to ax-template,
> distilled from the Payment blueprint's empirical 23-phase execution
> (`docs/blueprints/payment/plan.md`, P0..P11) and validated by the L4 sealed
> sub-agent acceptance test at commit `a2d3fac`
> (`docs/blueprints/payment/acceptance/l4-subagent-test.md` — 11/11 MUST_PASS + 6/6 SHOULD_PASS).

### When to use Appendix C

Use this procedure when adding a new domain blueprint (Notification, File upload, Audit log,
Multi-tenancy, Search, Subscription, etc.) AFTER the Spec Trio methodology (Appendix A) and
the catalog growth protocol (`practices/MAINTAINER.md`) are already in place. Appendix C is
the **integration playbook** that wires a new domain into all existing infrastructure: spec
trio, catalog, hard gates, verification scripts, blueprint docs, and the AGENTS.md sentinel.

### Pre-requisites (must already exist before starting S1)

| Pre-requisite | Verification |
|---------------|--------------|
| Spec Trio convention adopted (`specs/`, `contracts/`, `blueprints/`) | `ls specs/ contracts/ blueprints/` |
| `practices/` catalog with `_template.md` + `MAINTAINER.md` + 4 hard gates | `bash practices/evals/{spec_ref,substance,time_decay,evidence}_guard.sh` exits 0 |
| Test infrastructure (framework-native runner with tag/filter support) | At least one existing `./gradlew test{Domain}` task or `npm run test:{domain}` script |
| `verify/blueprint-completeness.sh` and `verify/cold-start-test.sh` scripts | `ls verify/` |
| `AGENTS.md` sentinel auto-regen | `practices/generate_agents.sh` exits 0 |

---

### The 12-step standard procedure

Each step maps to one or more Payment phases (P0..P11) and produces a discrete artifact
verifiable by a single command. Time estimates assume one engineer + AI assistance.

| # | Step | Maps to | Artifact | Verification | Time |
|---|------|---------|----------|--------------|------|
| **S1** | Plan + memory entry | P0 | `docs/blueprints/<domain>/{plan,progress}.md` + `memory/<domain>_blueprint_status.md` | `test -s docs/blueprints/<domain>/plan.md` | 0.5 d |
| **S2** | Seal L4 acceptance prompt + rubric | P0.5 + P1.3 | `docs/blueprints/<domain>/acceptance/{l4-sealed-prompt,l4-sealed-rubric}.md` | `grep -q "^## SEALED" docs/blueprints/<domain>/acceptance/l4-sealed-prompt.md` | 0.25 d |
| **S3** | Blueprint completeness manifest | P0.7 | `docs/blueprints/<domain>/blueprint-manifest.txt` (registers every artifact + CMD gate) | `bash verify/blueprint-completeness.sh <domain>` runs (will fail until later) | 0.25 d |
| **S4** | Cold-start file set declaration | P0.9 | Cold-start input list in `verify/cold-start-test.sh` for the new domain | `bash verify/cold-start-test.sh <domain>` runs (will fail until later) | 0.1 d |
| **S5** | Spec Trio | P1 | `specs/<domain>-l0.yaml` + `contracts/<domain>-openapi.yaml` + `blueprints/<domain>-manifest.yaml` | `npx @apidevtools/swagger-cli validate contracts/<domain>-openapi.yaml` + YAML parse | 1 d |
| **S6** | Domain-specific compliance scope (if any) | P1.25 | Compliance-framework mapping declared in manifest (e.g., `pci_dss`, `gdpr_scope`, `rbac_policy`) + `*_ref` field on relevant spec items | `grep -c "<framework>_ref" specs/<domain>-l0.yaml` | 0.25 d |
| **S7** | Generalization audit | P1.5 | `docs/blueprints/<domain>/decisions.md` — proposed-rule classification (`new_generic` / `extend_existing` / `<domain>_specific` / `reject_duplicate`) | manual review + table present | 0.25 d |
| **S8** | TDD RED | P2 | `backend/src/test/.../authblueprint/<domain>/*.java` with `@Tag("<DOMAIN>")` + `@Tag("<DOMAIN>-<FAMILY>-<NNN>")` | `cd backend && ./gradlew test<Domain>` exits NON-ZERO | 1 d |
| **S9** | TDD GREEN — baseline | P3.0 | `backend/src/main/.../authblueprint/<domain>/*.java` minimal impl | `cd backend && ./gradlew test<Domain>` exits 0 | 1–2 d |
| **S10** | Hardening — failure matrix + concurrency + invariants | P3.3 + P3.6 + P3.9 | Provider failure matrix tests, `@RepeatedTest` concurrency tests, domain invariant tests | `./gradlew test<Domain> --tests "*Matrix*" "*Concurrency*" "*Invariant*"` exit 0 | 1 d |
| **S11** | REFACTOR + java-reviewer + security-reviewer | P4 + P5 | Findings recorded in `decisions.md` + `security-review.md`; CRITICAL/HIGH fixed | `./gradlew testPractices` exit 0 + supplemental grep clean | 1 d |
| **S12** | Catalog growth + verification trifecta + L3 fork + L4 sealed + push | P6 + P7..P11 | New rules (per S7 audit) + upstream snapshots + AGENTS.md regen + `blueprint-completeness.sh <domain>` exit 0 + L3 fork sim PASS + L4 sealed sub-agent PASS | full gate suite exit 0 | 1.5–2 d |

**Total**: 7.5–10 engineering days per domain (Payment empirical baseline).

---

### Verification primitive — framework-native test runner with domain filter

The methodology requires a **single command** that returns binary pass/fail for the entire
domain's compliance. The abstract primitive is:

```
<framework-native test runner> --filter "<DOMAIN>"  →  exit 0 = compliant, non-zero = violation
```

Two concrete recipes for the two stacks ax-template currently supports.

#### Recipe A — Spring Boot (Java / Gradle)

Tests carry JUnit 5 `@Tag` annotations. Gradle registers a task that filters by tag.

```kotlin
// backend/build.gradle.kts
tasks.register<Test>("test<Domain>") {
    useJUnitPlatform { includeTags("<DOMAIN>") }
    description = "Run <Domain> blueprint compliance tests"
    group = "verification"
    shouldRunAfter("test")
}
```

```java
// backend/src/test/java/com/ax/template/authblueprint/<domain>/<Domain>ComplianceTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class <Domain>ComplianceTest {
    @LocalServerPort int port;
    @BeforeEach void setup() { RestAssured.port = port; }

    @Test
    @Tag("<DOMAIN>")
    @Tag("<DOMAIN>-<FAMILY>-<NNN>")
    void <domain>_<familyLower>_<descriptor>() {
        given().contentType(ContentType.JSON).body(...)
        .when().post("/api/<domain>/<action>")
        .then().statusCode(<expected>);
    }
}
```

**Verification**: `cd backend && ./gradlew test<Domain>` → exit 0 = GREEN.

ax-template has 4 such tasks today: `testAsvs`, `testCrud`, `testRateLimit`, `testPayment`.
The 5th domain adds `test<NewDomain>` following the same pattern.

#### Recipe B — React / Next.js (Node / npm / vitest)

Tests use vitest `describe` blocks named with the domain prefix; npm scripts expose a
filtered runner. The auth domain (`templates/L4/auth/`) is the canonical first instance of this recipe,
validated by SP2's `trio_integrity_guard.sh` acceptance gate. Every subsequent full-stack
domain blueprint (crud, payment, practices) follows this recipe.

```ts
// frontend/tests/<domain>/<domain>.compliance.test.ts
import { describe, test, expect } from 'vitest';

describe('<domain>: compliance', () => {
  test('<DOMAIN>-<FAMILY>-<NNN> <descriptor>', async () => {
    const res = await fetch('/api/<domain>/<action>', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ... }),
    });
    expect(res.status).toBe(<expected>);
  });
});
```

```json
// frontend/package.json
{
  "scripts": {
    "test": "vitest run",
    "test:<domain>": "vitest run --reporter=verbose -t '<domain>:'"
  }
}
```

**Verification**: `cd frontend && npm run test:<domain>` → exit 0 = GREEN.

For ESLint-rule blueprints in `practices-react/eslint-plugin-ax/`, the equivalent primitive is
`node --test tests/<domain>/*.test.js` filtered by file pattern instead of vitest `-t`.

#### Equivalence table

| Concern | Spring Boot | React/Next.js |
|---------|-------------|---------------|
| Test runner | JUnit 5 via Gradle | vitest via npm |
| Domain filter | `@Tag("<DOMAIN>")` + `useJUnitPlatform { includeTags(...) }` | `describe('<domain>: ...')` + `vitest -t '<domain>:'` |
| Single-command gate | `./gradlew test<Domain>` | `npm run test:<domain>` |
| Pass condition | exit 0 | exit 0 |
| Black-box HTTP | RestAssured + `RANDOM_PORT` | `fetch` against Next.js dev server or MSW |
| Spec ID convention | `<DOMAIN>-<FAMILY>-<NNN>` | identical |

The convention is **stack-agnostic**: one command, binary outcome, tag-based filter, spec ID
matches test annotation 1:1.

#### Canonical domain instances (L4 reference workloads)

| Domain | Stack | Spec Trio mode | Verified by |
|--------|-------|----------------|-------------|
| auth | full_trio | backend + frontend | SP2: `bash practices/evals/trio_integrity_guard.sh --domain auth` exit 0 |
| crud | full_trio | backend + frontend | SP9 acceptance gate |
| payment | full_trio | backend + frontend | SP10 acceptance gate |
| practices | frontend_only | frontend only | SP11 acceptance gate |

L4 domain workloads live under `templates/L4/<domain>/`. The frontend side
(`templates/L4/<domain>/app/`) follows Next.js 16 App Router conventions.
L1/L2/L3 template primitives referenced from L4 are documented in SP5–SP8.

Domain-selection guidance (which stack, when, and why) and the 12-step procedure
for wiring a new domain into the full composition kit are in **Appendix C Step-by-step
gates** and the **Composition kit hooks** table. Recipe B (this section) provides the
single-command primitive; Appendix C provides the integration playbook.
(ADR: TD-2026-05-17-009)

---

### Step-by-step gates

Each step has a single binary command. A new domain is "complete at step N" when the step's
command exits 0. Steps build on each other; a failing gate blocks the next step.

| Step | Single-command gate | Pass = |
|------|---------------------|--------|
| S1 | `test -s docs/blueprints/<domain>/plan.md && test -s docs/blueprints/<domain>/progress.md` | plan + progress non-empty |
| S2 | `grep -q "^## SEALED" docs/blueprints/<domain>/acceptance/l4-sealed-prompt.md` | sealed marker present |
| S3 | `test -s docs/blueprints/<domain>/blueprint-manifest.txt` | manifest registers artifacts |
| S4 | `bash verify/cold-start-test.sh <domain>` (runs; will fail until S5) | script accepts the domain arg |
| S5 | `npx @apidevtools/swagger-cli validate contracts/<domain>-openapi.yaml` + YAML parse on the other two | Spec Trio valid |
| S6 | `python3 -c "import yaml; d=yaml.safe_load(open('blueprints/<domain>-manifest.yaml')); assert '<framework>_ref' in d or '<scope_key>' in d"` | compliance scope declared (skip if N/A) |
| S7 | manual: `decisions.md` table covers every proposed rule with explicit classification | audit complete |
| S8 | `cd backend && ./gradlew test<Domain>` exits NON-ZERO with failures (not compile errors) | RED state |
| S9 | `cd backend && ./gradlew test<Domain>` exits 0 | GREEN state |
| S10 | `./gradlew test<Domain> --tests "*Matrix*" "*Concurrency*" "*Invariant*"` exits 0 | hardening GREEN |
| S11 | `./gradlew testPractices` exits 0 + `grep -rn "<forbidden-pattern>" backend/src/main/java/ \| wc -l` = 0 | no regression, no forbidden patterns |
| S12 | `bash verify/blueprint-completeness.sh <domain>` exits 0 + L4 rubric PASS | blueprint complete + sub-agent validated |

---

### Composition kit hooks — where a new domain plugs in

A new domain interacts with the existing catalog at these well-defined integration points.
Do not invent new locations; reuse the established convention.

| Concern | Location | Convention |
|---------|----------|------------|
| Compliance spec | `specs/<domain>-l0.yaml` | YAML list of items with `id` = `<DOMAIN>-<FAMILY>-<NNN>` |
| API contract | `contracts/<domain>-openapi.yaml` | OpenAPI 3.0; validated by `swagger-cli` |
| Policy manifest | `blueprints/<domain>-manifest.yaml` | YAML; declares thresholds, allowed values, compliance scope |
| Tests (Spring) | `backend/src/test/java/com/ax/template/authblueprint/<domain>/` | `@Tag("<DOMAIN>")` + `@Tag("<DOMAIN>-<FAMILY>-<NNN>")` |
| Impl (Spring) | `backend/src/main/java/com/ax/template/authblueprint/<domain>/` | Service + Controller + Repository + Entity per Java patterns |
| Tests (React) | `frontend/tests/<domain>/` or `practices-react/eslint-plugin-ax/tests/<domain>/` | `describe('<domain>: ...')` |
| Gradle task | `backend/build.gradle.kts` — `tasks.register<Test>("test<Domain>") { useJUnitPlatform { includeTags("<DOMAIN>") } }` | one task per domain |
| npm script | `frontend/package.json` — `"test:<domain>": "vitest run -t '<domain>:'"` | one script per domain |
| Rules (after S7 audit) | mostly extensions to existing generic rules; **avoid `practices/rules/<domain>-*.md` proliferation** | Only file under `<domain>-*` when audit classifies as `<domain>_specific` |
| Upstream snapshots | `practices/upstream/<authority>.snapshot.md` + entry in `practices/upstream/_MANIFEST.yaml` | tier 1–3 per MAINTAINER.md |
| Blueprint manifest | `docs/blueprints/<domain>/blueprint-manifest.txt` | consumed by `verify/blueprint-completeness.sh` |
| Blueprint docs | `docs/blueprints/<domain>/{plan,progress,decisions,security-review,verification-log}.md` + `acceptance/` | five files + acceptance subdir |
| Memory | `~/.claude/projects/<slug>/memory/<domain>_blueprint_status.md` | status anchor for cold-start recovery |
| AGENTS.md | auto-regenerated by `practices/generate_agents.sh` after S12 catalog growth | sha256 sentinel must match |

**Anti-pattern**: do NOT create a `<domain>-*` subdirectory under `practices/rules/`. The
catalog is flat by category, not by domain. Payment empirically produced 4 net new rules,
of which 3 went under existing generic namespaces (`api-*`, `lang-*`, `persistence-*`) and
only 1 under `payment-*` (the genuinely currency-specific rule). See
`docs/blueprints/payment/decisions.md` § Generalization Audit for the worked example.

---

### Domain-selection guidance — which stack, when, and why

ax-template currently supports two reference stacks: Spring Boot (backend/) with 4 domain
blueprints, and React/Next.js (frontend/ + practices-react/) with one ESLint-rule blueprint.
A new domain typically lives on one stack but cross-cuts both when it has UI surface area.

| Domain stack | When to use | Examples |
|--------------|-------------|----------|
| **Spring Boot only** | Backend-heavy, no user-facing UI surface, server-driven invariants dominate | Audit log, Reconciliation, Background jobs, Multi-tenancy |
| **React/Next.js only** | UI/UX-heavy, no new server-side concerns, design-system pressure | Component library, Form patterns, Accessibility blueprint |
| **Both stacks** | Domain has both server invariants AND user-facing UI; React must call new endpoints | Payment v2 with UI, Notification preferences UI, File upload with drag-drop |

#### Candidate future domains

| Domain | Stack | Rationale | Methodology stress test |
|--------|-------|-----------|------------------------|
| **Notification** (email/SMS/push) | Spring (primary), React (preferences UI optional) | Backend-heavy; idempotency reuse (`api-idempotency-key-required` from Payment), provider abstraction reuse (mock + adapter pattern) | **High** — weak external standards (no PCI-DSS-equivalent); stress-tests evidence-anchored system harder than Payment. Architect Iter 1 steelman flagged this as the canonical hard case. |
| **File upload** (S3/local) | Spring (primary), React (drag-drop UI) | Backend + minimal frontend; security-sensitive (path traversal, virus scan, signed URLs); state machine for uploads (PENDING→UPLOADING→PROCESSING→COMPLETE→FAILED) reuses `persistence-state-machine-atomic` | **Medium** — strong external standards (OWASP file upload cheat sheet, CSP). Validates state-machine rule generalization. |
| **Audit log** | Spring only | Cross-cuts ALL previous domains (auth/CRUD/ratelimit/payment); useful as 5th domain that consumes what previous 4 produced | **High** — exercises cross-domain composition; rules from 4 prior domains must compose without conflict. |
| **Multi-tenancy** | Spring (primary) | Cross-cuts persistence and security; high complexity (row-level security, tenant header propagation, isolation tests) | **Very high** — touches every existing entity/repository; will surface latent assumptions across the catalog. |
| **Search** (Elasticsearch / OpenSearch) | Spring (primary), React (UI) | Integration-heavy; demonstrates manifest provider abstraction at scale (multiple search backends); query DSL injection concerns | **Medium** — established external standards; mostly validates provider abstraction. |
| **Subscription / billing** | Spring (primary) | Extends Payment with recurring concerns; tests whether Payment blueprint's abstractions (PaymentProvider interface, idempotency, state machine) survive recurrence | **Very high** — direct stress test of Payment's generalization. If PaymentProvider survives Subscription, AI2-3 paper exercise becomes a binary test. |

**Recommended next iteration**: **Notification**. Architect Iter 1 specifically called this out as
the case that stress-tests the evidence-anchored system harder than Payment did, because
notification has no PCI-DSS-equivalent external standard to anchor against — the system must
fall back to RFC 5321 (SMTP), RFC 8030 (Web Push), and vendor docs (Twilio, FCM) for evidence.

---

### What Appendix C does NOT do — explicit non-goals

| Non-goal | Rationale |
|----------|-----------|
| Replace planning skills (`/ralplan`, `/plan`, planner agent) | Appendix C is the *execution* playbook; planning still belongs upstream. For non-trivial domains, run `/ralplan` for consensus approval before S1. |
| Mandate L4 sealed sub-agent test for EVERY domain | L4 is expensive (1 day + sealed prompt design). The first 2–3 domains added after Payment should run L4 to validate catalog discoverability; later domains may skip if the catalog has stabilized. Decision is the maintainer's. |
| Specify model tier (Opus / Sonnet / Haiku) | Model selection is per-task complexity and budget. Payment used Opus throughout for safety; routine domains may use Sonnet/Haiku for the S8–S10 implementation loop. |
| Enforce a fixed rule count after catalog growth | The S7 generalization audit determines rule count empirically (Payment yielded 4, not a quota). Do not pad the catalog to hit a number. |
| Define team workflow (PR review, branch policy, merge gates) | Per `CLAUDE.md`, ax-template does NOT enforce team policies on forks. Catalog quality is the skill's responsibility; human-collaboration policy is the fork's. |
| Cover non-Java / non-React stacks (Go, Rust, Python) | Out of scope for v1. The verification-primitive abstraction (`<framework-native test runner with domain filter>`) generalizes — when a fork ports the catalog to a new stack, the same shape applies (`go test -run` / `cargo test --` / `pytest -k`). |

---

### Evidence linked to Payment — the canonical first instance

Appendix C is not theoretical — every step in the 12-step procedure was executed end-to-end
during Payment, captured in the following artifacts (`docs/blueprints/payment/`):

- **`plan.md`** — the ralplan-approved 23-phase Payment plan that this procedure distills.
  Codex Critic Iteration 2 verdict: APPROVE (7/7 PASS A–G).
- **`decisions.md`** — the P1.5 generalization audit (S7 worked example): 5 proposed rules
  classified into 1 extension + 3 new generic + 1 `<domain>_specific` + 0 rejected. Also
  the P4 java-reviewer findings (S11 worked example): 17 findings triaged into 6 fixed /
  3 deferred / 8 documented.
- **`security-review.md`** — the P5 security-reviewer worked example (S11): 8 findings
  triaged with PCI-DSS 3.4 / 6.5 / 4.1 trace verified.
- **`verification-log.md`** — the P7..P7.7 verification trifecta output (S12): `/verification-loop`
  6-phase report + `blueprint-completeness.sh payment` 14/15 → 15/15 + `cold-start-test.sh payment` 8/8.
- **`acceptance/l3-fork-simulation.md`** — fresh clone of the repo, 5 gates all exit 0 in 191s.
  Proves the blueprint is portable to a fork-receiver with no hidden environmental dependencies.
- **`acceptance/l4-subagent-test.md`** — the L4 sealed sub-agent PASS at maximum verdict
  (11/11 MUST_PASS + 6/6 SHOULD_PASS), sub-agent commit `a2d3fac`. The empirical validation
  that the catalog is self-discoverable to a context-0 AI agent. **This is the evidence
  that Appendix C describes a procedure that actually works**, not an aspirational design.

A new domain following Appendix C should produce the same 5 documents in
`docs/blueprints/<domain>/`. If any document cannot be produced, the corresponding step did
not complete and the gate has not been crossed.

---

### Recipe D — Business pattern composition flow (via `/ax-scaffold business`)

When implementing a cross-domain business pattern (multi-tenant SaaS subscription,
e-commerce, CRM, etc.), use `/ax-scaffold business` instead of the per-domain
12-step procedure above. This subcommand composes **existing** L4 domains; it does
NOT create a new Spec Trio or a new L4 domain.

**When to use Recipe D instead of the 12-step procedure**

| Signal | Use |
|--------|-----|
| Business requires ≥2 existing L4 domains working together | Recipe D |
| Business introduces genuinely new backend/frontend surface not in any L4 | 12-step procedure (adds new domain) |
| Fork-receiver wants to enable a named pattern quickly | Recipe D |

**Composition flow (5 steps)**

| # | Step | Command | Artifact |
|---|------|---------|----------|
| **C1** | Pick pattern | Check `recipes/` for available patterns | — |
| **C2** | Dry-run | `bash skills/ax-scaffold/scripts/new-business-recipe.sh <pattern> <project> --dry-run` | Printed file plan |
| **C3** | Review scope | Confirm enabled L4 domains match project needs; add inline `override_allowed:` in `recipes/<pattern>/RECIPE.md` if adjusting | Optional: modified `override_allowed:` block |
| **C4** | Apply | `bash skills/ax-scaffold/scripts/new-business-recipe.sh <pattern> <project>` | `<project>/business-composition.yaml` + `/ax-verify-domain` exit 0 for each L4 |
| **C5** | Annotate + implement | Add `applied_recipe: <pattern>` to each enabled L4 domain README; implement business logic using L4 catalog + L2 blocks | Updated READMEs |

**Verification primitive**

```bash
bash skills/ax-scaffold/scripts/new-business-recipe.sh <pattern> <project> --dry-run
# Expected: exit 0, enabled L4 domains all ✓, L2 blocks listed
```

**Available patterns** (see `recipes/` for full recipe definitions):

| Pattern | Enabled L4 domains |
|---------|-------------------|
| `saas-subscription` | billing, auth, feature-flags, notification, audit-log |
| `e-commerce` | payment, auth, notification, audit-log, search |
| `crm` | auth, notification, audit-log, search, file-storage |

**Non-goals for Recipe D**

- Recipe D does NOT create new L4 Spec Trios.
- Recipe D does NOT register patterns in `trio_integrity_allowlist.yaml`.
- Recipe D does NOT ship business logic — only composition contracts.
- Patterns are explicitly named; there is no free-text inference (`--analyze` is not supported).

---

## Cross-cutting layers (R53 / R56 / R67 patterns)

The five-step new-domain playbook above covers the dominant case: an L4
domain with its own Spec Trio + backend + frontend trio. Some helpers
don't belong to any one L4 — they're shared infrastructure. The catalog
hosts three cross-cutting layers for these.

### When to add to L0 fork-receiver-kit (frontend)

`templates/L0/fork-receiver-kit/` hosts pure-TS helpers shared by L4
frontends — `use-caller-id.ts` (caller identity hooks), `parse-error.ts`
(RFC 9457 ProblemDetail unwrap + CodedError + PII deny-list),
`entity-key.ts` (polymorphic entity-ref guard).

**Add to L0 when**: a TS helper has been duplicated inline across 3+ L4
trios (R80 rule of three). Examples from R53:

- `useCallerId` was inline in 7 L4s → lifted to L0
- `parseError` was inline in 7 L4s → lifted to L0
- `assertSafeEntityRef` was inline in favorites only → lifted pre-
  emptively because the pattern was obviously domain-neutral

**Don't add to L0 when**: the helper has UI / JSX surface (that's L1 or
L2), or when only one L4 uses it (premature abstraction).

### When to add to L2 blocks (frontend widgets)

`templates/L2/blocks/` hosts reusable React widgets with rendering
surface — `confirm-dialog.tsx`, `rate-limit-banner.tsx`,
`offline-banner.tsx`, `announce-live.tsx`, plus 25+ others.

**Add to L2 when**: a UX pattern with concrete JSX + a11y attributes
recurs across L4 trios. Each L2 block ships with:
- frontmatter (evidence + dependencies + imports_from/forbidden)
- exported component(s)
- optional accompanying `templates/L2/_fixtures/<block>.spec.ts` static
  + runtime assertions (Playwright)

**Don't add to L2 when**: the widget is domain-specific (favorites
star button stays in `templates/L4/favorites-bookmarks/app/`), or when
it's just composition of existing L2 blocks without new behavior.

### When to add to backend `common` package (JVM)

`backend/src/main/java/com/ax/template/authblueprint/common/` hosts
shared Java helpers — `AuditPiiHelper` (PII hash + storage scrub).
R67 introduced the package after seven backend modules adopted the
helper inline.

**Add to common when**: a Java utility has been inline-duplicated or
package-private-shared across 3+ modules (R80 rule of three again).
The R67 trajectory was:
- R60: helper born inside emailoutbox package
- R62/R63/R65/R72: 6 more modules adopted via fully-qualified import
  through emailoutbox
- R67: lifted to common; class renamed (EmailPiiHelper → AuditPiiHelper)
  to reflect the now-cross-cutting scope

**Don't add to common when**: the helper has domain coupling (e.g. a
logger named for the domain; an entity-specific validator), or when
it's only a 1-2 module shared concern (defer to one of the existing
domain packages).

### Cross-layer import discipline (mechanical guards enforce)

| Layer | May import from | Must NOT import from |
|-------|-----------------|----------------------|
| L0 fork-receiver-kit | (nothing — pure TS, no deps) | L1, L2, L3, L4, app/, lib/ |
| L1 primitives | L0 | L2, L3, L4 |
| L2 blocks | L0, L1 | L3, L4 |
| L3 page templates | L0, L1, L2 | L4 |
| L4 domain verticals | L0, L1, L2, L3 | other L4 domains |
| backend common package | (nothing — leaf utility) | any per-domain package |

The L0 / common boundary is structural: L0 is for the frontend stack;
common is for the JVM stack. They're sibling concepts, not parent/child.

### Rule of three+ trigger (R80)

When the same helper appears in three modules:
1. **Lift in the same commit** — create the shared location, move
   all existing inline copies, delete the duplicates. No transition
   window where divergence is possible.
2. **OR explicit deferral with expiry** — commit message records:
   "Helper X now in three modules; deferring lift because <reason>.
   Lift trigger: <fourth adoption | dated quarter | named owner>."

Silent deferral is permanent duplication. The rule exists to catch
helpers BEFORE per-module drift accumulates.

See `practices/rules/promote-on-third-use.md` for the full rule + R80
canonical Java example, and `practices/MAINTAINER.md` for the detector
one-liner that scans for missed lifts.

## Mechanical Anchor Lifecycle Policy (R101)

R97-R100 introduced 3 distinct mechanical anchor classes. R100 P12 F10 NEW
finding flagged "class proliferation" anti-pattern + anchor retirement
review gap. This policy formalizes the taxonomy and lifecycle to guide
R102+ decisions.

### Three anchor classes (taxonomy)

| Class | Examples | Catalog touch | Fork-receiver touch | Source of truth |
|-------|----------|---------------|---------------------|-----------------|
| **hard_guard** | `wave_kickoff_ledger_guard.sh` (R97), `registry_backfill_completeness_guard.sh` (R98) | Registered in `practices/evals/run-all-guards.sh`; invoked by `verify-completion.sh` (R25 Iron Law) and pre-push hook | Cannot opt out without breaking catalog quality contract | Shell script in `practices/evals/` |
| **catalog_helper** | `pre-commit-fast-guards.sh` (R99) | Provided in `practices/scripts/`; NOT in `run-all-guards.sh` | Opt-in via `.git/hooks/pre-commit` (or Husky / lefthook); fork-receiver decides per CLAUDE.md fork-receiver-decides-git-policy principle | Shell script in `practices/scripts/` |
| **data_artifact** | `korean_law_reference_index.yaml` (R100) | YAML / JSON / Markdown reference file; no execution semantics | Read by humans (lens compliance audit) or future tools | Static file in `practices/` |

### Anchor lifecycle (per class)

| Class | Initial state | Refresh trigger | Retirement criterion |
|-------|---------------|-----------------|-----------------------|
| **hard_guard** | Permanent on land. Catalog evolution does not retire. | Bug fix (logic change) or pattern extension (new wave-specific check) — same script edited in place. | Only retire if the entire invariant it protects is removed from catalog (rare — would be a catalog regression). Wave_kickoff_ledger_guard is **permanent** as long as waves emit phase α atomic shapes. |
| **catalog_helper** | Optional from land. Fork-receivers choose adoption per release. | Speed budget adjustment, subset addition/removal, framework migration (e.g., Husky → lefthook example). | Retire when: (1) fork-receivers stop using it (zero adoption signal in fork survey), OR (2) underlying hard guards expand to cover the same gap (e.g., pre-commit hooks become catalog-mandatory), OR (3) catalog contract evolves to make the helper redundant. |
| **data_artifact** | Initial baseline at wave commit. | Manual update at each subsequent wave (or fork-receiver release) OR automated via generator script when one exists. | Retire when: (1) consumed by NO consumer (lens reorganization, audit stopped), OR (2) source-of-truth shifts to runtime computation (artifact becomes derived not authoritative), OR (3) **stale-when-generated** — if an automated generator (e.g., `build_law_index.sh` candidate) replaces manual maintenance, the manually-curated artifact lifecycle transitions to generator output (retire manual version after one wave of generator validation). |

### Principled criterion for introducing a NEW anchor class

**The 4-data-point evidence rule applies to class introduction**, not just
finding closure. Before introducing a 4th class (or beyond), the catalog
MUST demonstrate:

1. **Distinct execution semantics** — the new class CANNOT be expressed
   as a member of an existing class (e.g., DATA artifact is NOT a HELPER
   because it has no execution semantics; HELPER is NOT a hard guard
   because fork-receivers can decline).
2. **Distinct catalog/fork-receiver responsibility split** — clear ownership
   (catalog provides + enforces / catalog provides + fork-receiver decides /
   catalog provides + nobody enforces, only references).
3. **Distinct lifecycle** — the new class has retirement / refresh triggers
   not covered by existing classes.
4. **4 wave evidence** — at least 4 consecutive waves where the gap surfaced
   would have been better addressed by the new class than reusing existing
   classes. This mirrors the R97 F10 4-data-point escalation pattern.

**Auto-data artifact candidate (R101 evaluation)**: build_law_index.sh +
korean_law_reference_index.yaml synced.
- (1) Distinct semantics? Borderline — it's DATA + generator script; could
  be expressed as catalog HELPER that emits to a DATA artifact. NOT clearly
  distinct.
- (2) Distinct ownership split? Borderline — catalog provides both; fork-
  receiver runs generator at release. Similar to HELPER pattern.
- (3) Distinct lifecycle? The data artifact's lifecycle changes (manual →
  generated) but the GENERATOR's lifecycle is the same as HELPER (refresh
  / retirement same triggers).
- (4) 4 wave evidence? NO — only R100 has the artifact, R101+ may surface
  staleness.

**Verdict (R101)**: Auto-data is NOT distinct enough to introduce as a 4th
class today. The build_law_index.sh script (if introduced later) is a
catalog HELPER that emits to a DATA artifact — both classes already exist.
Anchor lifecycle table above governs the relationship. **3-class taxonomy
held; class proliferation avoided.**

### Anchor retirement review cadence

- **Quarterly** review of `catalog_helper` adoption signals (fork-receiver
  survey, GitHub issue references, last-commit-touch date).
- **At wave start** review of `data_artifact` freshness (compare current
  catalog wave_revision_seen vs newest wave). If 3+ waves behind, schedule
  refresh in the current wave's mandatory_iter1_agenda.
- **Pattern fitness check at every P12 finding** — does the proposed new
  anchor match the 4-criterion rule above? If not, document why an existing
  class suffices.

### Reference

- R97 hard_guard introduction: commit `6db3710` (wave_kickoff_ledger_guard.sh)
- R98 hard_guard expansion: commit `c1ea7f1` (registry_backfill_completeness_guard.sh)
- R99 catalog_helper introduction: commit `fb8a5de` (pre-commit-fast-guards.sh)
- R100 data_artifact introduction: commit `aa4b855` (korean_law_reference_index.yaml)
- R101 documentation contribution (this section): pre-wave commit closes
  R100 P12 F10 NEW finding via principled criterion definition rather than
  4th anchor class introduction. Pattern fitness preserved.

The catalog evolves by adding NEW mechanical artifacts only when an existing
class genuinely cannot express the need. Documentation contributions
(METHODOLOGY.md / CLAUDE.md updates) are catalog evolution too — they do
NOT introduce new mechanical classes. The principle: **mechanism for what
machines must enforce; documentation for what humans must decide.**

## Wave Shape Taxonomy (R113)

R94-R112 produced three wave shapes. R102 P12 + R112 P12 flagged whether
multi-spec waves are a new mechanical class. Per the anchor lifecycle
4-criterion rule above, they are NOT — they are the **same wave shape with
N spec files**. This section documents the shapes so future waves pick
deliberately.

### The three shapes

| Shape | Waves | Phase α | Closures | iter1 ledger | iter2 | registry backfill |
|-------|-------|---------|----------|--------------|-------|-------------------|
| **single-spec** | R94-R101 | 1 spec + 1 ledger placeholder (atomic) | 1 spec edited | 1 ledger (10 findings) | 1 iter2 | 10 persona entries + 1 review_summary |
| **dual-spec** | R102 | 2 specs + 1 ledger (atomic) | 2 specs edited (1 commit) | 1 ledger (10 findings for the WAVE) | 1 iter2 | 10 persona entries + 1 review_summary |
| **multi-spec batch** | R103-R112 | N specs + N ledger placeholders (1 batch commit) | N specs edited (1 batch commit) | N ledgers (10 findings each) | N iter2 | N×10 persona entries + N review_summary |

### Invariants across all shapes

1. **wave_kickoff_ledger_guard** pairs each spec's `R<N> phase α` marker with
   its `r<N>-iter1.yaml`. Multi-spec batch with distinct R-numbers needs one
   ledger per R-number; same R-number (dual-spec) needs one ledger for the wave.
2. **registry_backfill_completeness_guard** counts by WAVE (R-number), not by
   spec. Each terminated wave (`iter>=2` with `findings: []`) needs its
   `review_summary.R<N>` + 10 distinct P3-P12 history entries. A multi-spec
   batch covering R103-R112 produces one review_summary + 10 entries PER
   R-number.
3. **Findings are per-WAVE, not per-spec.** A dual/batch wave's 10-persona
   panel reviews all specs in the wave and emits 10 findings (1 per persona)
   covering the wave; closure commits reference the batch.

### When to use which

- **single-spec**: default; one cohesive domain (auth, payment, chat).
- **dual-spec**: two tightly-related specs landing together (pagination +
  idempotency — both HTTP-collection cross-cutting; reviewed as a pair).
- **multi-spec batch**: a themed sweep of N independent cross-cutting specs
  drafted together (R103-R112 generic catalog: caching / soft-delete /
  problem-details / optimistic-locking / health-check / resilience /
  consent / data-subject-rights / distributed-tracing). Batch commits
  (phase α / closures / ledger / iter2+registry) keep the shape legible
  while amortizing per-wave overhead. Use Python batch generation +
  `pre-commit-fast-guards` per batch + one `verify-completion` for the set.

### Quality caveat (R113 learning)

Multi-spec batch waves that draft specs via parallel agents (Workflow)
risk **anchor hallucination** — agents fabricate RFC quotes / mis-cite
section numbers. R103-R112 land with paraphrase-labeled anchors and a
follow-up quote-recovery pass (R113) re-verifies each against the live
source. **Rule: a batch-drafted spec's `Quote anchor` claims are NOT
authoritative until source-verified.** Prefer `Reference: RFC X §Y (see
source)` over a fabricated verbatim quote. Adversarial verify (a second
agent refuting each draft) catches most hallucination before land — make
it a required stage of any Workflow-drafted batch.

## Dogfood-Driven Hardening Loop (IDW / IMW)

The Spec Trio + wave methodology above *builds* the catalog. This loop
*proves and hardens* it empirically — it is how the catalog converges toward
the north star: **100% complete** (no gap forces off-template code) **+
zero-tolerance enforced** (every deviation mechanically blocked). Spec-only
authoring (prose YAML) is necessary but insufficient; only real builds reveal
whether the standardization actually holds.

### The cycle

```
IDW (Industry Dogfood Wave)
  pick a real, usable industry app slice (issue-tracker, seller-admin, …)
  → 3 personas (Junior / Senior / 특급시니어) build the SAME slice INDEPENDENTLY
    in isolated git worktrees, each USING + stress-testing the live catalog
  → synthesize the 3 builds into the empirical signal:
       • common_components   — built the same by all → promote as REAL code
       • completeness_gaps    — where the catalog lacked guidance (→ build it)
       • enforcement_gaps     — deviations the guards FAILED to block (→ guard it)
       • automation_validation — did build/test/guards actually work?
        ↓
IMW (Improvement Wave) — close what IDW found, each sub-wave verified + pushed:
       • -A enforcement   : re-scope / add guards so deviations are blocked
       • -B completeness  : ship REAL reusable code (prose→code) under common/
       • -C guards        : new mechanical guards (calibrated GREEN-on-current)
       • -D… : fix surfaced existing violations (with their test updates)
        ↓
next IDW on the HARDENED catalog → validate the IMW worked (personas now REUSE
  the new helpers instead of hand-rolling; new guards guide them) + find the
  next gap round → loop until gaps→0 and every deviation is blocked.
```

### Rules that make the signal trustworthy

1. **3 independent personas, isolated worktrees.** Convergence (all 3 build the
   same shape) is strong evidence a pattern is canonical + promotable. Divergence
   marks a missing canonical rule. Worktrees prevent cross-contamination AND let
   each build self-verify its own domain test before reporting.
2. **Capture enforcement gaps explicitly.** Each persona records deviations it
   tried and whether a guard/test caught them (`was_caught`). An off-template move
   that passed silently is the highest-value finding — it becomes an IMW-C guard.
3. **Guards calibrate GREEN-on-current.** A new guard must pass on the existing
   tree (no false positives) while catching the found deviation. If GREEN needs
   fixing existing code, do NOT weaken the guard — fix the code (IMW-D) or
   allowlist documented debt with a retirement task.
4. **Backend-only for now.** Per the React+Spring north star, dogfood the Spring
   Boot backend first; the React frontend is a deferred second target within the
   same stack (do not branch to other backend languages until the contract layer
   is frozen — see project memory).
5. **Integrate from the worktree, not the agent's returned text.** Workflow
   subagents self-verify in their worktree (compiled, GREEN); copy the actual
   files from the worktree on integration — returned `changed_files` content can
   be serialization-corrupted (a 295-line build.gradle.kts once collapsed to one).
6. **Never run a spec-/code-drafting Workflow concurrently with `verify-completion`.**
   Subagents mutate the working tree; a concurrent verify scans it and spuriously
   FAILs. Serialize: build → integrate → verify → push → next.
7. **Resource discipline.** Cap concurrent worktree gradle builds at ~3 (a proven
   safe oversubscription on a 16-core host with zero swap); batch larger fan-outs.
   Watch swap (the real OOM signal), not transient load spikes.

### Artifacts

- `docs/NEW-DOMAIN-CHECKLIST.md` — the single-entry scaffold a new domain follows
  (the IDW1 "had to reverse-engineer the artifact set" gap closure).
- `docs/dogfood-ledger/<idw>-…md` — per-IDW findings + the prioritized IMW backlog.
- The IMW sub-waves land as ordinary verified+pushed commits; the next IDW is the
  regression test that the improvement actually took.

## Test Context Isolation — the ContextCache lever (R22)

A failure mode that recurs every time the catalog grows a new `@SpringBootTest`
class, costs an afternoon to diagnose, and is invisible to per-domain test runs.
Codified here so a fork-receiver inherits the fix instead of re-discovering it.

### The hazard

Spring's TestContext framework caches loaded `ApplicationContext`s in an LRU keyed
by the test's context configuration, capped by default at **32**
(`spring.test.context.cache.maxSize`). Once the suite boots more than 32 *distinct*
context configurations, the LRU starts evicting. An evicted context has its
singletons torn down — including the Hikari pool — so a **sibling** test class that
was relying on a cached context can find it shut down before its own methods run.
The symptom is bizarre: a class that passes in isolation, and passes under its own
`./gradlew test{Domain}` task, fails (often `UndeclaredThrowableException` at an SPI
proxy, or a dead-port `401`) only in the **full `./gradlew test` aggregate** — and
the *trigger* is some unrelated, newly-added `@SpringBootTest` that tipped the cache
past its cap. The failing class and the culprit class have nothing to do with each
other; that is what makes it expensive.

### The lever

Annotate the affected class with
`@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)` (or `AFTER_CLASS`). That forces
a fresh context boot for the class and removes it from cache reuse, so eviction can no
longer pull the rug out from under it. It is a one-annotation, **production-neutral**
change (the test's behavior is identical; it just no longer shares a cached context).
Applied across this catalog to `BillingFlowIT`, `FeatureFlagFlowIT`,
`ApiKeyComplianceTest`, `I18nPolicyComplianceTest`, `ReportExportComplianceTest`,
`SessionRevocationCheckTest`, and the `RealtimePolicyComplianceTest` whose addition
triggered the latest eviction.

**Apply it to the victim, not the trigger.** Stash-test to confirm which class
actually fails, and harden *that* class. Annotating the newly-added trigger is often
insufficient (it does not change which sibling gets evicted); the victim is the one
that must stop relying on a cacheable context.

### The mechanical contract

`practices/evals/randomport_contextcache_dirtiescontext_guard.sh` (run-all-guards
[62]) makes the lesson binary: **any backend test source that NAMES the ContextCache
hazard (contains the literal token `ContextCache`) MUST also carry `@DirtiesContext`.**
Naming the hazard in a comment without mitigating it is the exact regression that
returns the spurious aggregate failures, so the guard refuses the push. The contract
is self-describing and generalizes — a fork-receiver whose suite outgrows the cache cap
documents the hazard once and the guard enforces the mitigation from then on.

Reference: Spring Framework Reference — [Context Caching](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html)
· [@DirtiesContext](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-dirtiescontext.html).

## Promoting a backend-only cross-cutting policy domain (future_add → selectable)

A *cross-cutting policy domain* is an L4 that enforces a server-side concern with **no
first-class UI** — tenancy, locale/i18n, rate limiting, realtime channel policy. Unlike
a feature vertical it ships as a filter / interceptor / AOP advice / config that every
other L4 composes with, so it is registered `domain_mode: backend_only` and deliberately
ships no `templates/L4/<domain>/app/`. This playbook is the exact sequence run four times
this catalog cycle — **multi-tenant, i18n-policy, ratelimit, realtime-policy** — to move
such a domain from the `future_add` tier (a reserved enum slot) to `selectable` (a
discoverable, recipe-adoptable L4).

### The sequence — every step is already mechanically enforced

This is a worked variant of Appendix C's 12-step procedure. What makes it safe is that
**no step relies on reviewer memory**: each lands an artifact that an existing hard guard
refuses to let drift. The playbook is therefore an *index over enforcement*, not a new
checklist to police — and that is why this wave added **no new guard** (a "promotion
completeness" guard would only duplicate the seven below).

1. **Spec Trio, backend-only.** Author `specs/<domain>-l0.yaml` with
   `domain_mode: backend_only`, plus `contracts/<domain>-openapi.yaml` and
   `blueprints/<domain>-manifest.yaml`. The manifest holds the tunable policy values so a
   fork-receiver configures, never hardcodes.
   → *enforced by* `spec_ref_guard.sh` (R88 item-id matching) + `l4_frontend_domain_mode_guard.sh` [41]
   (if an `app/` ever appears for a `backend_only` spec, the push is BLOCKED).
2. **Reference backend** under `backend/src/main/java/.../<domain>/` — the filter/AOP/
   interceptor + supporting beans. Additive only; it composes with the existing security
   chain and the other cross-cutting runtimes (e.g. tenancy) by reference, never by rewrite.
   → *enforced by* `controller_repository_shell_guard.sh` + the layering/entity guards.
3. **`@Tag`-tagged compliance test** (`<Domain>ComplianceTest`, RestAssured black-box) +
   a per-domain Gradle task `tasks.register<Test>("test<Domain>") { includeTags("<TAG>") }`.
   → *enforced by* `verification_checklist_task_coverage_guard.sh` [58] — the new
   `test<Domain>` task MUST also be listed in `practices/verification-checklist.yaml`, so
   it joins the verify-completion hard gate, not just the ad-hoc developer run.
   (Heed the **ContextCache lever** appendix above: a new RANDOM_PORT `@SpringBootTest`
   can tip the cap-32 cache and need `@DirtiesContext` — guard [62] enforces the mitigation.)
4. **Tier move** in `specs/l4-domain-classification.yaml`: `future_add` → `selectable`,
   and create `templates/L4/<domain>/` on disk (README only — no `app/`).
   → *enforced by* `l4_domain_enum_sync_guard.sh` [19] — validates 3-source coherence
   (disk dirs / schema enum / recipe lists) against the classification, so a tier move
   without the matching disk dir (or vice-versa) BLOCKS.
5. **Fork-receiver README** at `templates/L4/<domain>/README.md` declaring
   `**Tenant model**:` (citing `specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`)
   and the composition contract (how to wire the filter, what to replace for multi-tenant).
   → *enforced by* `l4_readme_tenant_model_declaration_guard.sh`.
6. **`backend_only` registration** in `practices/evals/trio_integrity_allowlist.yaml`.
   → *enforced by* `trio_integrity_guard.sh` — skips the frontend-trio check for the
   domain and refuses an unlisted on-disk L4.
7. **Sentinel + headline sync.** Re-run `practices/generate_agents.sh` (its inline assert
   bumps the L4 count and regenerates `practices/AGENTS.md`); bump the L4-domain count in
   the README hero + CLAUDE.md vision line; add the `IMPLEMENTATION-STATUS.md` row.
   → *enforced by* `agents_md_toc_disk_truth_guard.sh` (re-runs the generator + diffs) +
   `doc_headline_count_guard.sh` [60] (headline L4 count MUST equal disk).

### Definition of done

`./gradlew test<Domain>` GREEN · `run-all-guards.sh` all-PASS · the seven guards above
each green · `verify-completion.sh` exit 0 · pushed. Because every artifact is guarded,
"done" is binary and survives a fork: a receiver who edits one half of the promotion
(drops the README line, forgets the allowlist entry, skews a headline count) gets a
BLOCKED push, not a silent half-promotion.

### Frontend is a separate, later decision

`backend_only` is not a downgrade — SSE/i18n/tenancy/rate-limit have no first-class UI by
design (the client-facing surface, when any, is an L2 block such as
`templates/L2/blocks/rate-limit-banner.tsx`). If a fork-receiver later wants a UI, that is
a distinct `domain_mode` change (`backend_only` → `full_trio`) gated by the **Frontend Spec
Trio additions** in Appendix B, not part of this promotion.

## Adversarial re-verification of sub-agent output (green ≠ correct)

When a sub-agent delivers a substantial change (an L4 lift, a domain backend, a
multi-file refactor) it almost always reports its own gate GREEN — build passes,
`./gradlew test<Domain>` passes, `run-all-guards.sh` all-PASS. **Do not commit on that
signal alone.** A passing suite proves the cases it encodes; it does not prove the
absence of plausible-but-wrong logic. Before trusting sub-agent output, run a
**read-only, refute-by-default adversarial review**, fix the survivors, and only then
commit.

### Why a green self-verify is not enough — this session's evidence

Two sub-agent deliverables this cycle passed their full self-verify and were still
wrong; the adversarial pass caught real defects the green suite missed:

- **realtime-policy** (sub-agent reported testRealtime GREEN + run-all-guards 104/0):
  the review found (1) a **metric-cardinality DoS** — an unbounded `{topic}` path
  variable became an unbounded Micrometer label, (2) a **disconnect-reason CAS race**
  between the backpressure path and the drain-error path double-counting the
  active-subscriber gauge, (3) a hardcoded tenant scope that would silently cross-tenant
  leak in a multi-tenant fork. All three shipped *green*.
- **IMW6 data-subject-rights** (sub-agent reported 15/15 GREEN): the review found the
  restriction gate **failing OPEN** on access + erase (consulted only on
  rectify/portability), a gate evaluated *after* format-validation instead of before, and
  an erasure idempotency hole that re-collected on re-request. Three real bugs behind a
  green 15/15.

In both cases the tests passed because they encoded the happy path; the bugs lived in
the seams the author did not think to test — exactly what an adversary looks for.

### The protocol

1. **Sub-agent builds + self-verifies** to GREEN and reports — but does NOT commit/push.
2. **Independent adversarial review** (a second agent, or a `Workflow` of parallel
   skeptics) reads the diff **read-only** with a refute-by-default stance: each reviewer's
   job is to find a way the change is wrong, and to **default to "defective" when
   uncertain** rather than rubber-stamping. Give diverse lenses (correctness, security,
   concurrency, does-it-actually-reproduce) rather than N identical passes.
3. **Triage**: keep only findings that survive scrutiny (an over-eager refutation that is
   itself wrong is discarded — the review is adversarial in both directions).
4. **Fix the survivors** + add a regression test that locks each one, so the next green is
   meaningfully greener.
5. **Then** commit → `verify-completion.sh` → push.

This is a process discipline, not a mechanical guard (you cannot grep for "did someone
think hard enough"). It is cheap relative to what it prevents: every defect above would
otherwise have been a shipped, green, *catalog-endorsed* bug — the worst kind, because the
catalog's whole promise is "inside the rules = safe". Apply it to any sub-agent-produced
substantive change before merge; skip it only for trivial mechanical edits a human can
fully read in one pass.

## Non-Vacuity / Hollow-Test Enforcement

### Thesis

Green ≠ correct. A test MUST fail if the correctness gate it nominally covers is
removed or flipped. A test that passes even when the gate is deleted is
*vacuously passing* (green-but-hollow): it encodes a scenario but proves nothing
about the gate's presence. The goal is to make catching this mechanical, not
dependent on a human remembering to dispatch a reviewer.

The adversarial re-verification section above addresses the *process* side: a
second agent reads the diff with a refute-by-default stance. This section
addresses the *structural* side: defining which seam classes recur, which are
mechanically detectable, and what operating rules close each class before commit.

---

### The three seam classes

**Type A — two-sided invariant.** A conservation or balance test that asserts only
ONE side of a two-sided effect. Example: a fund lock-up rejection test that checks
the recipient's balance but not the sender's. A mutant that silently debits the
sender still passes because no assertion covers the sender's post-state. Rule:
every conservation invariant must assert BOTH sides.

**Type B — exemption carve-out.** An exemption branch in a rule or check that has
zero tests — implemented but unproven. Example: an issuer holding-limit that
exempts a specific entity class. If the exemption clause is deleted, no test
breaks. Rule: every declared exemption path needs ≥ 1 test that exercises it and
confirms the exempted entity is NOT blocked.

**Type C — fail-closed default.** The deny-by-default branch in a fail-closed SPI
or conditional is untested. Example: a restriction gate that returns `orElse(false)`
when no row exists — tested only on the "row exists" path. Flipping to
`orElse(true)` (failing open) breaks no existing test. Rule: every fail-closed
default needs ≥ 1 test driving the empty / missing-row branch to confirm denial.

---

### Mechanizable vs judgment (honest split)

Mutation testing (PIT) mechanically catches all three seam classes **for code that
already exists**: a SURVIVED mutant is the binary signal that "delete or flip this
gate, nothing fails." PIT reports the surviving mutant and the covering test set;
the missing assertion is locatable.

However, a **missing feature** — a carve-out or fail-closed clause that was never
coded — has no bytecode to mutate. PIT cannot surface what is absent. Only a
spec-reading adversarial review (ralph Step-7, described below) catches the
missing-feature residue: for each spec clause declaring a carve-out or fail-closed
behaviour, check whether it is implemented AND whether it is tested.

These two methods are complementary, not redundant. Neither subsumes the other.

---

### Operating rules

1. Assert BOTH sides of every conservation invariant (credit AND debit, accept AND
   reject, producer AND consumer). A test encoding only one side of a two-sided
   effect is a Type A gap by definition.
2. Provide ≥ 1 test per declared exemption path — testing that an exempted entity
   is NOT blocked by the rule that blocks non-exempt entities.
3. Provide ≥ 1 test driving the empty / missing-row branch of every fail-closed
   default — testing that absence of a permitting record produces denial, not
   permission.

---

### `vacuity_class → kill_mutator` consistency table

| `vacuity_class`        | `kill_mutator` (PIT operator)                   |
|------------------------|-------------------------------------------------|
| `fail_closed_default`  | `TRUE_RETURNS` / `FALSE_RETURNS`                |
| `two_sided_invariant`  | `VOID_METHOD_CALLS` / `NEGATE_CONDITIONALS`     |
| `exemption_carveout`   | `REMOVE_CONDITIONALS`                           |

A SURVIVED mutant of the listed operator class in a gate that is declared with the
corresponding `vacuity_class` is a confirmed hollow test — it must be fixed before
merge.

---

### Pipeline

```
ralplan Critic
  → declares vacuity_class for every new carve-out / fail-closed SPI in the plan
      ↓
R25 vacuity-nonvacuity step
  → proves the declared kill_mutator is KILLED (binary: not survived = PASS)
      ↓
ralph Step-7 adversarial-non-vacuity reviewer
  → covers the missing-feature residue: reads the spec, names any clause with
    zero corresponding test (PIT cannot reach what is absent)
      ↓
advisory mutation sweep (periodic, outside R25 hot path)
  → discovers undeclared gates that survived mutation without a prior vacuity_class
    annotation — surfaces Type A/B/C gaps author did not self-declare
```

---

### ralph Step-7 adversarial-non-vacuity reviewer role

> Refute-by-default. For each correctness gate under test: mentally delete or flip
> it (sender side-effect, exemption carve-out, fail-closed empty/missing-row branch,
> `orElse(false)→true`); if no assertion fails on the mutant, REJECT with the mutant
> and the missing assertion. THEN read the SPEC: for every carve-out and fail-closed
> clause, confirm it is both implemented AND tested; name any spec clause with zero
> corresponding test. APPROVE only when every gate has a test that would fail if the
> gate were removed AND every spec clause has a non-vacuous test.

This role fires inside the ralph Step-7 slot and is additive to, not a replacement
for, the broader adversarial review described in the preceding section.

---

### Honest limits

- **Mutation only tests code that exists.** PIT cannot surface a missing exemption
  clause or a missing fail-closed branch. The spec-reading adversarial pass is the
  only backstop for the missing-feature residue.
- **`vacuity_class` is author-declared.** Under-declaration leaves gaps. The
  advisory mutation sweep discovers undeclared gaps, but runs outside the R25 hot
  path and only covers code that already exists.
- **The vacuity guard itself must ship a self-proof fixture.** A guard that only
  asserts "the keyword `vacuity_class` appears somewhere" is itself a vacuous check
  — the same trap it purports to catch. The fixture must demonstrate that a
  concrete SURVIVED mutant is BLOCKED, not merely that a label is present.
- **PIT "killed" ≠ "right assertion".** A mutant can be killed by a test that
  asserts the wrong thing at the right time. Kill coverage is a floor, not a
  ceiling.
- **Flakiness produces false kills.** Run mutation on the fast unit/mock slice only
  — never on `@SpringBootTest` integration tests. Flaky IT timings corrupt kill
  scores, masking real survivals with noise-driven apparent kills.
- **The ralph socket fires only for ralph-driven work.** Inline or ad-hoc agent
  sub-tasks that do not route through the ralph loop do not receive the Step-7 pass
  automatically.
- **DROP any "review-was-logged" ledger guard.** A record-exists check that claims
  to prove non-vacuity is itself the vacuity it purports to catch: it verifies that
  a log entry was written, not that any gate was meaningfully tested.

## Catalog currency — keeping implementation, templates, and rules up to date

Every rule anchors to an external source (Spring docs / React-Next docs / RFC / JEP /
OWASP), snapshotted under `practices/upstream/` (61 snapshots) and
`practices-react/upstream/` (42 snapshots), each recorded in `_MANIFEST.yaml` with
`source` URL + `fetched_at` + `sha` + `bytes`. Upstream evolves (Spring Boot minors,
React/Next majors, RFC/JEP revisions); if the snapshots are never refreshed, a rule's
evidence silently rots and the "inside the rules = safe" promise weakens. This is the
procedure for keeping the catalog **current** — refreshing what exists, NOT adding new
rules.

### The mechanism already enforces the floor — do not reinvent it

- **`time_decay_guard.sh`** (drift-axis hard gate) BLOCKS the build when any snapshot's
  `fetched_at` is older than `TIME_DECAY_THRESHOLD_DAYS` (default **90**). It is the
  staleness tripwire — run per catalog: `bash practices/evals/time_decay_guard.sh
  --catalog practices` / `--catalog practices-react`.
- **`evidence_guard.sh`** re-validates that every rule's `evidence` block still resolves
  (quoted substring present in the named snapshot section; no placeholder).
- `_MANIFEST.yaml` is the snapshot registry the two guards read.

The guards detect *that* the catalog is stale; the steps below are *what to do* when they
trip — the previously-undocumented half.

### The refresh procedure (per stale snapshot, or on a known upstream release)

1. **Detect.** `time_decay_guard` flags snapshots past 90d; additionally watch for upstream
   *events* (a Spring Boot minor, a React/Next major, a new/updated RFC·JEP·OWASP doc a
   rule cites) and refresh proactively rather than waiting for the 90d floor.
2. **Re-fetch.** Pull the snapshot's `source` URL fresh; recompute `sha` + `bytes`.
3. **Diff.** If `sha` is unchanged → the content is stable; just bump `fetched_at` (re-anchor
   the date) and you are done. If `sha` changed → diff old vs new snapshot content.
4. **Re-anchor evidence.** For every rule whose `evidence.upstream_id` points at the changed
   snapshot, confirm its quoted substring + `section` still exist in the new content.
   - quote moved/reworded but guidance same → update the rule's `quote`/`section`;
   - upstream *guidance itself* changed (new default, deprecated API, version bump) → update
     the rule body **and** its reference implementation **and** its `@Tag` test together, so
     the rule, the code, and the test stay in lockstep (never refresh the doc quote while
     leaving a now-contradicted impl).
5. **Re-verify.** `evidence_guard` (quote resolves) + the rule's `@Tag` test / guard +
   `run-all-guards.sh` + `verify-completion.sh`.
6. **Land.** Commit the `_MANIFEST.yaml` delta (`fetched_at`/`sha`/`bytes`) together with any
   rule/impl/test edits in one wave; push. Mirror the same loop for `practices-react/upstream`.

### Cadence + scope

- The 90-day `time_decay` threshold is the **hard cadence floor**; run a **proactive
  quarterly sweep** plus **event-driven** refreshes on major upstream releases.
- This procedure refreshes **existing** rules/impl/snapshots for currency. It is distinct
  from adding a new rule/domain (METHODOLOGY 5-step) — currency is maintenance, not growth.
- **No new guard is added**: `time_decay_guard` + `evidence_guard` already enforce the
  staleness and evidence-validity invariants mechanically; this appendix is the human/agent
  response procedure when they fire, anchored to those two guards.

## Adding a new backend language / stack (NestJS / Kotlin / Go / …)

ax-template's Java/Spring catalog was the FIRST stack; the same pattern extends to other
backend languages. The catalog system is already **multi-catalog** — `practices-react/` is
a second rules catalog with its own `rules/` + `upstream/` + `evals/` + `AGENTS.md`
sentinel, wired into the SAME hard gates via the `--catalog` flag (e.g.
`evidence_guard.sh --catalog practices-react`). A new backend language follows that proven
precedent; this is the procedure + the invariants it must satisfy.

### When (timing)

Only after stack #1 (Spring Boot) reaches the dual north-star property (enforcement +
completeness) AND `specs/` is frozen v1. Adding stack #2 while stack #1 is still converging
(per the current verification verdict, it is) splits effort and destabilizes the shared
contract. Until then a new stack is **record-only** (note the intent, don't scaffold).

### What is SHARED (stack-neutral — never duplicated per language)

- `specs/` (compliance contracts) + `contracts/` (OpenAPI) + `blueprints/` (policy
  manifests) — language-agnostic.
- the frontend layer (`practices-react/` + `templates/` + `frontend/`).
- `METHODOLOGY.md` (the 5-step + these appendices), the verification harness, the dogfood apps.

Keep `specs/` strictly stack-neutral NOW so a later stack is purely additive
(`backend-<lang>/` + `practices-<lang>/`) without touching the shared contract/frontend layers.

### What is RE-AUTHORED per language

- `backend-<lang>/` — reference implementation of the SAME domains against the shared specs.
- `practices-<lang>/` — framework rules, mirroring `practices/` layout
  (`rules/` + `upstream/` + `evals/` + `AGENTS.md` + `generate_agents.sh`).
- enforcement toolchain — the language's binary per-domain verification (the equivalent of
  `./gradlew test{Domain}`) plus the 4 hard gates run with `--catalog practices-<lang>`.

### The invariants the new stack MUST satisfy (same bar as stack #1)

- Same **4 hard gates** (`spec_ref` / `substance` / `time_decay` / `evidence`) on
  `practices-<lang>` — they already take `--catalog`, so NO new gate code.
- Same **Spec Trio discipline** (no spec, no merge).
- Same **per-domain binary verification** + ViolationProof tests.
- Same **AGENTS.md sentinel** (sha-anchored) + the doc-truth headline guard extended to the
  new catalog's counts.
- Same **dual-property target**, proven by re-running the SAME industry dogfoods on the new
  stack (not a fresh proof method — the existing one transfers).

### Procedure

1. Freeze `specs/` v1.
2. Scaffold `backend-<lang>/` + `practices-<lang>/` (mirror `practices/` layout).
3. Author framework rules in `practices-<lang>` with `evidence` anchored to that stack's
   upstream (snapshotted under `practices-<lang>/upstream/`).
4. Build the reference impl for the same domains in `backend-<lang>/` against the shared specs.
5. Wire the enforcement toolchain; extend `run-all-guards.sh` (`--catalog practices-<lang>`)
   and the doc-truth headline counts.
6. Re-run the industry dogfoods (B2B tracker / e-commerce / food delivery / EMR) on the new
   stack → prove dual-property holds for stack #2.
7. Update `specs/l4-domain-classification.yaml` + headline docs to advertise N stacks — both
   **equal active partners**, never archive stack #1.

### Effort + no new guard

Roughly **20–40% of stack #1**: methodology, contracts, apps, and the frontend all transfer;
only the framework rules + enforcement toolchain + reference impl are re-authored. **No new
gate is added** — the 4 hard gates + `run-all-guards.sh` already parameterize by `--catalog`
(`practices-react` is the living precedent); onboarding a stack extends them, it does not add
a gate.

