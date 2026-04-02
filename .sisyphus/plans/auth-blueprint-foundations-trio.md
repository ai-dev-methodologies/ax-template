# ax-template Auth Blueprint Foundations Trio

## TL;DR
> **Summary**: Wave 1에서 가장 먼저 닫아야 하는 세 파일, `blueprints/pinned-versions.yaml`, `blueprints/auth-manifest.yaml`, `contracts/auth-openapi.yaml`을 decision-complete하게 정의한다. 이 세 파일이 닫혀야 backend/frontend/verify가 같은 기준 위에서 움직일 수 있다.
> **Deliverables**:
> - `blueprints/pinned-versions.yaml`
> - `blueprints/auth-manifest.yaml`
> - `contracts/auth-openapi.yaml`
> **Effort**: Short
> **Parallel**: NO
> **Critical Path**: build tool + generator pin → auth manifest policy freeze → schema-first OpenAPI contract freeze

## Context
이 계획은 `.sisyphus/plans/auth-blueprint-stage3-assets.md`를 더 좁힌 하위 계획이다.
Metis 리뷰 결과, 현재 가장 위험한 공백은 다음이었다.
- build tool이 아직 `gradle-kotlin or maven`으로 떠 있다
- OpenAPI contract와 manifest policy의 선후관계가 불명확하다
- `/auth/me`, account linking, refresh race`가 넓어질 위험이 있다
- verify가 읽는 machine-readable 기준이 아직 느슨하다

## Locked Defaults
- pinned versions 파일: `blueprints/pinned-versions.yaml`
- OpenAPI 버전: `3.0.3`
- provider flag는 contract shape를 바꾸지 않는다
- disabled provider는 runtime structured error
- login rate limit: `5 / 15m / IP+identifier`
- resend verification rate limit: `3 / 10m / email`
- Spring auth chain: built-in JWT flow
- refresh token: stateful
- account linking: explicit
- `/auth/me`: 최소 UI 상태만 반환

## Work Objectives
### Core Objective
세 파일만으로도 implementer가 더 이상 핵심 auth 정책을 새로 결정하지 않아도 되게 만든다.

### Definition of Done
- build tool과 generator가 exact pin 수준으로 고정된다
- manifest가 OpenAPI보다 앞선 policy source로 작동한다
- OpenAPI가 auth 최소 범위를 벗어나지 않는다
- verify가 읽을 machine-readable 기준이 manifest에 담긴다
- 이 세 파일만으로 backend/frontend/verify workstreams가 시작 가능하다

### Must Have
- exact pinned versions
- provider / token / RBAC / rate limit / source provenance policy in manifest
- auth endpoints only in OpenAPI
- structured error shape
- refresh race / linking / unverified state reflected

### Must NOT Have
- build tool 미정 상태
- OpenAPI가 manifest policy보다 먼저 설계를 끌고 가는 상태
- `/auth/me`에 profile/settings 범위 포함
- role administration endpoint 확장
- UI library 결정이 pinned versions에 섞여 들어감

## Execution Strategy
### Order
1. Fix toolchain versions
2. Freeze manifest policies
3. Derive static contract from manifest

이 순서를 바꾸지 않는다.

## TODOs
- [x] 1. Freeze backend/frontend toolchain exact pins

  **What to do**: Java, Spring Boot, Spring Security, Node, React, frontend codegen, backend build tool을 exact version 또는 exact minor train 수준으로 고정한다. 동시에 backend build tool을 `gradle-kotlin` 또는 `maven` 중 하나로 확정한다.
  **Must NOT do**: UI component 라이브러리나 broad product stack까지 여기서 끌어오지 않는다.

  **Acceptance Criteria**:
  - [ ] `blueprints/pinned-versions.yaml`에 build tool까지 포함된다
  - [ ] exact pin 또는 exact train rationale이 각 항목에 존재한다
  - [ ] pre-release / experimental 버전이 없다

  **QA Scenarios**:
  ```
  Scenario: toolchain exactness
    Tool: Bash
    Steps: pinned-versions 파일에서 core toolchain 값 존재 여부 확인
    Expected: Java/Spring/Node/React/OpenAPI generator/build tool 누락 없음
    Evidence: .sisyphus/evidence/task-trio-1-toolchain.txt

  Scenario: scope hygiene
    Tool: Bash
    Steps: pinned-versions에 auth scope 밖 UI/product deps가 포함됐는지 확인
    Expected: broad UI framework lock-in 없음
    Evidence: .sisyphus/evidence/task-trio-1-scope.txt
  ```

- [x] 2. Freeze auth manifest as policy source

  **What to do**: `blueprints/auth-manifest.yaml`에 provider policy, token delivery/storage, RBAC, account linking, rate limits, testing baseline, source provenance, reject_if/must_not를 모두 machine-readable하게 고정한다.
  **Must NOT do**: rate limit이나 provider policy를 OpenAPI note로만 남기지 않는다.

  **Acceptance Criteria**:
  - [ ] `rate_limits`가 discrete field로 존재한다
  - [ ] provider policy와 disabled behavior가 discrete field로 존재한다
  - [ ] `/auth/me`가 minimal UI state only라는 범위가 policy에 반영된다
  - [ ] source refs 배열이 placeholder가 아니라 채울 슬롯을 가진다
  - [ ] verify가 읽을 필드가 prose가 아니라 structured field다

  **QA Scenarios**:
  ```
  Scenario: manifest policy completeness
    Tool: Bash
    Steps: manifest 필수 policy field 존재 여부 검사
    Expected: token/provider/rbac/rate_limits/reject rules 누락 없음
    Evidence: .sisyphus/evidence/task-trio-2-manifest-fields.txt

  Scenario: machine readability
    Tool: Bash
    Steps: YAML parse 및 key presence 확인
    Expected: verify consumer가 읽을 수 있는 구조 유지
    Evidence: .sisyphus/evidence/task-trio-2-machine-readable.txt
  ```

- [x] 3. Derive schema-first OpenAPI from manifest

  **What to do**: `contracts/auth-openapi.yaml`을 manifest 정책을 기준으로 고정한다. endpoint 범위는 signup/login/resend/verify/refresh/logout/me/link-account로 제한한다. disabled provider structured error, unverified state, refresh race response, linking payload를 contract에 명시한다.
  **Must NOT do**: user profile / role administration / broad account management로 범위를 늘리지 않는다.

  **Acceptance Criteria**:
  - [ ] auth endpoint 범위가 확정된다
  - [ ] disabled provider structured error가 존재한다
  - [ ] unverified state shape가 존재한다
  - [ ] refresh race 대응 response가 문서화된다
  - [ ] account linking request/response shape가 문서화된다
  - [ ] `/auth/me`는 minimal UI state로 제한된다

  **QA Scenarios**:
  ```
  Scenario: endpoint boundary audit
    Tool: Bash
    Steps: auth-openapi endpoint 목록 검사
    Expected: auth scope 밖 endpoint 없음
    Evidence: .sisyphus/evidence/task-trio-3-endpoints.txt

  Scenario: state/error coverage audit
    Tool: Bash
    Steps: disabled provider, unverified state, linking, refresh race schema 존재 여부 확인
    Expected: 핵심 auth edge states 누락 없음
    Evidence: .sisyphus/evidence/task-trio-3-edge-states.txt
  ```

- [x] 4. Add verify schema placeholder alignment

  **What to do**: `verify/manifest.schema.json` placeholder responsibility를 pinned versions / manifest / contract 기준과 맞춘다. verify가 최소한 어떤 필드를 기대하는지 계획 수준에서 고정한다.
  **Must NOT do**: verify가 구현 때 알아서 맞추는 식으로 남겨두지 않는다.

  **Acceptance Criteria**:
  - [ ] verify consumer 필수 field set이 문서화된다
  - [ ] manifest와 verify schema placeholder의 역할 분리가 명확하다

  **QA Scenarios**:
  ```
  Scenario: verify placeholder contract
    Tool: Bash
    Steps: 필수 field set 문서 존재 여부 확인
    Expected: verify가 기대하는 최소 schema가 정의됨
    Evidence: .sisyphus/evidence/task-trio-4-verify-schema.txt
  ```

## References
- `.sisyphus/plans/auth-blueprint-execution.md`
- `.sisyphus/plans/auth-blueprint-stage3-assets.md`
- `.sisyphus/drafts/auth-blueprint-wave1-artifacts-draft.md`
- `docs/TEMPLATE-GOVERNANCE.md`
- `docs/designs/auth-blueprint.md`
- `ACTIVE-LOOP.md`

## Success Criteria
이 계획이 끝나면 implementer는 더 이상 “manifest에 이걸 넣어야 하나?”, “OpenAPI에 이 상태를 넣어야 하나?”, “build tool 뭐로 하지?” 같은 설계 결정을 하지 않는다. 즉 세 파일이 auth blueprint의 policy/contract/toolchain truth를 고정한 상태가 된다.

## Draft Artifacts Produced
- `.sisyphus/drafts/blueprints.pinned-versions.yaml.md`
- `.sisyphus/drafts/blueprints.auth-manifest.yaml.md`
- `.sisyphus/drafts/contracts.auth-openapi.yaml.md`
- `.sisyphus/drafts/verify.manifest.schema.json.md`
