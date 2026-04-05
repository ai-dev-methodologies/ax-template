# ax-template Auth Blueprint Stage 3 Assets

## TL;DR
> **Summary**: `draft -> curated`로 넘어가기 전에 필요한 실제 canonical draft asset을 만든다. 이번 하위 계획은 코드 구현보다 먼저, pinned versions / manifest / scaffold baseline / architecture baseline / quality companion draft를 고정하는 데 집중한다.
> **Deliverables**:
> - pinned version matrix
> - `blueprints/auth-manifest.yaml` draft
> - `blueprints/auth-checklist.md` draft
> - scaffold asset list
> - architecture baseline rule set
> - quality companion baseline
> **Effort**: Short
> **Parallel**: YES - 2 waves
> **Critical Path**: pinned versions → manifest draft → baseline trio alignment → promotion evidence handoff

## Context
이 계획은 `.sisyphus/plans/auth-blueprint-execution.md`의 Wave 1을 더 세밀하게 분해한 하위 계획이다.
현재 `ACTIVE-LOOP.md` 기준 auth blueprint 상태는 `draft`이고, curated 승격 전 가장 부족한 것은 실제 canonical draft asset이다.

## Locked Defaults for Wave 1
- pinned versions 파일 위치: `blueprints/pinned-versions.yaml`
- OpenAPI 스펙 버전: `3.0.3`
- contract는 static 유지, provider flag는 schema를 바꾸지 않고 runtime structured error로 처리
- login rate limit 기본값: `5 attempts / 15 minutes / IP + identifier`
- resend verification rate limit 기본값: `3 attempts / 10 minutes / email`
- `auth-manifest.yaml`은 BP consumer contract 최소 shape를 따른다
- verify placeholder로 `verify/manifest.schema.json`을 포함한다

## Objectives
### Core Objective
정책 문서와 후보 문서를 실제 template asset 초안으로 내려, curated promotion check에 들어갈 준비를 마친다.

### Definition of Done
- pinned version 초안이 존재한다
- manifest/checklist draft가 존재한다
- scaffold / architecture baseline / quality companion의 범위와 산출물이 명확하다
- provenance와 reject rule이 draft 수준에서 채워져 있다
- ACTIVE-LOOP Stage 3 체크박스를 대부분 닫을 수 있다

## Execution Strategy
### Wave 1
1. Freeze pinned version matrix
2. Define schema-first OpenAPI contract
3. Define auth manifest draft
4. Define auth checklist draft

### Wave 2
4. Define scaffold draft asset set
5. Define architecture baseline rule set
6. Define quality companion baseline
7. Prepare curated promotion evidence map

## TODOs
- [x] 1. Freeze pinned version matrix

  **What to do**: `blueprints/pinned-versions.yaml`에 Java, Spring Boot, Spring Security, Node, React, OpenAPI generator, test tooling, build tooling의 pinned version 초안을 만든다.
  **Must NOT do**: latest only reasoning으로 고정하지 않는다.

  **Acceptance Criteria**:
  - [ ] `blueprints/pinned-versions.yaml` 파일이 존재한다
  - [ ] 각 핵심 dependency의 버전이 명시된다
  - [ ] 각 버전에 stable-enough rationale이 붙는다
  - [ ] pre-release/experimental 채택이 없다

- [x] 2. Draft `contracts/auth-openapi.yaml`

  **What to do**: OpenAPI `3.0.3` 기준으로 auth endpoints, request/response schema, error schema, unverified state, account linking, `/auth/me`, provider-disabled structured error를 포함한 계약 초안을 만든다.
  **Must NOT do**: provider flag에 따라 schema를 갈라놓지 않는다.

  **Acceptance Criteria**:
  - [ ] 필수 auth endpoint가 모두 정의된다
  - [ ] disabled provider error shape가 정의된다
  - [ ] login/resend rate limit 기본값이 문서에 반영된다
  - [ ] schema가 static contract로 유지된다

- [x] 3. Draft `blueprints/auth-manifest.yaml`

  **What to do**: BP consumer contract 최소 shape에 맞춰 template_id, family, status, stack, when_to_use, must_not, reject_if, provider policy, token policy, testing baseline, source refs, last_reviewed_at를 포함한 draft를 만든다.
  **Must NOT do**: rules를 prose only로 두지 않는다.

  **Acceptance Criteria**:
  - [ ] machine-readable 필드가 채워진다
  - [ ] auth blueprint scope가 manifest에 반영된다
  - [ ] official_doc_refs / approved_github_refs / practical refs 자리가 비어 있지 않다

- [x] 4. Draft `blueprints/auth-checklist.md`

  **What to do**: 구현/검증 시 반드시 통과해야 하는 checklist를 작성한다.
  **Must NOT do**: 중복된 TODO 나열만 하지 않는다.

  **Acceptance Criteria**:
  - [ ] auth boundary, CSRF/CORS, refresh, provider fallback, account linking, unverified state, tests가 checklist에 포함된다
  - [ ] 각 항목이 verify/test에 연결 가능하다

- [x] 5. Define scaffold draft asset set

  **What to do**: `frontend/`, `backend/`, `contracts/`, `blueprints/`, `verify/`, `.github/workflows/` 각각에서 V1에 실제로 필요한 최소 파일군을 정의한다.
  **Must NOT do**: broad starter repo를 그대로 복제하는 식으로 범위를 부풀리지 않는다.

  **Acceptance Criteria**:
  - [ ] 각 디렉토리별 최소 필수 산출물이 목록화된다
  - [ ] V1 제외 범위가 끼어들지 않는다

- [x] 6. Define architecture baseline rule set

  **What to do**: OpenAPI source-of-truth, Spring Security built-in JWT, stateful refresh, `/auth/me`, refresh mutex/queue, explicit account linking, CSRF/CORS, provider flag, rate limit의 규칙을 architecture baseline으로 정리한다.
  **Must NOT do**: 구현 세부를 라이브러리 마법에 맡긴 추상 문장으로 끝내지 않는다.

  **Acceptance Criteria**:
  - [ ] backend/frontend boundary 규칙이 명시된다
  - [ ] auth state flow와 failure path가 rule set에 포함된다
  - [ ] custom JWT filter 금지가 명시된다

- [x] 7. Define quality companion baseline

  **What to do**: 테스트 4축, verify 3종, zero-warning 목표, reject simulation, evidence paths를 quality companion 초안으로 정리하고 `verify/manifest.schema.json` placeholder를 포함한다.
  **Must NOT do**: QA를 나중 과제로 미루지 않는다.

  **Acceptance Criteria**:
  - [ ] Spring integration / React auth-state / verify / key-flow E2E가 baseline에 명시된다
  - [ ] false-positive 방지까지 포함된다
  - [ ] curated promotion에 필요한 evidence map이 이어진다

- [x] 8. Prepare curated promotion evidence map

  **What to do**: `auth-blueprint-promotion-checklist.md`와 `ACTIVE-LOOP.md` 기준으로 어떤 산출물이 어떤 체크박스를 닫는지 대응표를 만든다.
  **Must NOT do**: evidence 없이 curated 승격 가능하다고 주장하지 않는다.

  **Acceptance Criteria**:
  - [ ] Stage 3 산출물과 Stage 4 curated gate가 연결된다
  - [ ] 아직 비어 있는 증거가 명시된다

## References
- `ACTIVE-LOOP.md`
- `docs/TEMPLATE-GOVERNANCE.md`
- `docs/governance/TEMPLATE-LIFECYCLE.md`
- `docs/plans/auth-blueprint-reference-selection.md`
- `docs/plans/auth-blueprint-promotion-checklist.md`
- `.sisyphus/plans/auth-blueprint-execution.md`
- `.sisyphus/drafts/auth-blueprint-wave1-artifacts-draft.md`

## Success Criteria
이 하위 계획이 끝나면, 다음 implementer는 더 이상 “무슨 기준으로 manifest를 쓰지?” 같은 판단을 할 필요가 없다. 즉 Stage 3 asset이 policy-compatible한 draft로 존재하고, curated 승격까지의 거리와 누락이 명확하게 보인다.
