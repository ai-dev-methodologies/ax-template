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
