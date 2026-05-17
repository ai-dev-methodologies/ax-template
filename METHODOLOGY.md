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
