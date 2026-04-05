# ax-template Auth Blueprint Execution

## TL;DR
> **Summary**: `ax-template`에 React + Spring Boot 인증/보안 블루프린트의 최소 수직 슬라이스를 구현한다. 코드 뼈대만 만드는 것이 아니라 OpenAPI 계약, manifest 기반 verify, 테스트 4축, 운영 롤백 기준까지 함께 고정한다.
> **Deliverables**:
> - pinned version baseline
> - `contracts/auth-openapi.yaml`
> - `blueprints/auth-manifest.yaml` + quality baseline
> - `frontend/` + `backend/` auth minimal slice
> - `verify/` scripts and fixtures
> - test 4축 baseline
> **Effort**: Medium
> **Parallel**: YES - 3 waves
> **Critical Path**: pinned versions → OpenAPI contract → backend auth core → frontend auth state → verify/test convergence

## Context
### Original Request
`ax-template`를 기준으로 auth blueprint를 구현 가능한 수준까지 계획하고, scattered BP 자료를 canonical template governance와 execution flow로 묶는다.

### Interview Summary
- 1차 구매자는 약 20명 규모 SI 회사 CEO다.
- 제품은 AI coding tool이 아니라 auth/security blueprint + governance layer로 판다.
- V1은 새 프로젝트용이다.
- React + Spring Boot 단일 repo를 기준으로 한다.
- 구글 + 카카오 + 이메일을 지원한다.
- verify loop와 checklist 강제가 핵심 차별점이다.

### Metis Review (gaps addressed)
- access/refresh 전달 방식을 명시적으로 고정했다.
- OpenAPI를 schema-first로 고정했다.
- CSRF/CORS, rate limiting, local mock 전략을 계획에 포함했다.
- account linking, refresh race, fallback UX를 acceptance criteria로 끌어올렸다.

## Work Objectives
### Core Objective
실제로 작동하고 검증 가능한 auth blueprint 최소 수직 슬라이스를 `ax-template` 안에 만든다.

### Deliverables
- Java/Node/tooling pinned version 문서
- `contracts/auth-openapi.yaml`
- `blueprints/auth-manifest.yaml`
- `blueprints/auth-checklist.md`
- backend auth core skeleton + security baseline
- frontend auth UI/state skeleton
- `verify/` scripts + fixtures
- Spring integration / React auth-state / verify / key-flow E2E 테스트 baseline
- provider flag / rollback / KPI 문서화

### Definition of Done
- `auth-openapi.yaml`이 source of truth로 존재한다.
- backend와 frontend가 계약 계층에 맞춰 최소 흐름을 공유한다.
- verify가 보안/계약/RBAC 위반을 즉시 실패시킨다.
- 테스트 4축이 모두 생성된다.
- ACTIVE-LOOP 기준으로 `draft -> curated` 진입 가능한 evidence가 생긴다.

### Must Have
- schema-first OpenAPI
- Spring Security built-in JWT flow
- stateful refresh token
- explicit account linking
- CSRF/CORS default included
- refresh mutex/queue
- rate limiting on login/resend endpoints
- local mock strategy for OAuth/email

### Must NOT Have
- custom JWT filter 중심 구조
- stateless refresh token
- fail-open freshness or verify
- real OAuth browser E2E를 V1 필수로 요구
- provider/platform abstraction 과설계
- business logic in controllers

## Verification Strategy
> ZERO HUMAN INTERVENTION — all verification is agent-executed.
- Test decision: tests-after but same plan scope, framework baseline defined before coding
- Backend: Spring integration tests + security tests
- Frontend: React auth-state tests
- Verify: golden / violation / false-positive tests
- E2E: 핵심 로그인 경로만 mock-first
- Evidence: `ax-template/verify/fixtures/`, test logs, CI workflow logs

## Execution Strategy
### Parallel Execution Waves
Wave 1: foundations
- pinned version baseline
- contract skeleton
- blueprint manifest/checklist skeleton

Wave 2: core auth slice
- backend auth core
- frontend auth state/UI
- verify script baseline

Wave 3: convergence
- tests 4축
- provider flag / rollback / KPI docs
- promotion checklist evidence fill-in

### Dependency Matrix
| Step | Depends on |
|---|---|
| pinned versions | — |
| auth-openapi | pinned versions |
| auth-manifest | pinned versions, auth-openapi |
| backend auth core | auth-openapi, auth-manifest |
| frontend auth state | auth-openapi |
| verify scripts | auth-manifest, auth-openapi |
| tests | backend auth core, frontend auth state, verify scripts |
| rollout docs | backend auth core, frontend auth state |

### Agent Dispatch Summary
- Wave 1: quick/unspecified-low for docs/contracts
- Wave 2: fe + be lanes can run in parallel after contract freeze
- Wave 3: qa/test-engineering lane plus docs lane

## TODOs
- [ ] 1. Freeze pinned versions

  **What to do**: `docs/plans/auth-blueprint-reference-selection.md`와 governance 기준을 바탕으로 Java, Spring Boot, Spring Security, React, Node, OpenAPI generator, build tooling의 pinned version 초안을 작성한다.
  **Must NOT do**: latest만 보고 day-zero 채택하지 않는다.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: version policy와 framework compatibility 판단 필요
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 2,3,4 | Blocked By: —

  **References**:
  - `docs/TEMPLATE-GOVERNANCE.md:174-269` — latest-enough vs stable-enough 기준
  - `docs/plans/auth-blueprint-reference-selection.md` — reference source 조합
  - `ACTIVE-LOOP.md:34-49` — pinned version 초안이 Stage 2 exit 조건

  **Acceptance Criteria**:
  - [ ] pinned version 표가 문서로 존재한다
  - [ ] LTS / patch maturity rationale가 각 핵심 dependency에 붙는다

  **QA Scenarios**:
  ```
  Scenario: 버전 baseline completeness
    Tool: Bash
    Steps: 문서에서 Java/Spring/React/Node/OpenAPI tooling 버전 존재 여부 확인
    Expected: 핵심 dependency version 누락 없음
    Evidence: .sisyphus/evidence/task-1-version-baseline.txt

  Scenario: 안정성 근거 확인
    Tool: Bash
    Steps: 각 버전 옆 rationale 존재 여부 확인
    Expected: latest-only 결정 없음
    Evidence: .sisyphus/evidence/task-1-version-rationale.txt
  ```

  **Commit**: NO | Message: `docs(auth): freeze pinned versions` | Files: [docs/...]

- [ ] 2. Define schema-first OpenAPI contract

  **What to do**: `contracts/auth-openapi.yaml`을 작성해 auth endpoints, request/response schema, error schema, unverified user state, account linking, `/auth/me` 최소 상태를 고정한다.
  **Must NOT do**: Spring filter path를 숨긴 implicit magic으로 남기지 않는다.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: contract가 frontend/backend/verify 전부의 기준
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 4,5,6 | Blocked By: 1

  **References**:
  - `docs/designs/auth-blueprint.md:129-132` — OpenAPI source of truth
  - `docs/plans/auth-blueprint-implementation-plan.md:177-203` — endpoint scope
  - `docs/plans/auth-blueprint-eng-review-test-plan.md` — critical paths and routes

  **Acceptance Criteria**:
  - [ ] signup/login/resend/verify/refresh/logout/me/link-account schema 존재
  - [ ] error response shape와 auth state shape가 정의됨
  - [ ] roles와 unverified state가 응답 모델에 반영됨

  **QA Scenarios**:
  ```
  Scenario: 계약 범위 검증
    Tool: Bash
    Steps: auth-openapi에 필수 endpoint 존재 여부 확인
    Expected: 필수 endpoint 누락 없음
    Evidence: .sisyphus/evidence/task-2-openapi-endpoints.txt

  Scenario: schema integrity 검증
    Tool: Bash
    Steps: OpenAPI validator 실행
    Expected: schema validation pass
    Evidence: .sisyphus/evidence/task-2-openapi-validate.txt
  ```

  **Commit**: NO | Message: `docs(auth): define auth contract` | Files: [contracts/auth-openapi.yaml]

- [ ] 3. Create canonical auth manifest and checklist

  **What to do**: `blueprints/auth-manifest.yaml`과 `blueprints/auth-checklist.md`를 작성해 must_not, reject_if, testing baseline, verification checkpoints, provider policy, token policy를 고정한다.
  **Must NOT do**: rules와 verify를 따로 손관리 구조로 두지 않는다.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: governance를 machine-readable로 내려야 함
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 6,7 | Blocked By: 1,2

  **References**:
  - `docs/TEMPLATE-GOVERNANCE.md:207-251` — canonical acceptance gates
  - `docs/governance/TEMPLATE-LIFECYCLE.md` — stage exit conditions
  - `docs/plans/auth-blueprint-promotion-checklist.md` — promotion checklist

  **Acceptance Criteria**:
  - [ ] manifest에 official_doc_refs / approved_github_refs / practical refs가 채워짐
  - [ ] must_not / reject_if / anti-pattern 정의됨
  - [ ] testing baseline / verify checkpoints 정의됨

  **QA Scenarios**:
  ```
  Scenario: manifest completeness
    Tool: Bash
    Steps: 필수 필드 존재 여부 검사
    Expected: governance 필수 필드 누락 없음
    Evidence: .sisyphus/evidence/task-3-manifest-complete.txt

  Scenario: reject rule usability
    Tool: Bash
    Steps: reject simulation 샘플 입력과 대조
    Expected: 최소 1개 must_not, 1개 reject_if가 실제로 판정 가능
    Evidence: .sisyphus/evidence/task-3-reject-rules.txt
  ```

  **Commit**: NO | Message: `docs(auth): define manifest and checklist` | Files: [blueprints/auth-manifest.yaml, blueprints/auth-checklist.md]

- [ ] 4. Scaffold backend auth baseline

  **What to do**: `backend/` skeleton과 Spring Boot project baseline을 만들고, Spring Security built-in JWT flow, stateful refresh storage, RBAC role model, CSRF/CORS baseline, provider config/property flags의 자리를 만든다.
  **Must NOT do**: custom JWT filter 중심으로 설계하지 않는다.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: security baseline이 전체 품질을 좌우함
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 7,8 | Blocked By: 1,2,3

  **References**:
  - `docs/designs/auth-blueprint.md:69-82` — core principles
  - `docs/plans/auth-blueprint-implementation-plan.md:206-238` — backend slice
  - `docs/plans/auth-blueprint-reference-selection.md` — Spring source choices

  **Acceptance Criteria**:
  - [ ] backend project boots
  - [ ] security baseline skeleton exists for JWT/refresh/RBAC/CSRF/CORS/provider flags
  - [ ] account linking and unverified state have explicit placeholders/contracts

  **QA Scenarios**:
  ```
  Scenario: backend skeleton health
    Tool: Bash
    Steps: backend build 실행
    Expected: build pass
    Evidence: .sisyphus/evidence/task-4-backend-build.txt

  Scenario: security baseline presence
    Tool: Bash
    Steps: security config에서 JWT, CSRF/CORS, RBAC, provider flag 자리 확인
    Expected: 핵심 security concerns 누락 없음
    Evidence: .sisyphus/evidence/task-4-security-baseline.txt
  ```

  **Commit**: NO | Message: `feat(auth): scaffold backend baseline` | Files: [backend/...]

- [ ] 5. Scaffold frontend auth baseline

  **What to do**: `frontend/` skeleton과 최소 auth UI/state 구조를 만들고 `/auth/me` 기반 상태, refresh mutex/queue, provider button policy, fallback UX의 자리를 만든다.
  **Must NOT do**: broad UI scope로 새 제품 기능까지 확장하지 않는다.

  **Recommended Agent Profile**:
  - Category: `visual-engineering` — Reason: auth UX와 state 경계가 중요
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 7,8 | Blocked By: 1,2

  **References**:
  - `docs/designs/auth-blueprint.md:42-60` — in-scope UX states
  - `docs/plans/auth-blueprint-implementation-plan.md:240-290` — frontend slice
  - `docs/plans/auth-blueprint-eng-review-test-plan.md` — affected routes and interactions

  **Acceptance Criteria**:
  - [ ] login/signup/verify result/protected route placeholder states exist
  - [ ] `/auth/me` driven minimal auth state structure exists
  - [ ] refresh mutex/queue and provider fallback UX positions are explicit

  **QA Scenarios**:
  ```
  Scenario: auth UI skeleton completeness
    Tool: Bash
    Steps: frontend route/component skeleton 존재 여부 확인
    Expected: key auth routes/states 누락 없음
    Evidence: .sisyphus/evidence/task-5-frontend-skeleton.txt

  Scenario: minimal auth state contract fit
    Tool: Bash
    Steps: frontend auth model과 auth-openapi 응답 shape 대조
    Expected: `/auth/me` 최소 UI 상태와 일치
    Evidence: .sisyphus/evidence/task-5-auth-state-fit.txt
  ```

  **Commit**: NO | Message: `feat(auth): scaffold frontend baseline` | Files: [frontend/...]

- [ ] 6. Implement verify engine skeleton

  **What to do**: `verify/` 구조를 만들고 manifest를 읽어 보안/계약/RBAC 위반을 즉시 실패시키는 script skeleton과 fixture를 만든다.
  **Must NOT do**: verify 기준을 코드 안에 하드코딩해 manifest와 중복 관리하지 않는다.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: 이 제품의 엔진
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 8 | Blocked By: 2,3

  **References**:
  - `docs/designs/auth-blueprint.md:134-147` — verify loop principles
  - `docs/governance/TEMPLATE-LIFECYCLE.md` — curated promotion checks
  - `docs/plans/auth-blueprint-promotion-checklist.md` — required evidence

  **Acceptance Criteria**:
  - [ ] manifest consumer skeleton exists
  - [ ] golden / violation / false-positive fixture strategy exists
  - [ ] fail-open 없음 원칙이 스크립트 구조에 반영됨

  **QA Scenarios**:
  ```
  Scenario: manifest-driven verify
    Tool: Bash
    Steps: sample manifest와 fixture로 verify skeleton 실행
    Expected: manifest를 읽고 판정 경로가 분기됨
    Evidence: .sisyphus/evidence/task-6-verify-manifest.txt

  Scenario: false-positive guard
    Tool: Bash
    Steps: 정상 fixture를 verify에 통과시킴
    Expected: 정상 케이스를 잘못 reject하지 않음
    Evidence: .sisyphus/evidence/task-6-verify-false-positive.txt
  ```

  **Commit**: NO | Message: `feat(auth): scaffold verify engine` | Files: [verify/...]

- [ ] 7. Add backend and frontend key-flow tests

  **What to do**: Spring integration tests, React auth-state tests, verify tests, key-flow E2E skeleton을 추가한다.
  **Must NOT do**: broad real-OAuth browser E2E를 V1에 끌어오지 않는다.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: tests are load-bearing
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: 8 | Blocked By: 4,5,6

  **References**:
  - `docs/plans/auth-blueprint-eng-review-test-plan.md`
  - `docs/designs/auth-blueprint.md:149-168`
  - `docs/plans/auth-blueprint-implementation-plan.md:305-339`

  **Acceptance Criteria**:
  - [ ] test 4축 skeleton 존재
  - [ ] verify script test 3종 존재
  - [ ] key-flow E2E 범위가 문서와 일치

  **QA Scenarios**:
  ```
  Scenario: test matrix completeness
    Tool: Bash
    Steps: backend/frontend/verify/e2e test files 존재 여부 검사
    Expected: 4축 테스트 파일 누락 없음
    Evidence: .sisyphus/evidence/task-7-test-matrix.txt

  Scenario: verify 3종 completeness
    Tool: Bash
    Steps: golden/violation/false-positive 케이스 대응 테스트 존재 확인
    Expected: verify 핵심 3종 테스트 누락 없음
    Evidence: .sisyphus/evidence/task-7-verify-triplet.txt
  ```

  **Commit**: NO | Message: `test(auth): add baseline auth test matrix` | Files: [backend/src/test..., frontend/tests..., verify/...]

- [ ] 8. Fill promotion evidence and curated gate pack

  **What to do**: promotion checklist, ACTIVE-LOOP, source provenance, watchlist linkage를 업데이트해서 `curated` 승격에 필요한 evidence package를 만든다.
  **Must NOT do**: evidence 없이 curated 승격 선언하지 않는다.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: governance closure
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: — | Blocked By: 4,5,6,7

  **References**:
  - `ACTIVE-LOOP.md`
  - `docs/governance/TEMPLATE-LIFECYCLE.md`
  - `docs/plans/auth-blueprint-promotion-checklist.md`
  - `docs/governance/upstream-watchlist.yaml`

  **Acceptance Criteria**:
  - [ ] pinned version 초안 채워짐
  - [ ] provenance fields 채워짐
  - [ ] ACTIVE-LOOP Stage 4 진입 조건 충족
  - [ ] curated 승격 가능 여부가 evidence 기반으로 판단 가능

  **QA Scenarios**:
  ```
  Scenario: curated gate completeness
    Tool: Bash
    Steps: promotion checklist와 ACTIVE-LOOP cross-check
    Expected: curated gate에 필요한 증거 누락 없음
    Evidence: .sisyphus/evidence/task-8-curated-gate.txt

  Scenario: fail-open audit
    Tool: Bash
    Steps: freshness/verify/reject simulation 누락 여부 검사
    Expected: fail-open 항목 0개
    Evidence: .sisyphus/evidence/task-8-fail-open-audit.txt
  ```

  **Commit**: NO | Message: `docs(auth): prepare curated promotion evidence` | Files: [docs/..., ACTIVE-LOOP.md]

## Final Verification Wave
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- 문서/계약/manifest는 작게 나눠 커밋 가능
- backend/frontend/verify는 최소 수직 슬라이스 단위로 커밋
- curated evidence는 마지막에 별도 정리 커밋 가능

## Success Criteria
- `ax-template`가 draft에서 curated gate 직전까지 이동한다.
- 모든 핵심 결정이 코드/계약/manifest/verify/test에 반영된다.
- implementer가 더 이상 핵심 아키텍처 결정을 새로 내릴 필요가 없다.
