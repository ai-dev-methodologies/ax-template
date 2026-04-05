# Auth Blueprint Curated Unblockers

## TL;DR
> **Summary**: 현재 `draft 유지`를 만든 3개 blocker(frontend test script, e2e infra, Spring-stack freshness)를 순차적으로 제거하고, evidence를 재생성한 뒤 curated 재판정을 수행한다.
> **Deliverables**:
> - frontend test 실행 경로
> - 최소 e2e 실행 인프라 + auth happy path
> - Spring freshness fallback evidence
> - cleaned workspace + regenerated curated evidence
> - curated 재판정 결과
> **Effort**: Medium
> **Parallel**: YES - 4 waves
> **Critical Path**: frontend test path → e2e infra → freshness fallback → evidence refresh → curated re-eval

## Context
### Original Request
사용자는 `auth-blueprint-next-procedure`를 확정한 뒤, 다음으로 무엇을 할지 순차적 계획을 원한다.

### Interview Summary
- 직전 절차의 최종 결론은 `draft 유지`다.
- blocker는 세 가지로 확정됐다:
  1. frontend `test` script 없음
  2. e2e 인프라 없음
  3. Spring-stack `chub` freshness unavailable
- 추가로, 커밋 전에 `backend/.gradle`, `backend/build` 같은 generated artifact 정리가 필요하다.

### Oracle / Metis Review (gaps addressed)
- E2E는 전체 테스트 프레임워크가 아니라 **auth curated 검증에 필요한 최소 happy-path 범위**로 제한해야 한다.
- `chub` 실패는 무한 재시도가 아니라 **수동 freshness fallback**으로 닫을 규칙이 필요하다.
- cleanup은 맨 앞이 아니라 **재평가 직전**에 수행해야 evidence와 workspace를 동시에 안정화할 수 있다.
- 최종 결과는 `draft 유지` 재확인 또는 `curated 승격 가능` 중 하나여야 하며, 중간 상태로 흐리면 안 된다.

## Work Objectives
### Core Objective
현재 curated 승격을 막는 3개 blocker를 제거하고, 동일 기준으로 다시 평가했을 때 curated 승격 가능 여부를 evidence 기반으로 재판정할 수 있게 만든다.

### Deliverables
- `frontend/package.json` 기반 실제 test script + 최소 실행 결과
- 최소 e2e infrastructure + auth key-flow happy path 실행 결과
- Spring-stack freshness fallback evidence (`chub unavailable` 대체 근거)
- regenerated curated evidence bundle
- 최종 재판정 기록

### Definition of Done
- frontend 테스트 명령이 실제로 존재하고 실행된다.
- e2e happy path가 실제로 한 번이라도 실행돼 evidence가 남는다.
- Spring / Spring Security / Springdoc 계열 freshness 공백이 fallback evidence로 닫힌다.
- generated artifact 정리 기준이 반영되고 workspace가 커밋 가능한 상태로 정리된다.
- `Draft -> Curated` 체크리스트 항목을 다시 대조한 최종 판정이 evidence로 남는다.

### Must Have
- frontend test path를 실제 runner와 연결
- e2e scope는 auth curated path 한정
- `chub` 실패 시 공식 문서 fallback 규칙 명시
- cleanup 후 evidence 재생성 또는 재검증
- 최종 판정은 binary (`draft 유지` 또는 `curated 가능`)

### Must NOT Have
- 광범위한 UI 테스트 스위트 구축
- Spring freshness를 "unavailable이지만 괜찮음"으로 넘기기
- generated artifact를 source 변경과 섞어서 커밋
- evidence 재생성 없이 예전 판정을 뒤집기

## Verification Strategy
> ZERO HUMAN INTERVENTION — 모든 판정은 agent-executed evidence로만 이뤄진다.
- Test decision: tests-after + evidence-refresh
- QA policy: 각 blocker 제거는 구현 + 실행 + evidence를 한 task로 묶는다
- Evidence root: `.sisyphus/evidence/`

## Execution Strategy
### Parallel Execution Waves
Wave 1: frontend test path + e2e scope definition
Wave 2: e2e infra 구현 + Spring freshness fallback 수집
Wave 3: cleanup + evidence refresh + checklist recorrelation
Wave 4: curated 재판정 + final verification wave

### Dependency Matrix
- T1 fixes the frontend testing blocker and unblocks T4 evidence refresh.
- T2 creates the minimal e2e path and unblocks curated gate re-evaluation.
- T3 resolves freshness fallback and removes Spring documentation uncertainty.
- T4 cleans generated artifacts only after blocker-removal work is complete.
- T5 refreshes the evidence set and checklist correlation.
- T6 makes the new promotion decision.

## TODOs

- [x] 1. Add real frontend test execution path

  **What to do**: `frontend/package.json`에 실제 `test` script를 추가하고, 현재 placeholder 테스트를 실행 가능한 최소 runner(Vitest 권장)로 연결한다.
  **Must NOT do**: auth domain 밖의 UI/test infra를 넓히지 않는다.

  **Recommended Agent Profile**:
  - Category: `visual-engineering` — Reason: frontend tooling + auth-state tests
  - Skills: [`vercel-react-best-practices`] — React runtime/tooling baseline
  - Omitted: [`frontend-ui-ux`] — UI polish가 목적이 아님

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 5,6 | Blocked By: —

  **References**:
  - `frontend/package.json`
  - `frontend/tests/auth-state.placeholder.ts`
  - `frontend/tests/key-flow.placeholder.ts`
  - `.sisyphus/drafts/auth-blueprint-quality-companion.md`
  - `.sisyphus/evidence/procedure-4-test-matrix.txt`

  **Acceptance Criteria**:
  - [ ] `frontend/package.json`에 `test` script 존재
  - [ ] `npm --prefix frontend test` 또는 동등 명령이 실행된다
  - [ ] auth-state / key-flow placeholder가 실제 test runner에 연결된다

  **QA Scenarios**:
  ```
  Scenario: frontend test command works
    Tool: Bash
    Steps: frontend test 명령 실행
    Expected: exit code 0 또는 실패 시 구체적인 assertion failure
    Evidence: .sisyphus/evidence/curated-frontend-test.txt

  Scenario: missing script blocker removed
    Tool: Bash
    Steps: frontend/package.json에서 test script 존재 확인 후 실행
    Expected: "missing script: test" 오류가 더 이상 없음
    Evidence: .sisyphus/evidence/curated-frontend-test-script.txt
  ```

  **Commit**: YES | Message: `test(frontend): wire auth placeholders to runner` | Files: [`frontend/package.json`, `frontend/tests/**`]

- [x] 2. Introduce minimal e2e infrastructure for auth curated path

  **What to do**: Playwright 또는 동등한 최소 e2e runner를 도입하고, auth curated 판단에 필요한 단일 happy path(`signup -> unverified -> verify -> login -> protected route`)만 실행 가능하게 만든다.
  **Must NOT do**: 전역 e2e 프레임워크나 비관련 시나리오를 추가하지 않는다.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: infra + flow wiring
  - Skills: []
  - Omitted: [`browse`] — 실행 확인은 가능하지만 구축 자체는 별도 skill 불필요

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 5,6 | Blocked By: 1

  **References**:
  - `.sisyphus/drafts/auth-blueprint-quality-companion.md`
  - `frontend/tests/key-flow.placeholder.ts`
  - `contracts/auth-openapi.yaml`
  - `.sisyphus/evidence/procedure-4-test-matrix.txt`

  **Acceptance Criteria**:
  - [ ] e2e runner 설정 파일 존재
  - [ ] auth happy path 시나리오 파일 존재
  - [ ] e2e 실행 evidence 생성

  **QA Scenarios**:
  ```
  Scenario: auth happy path e2e
    Tool: Bash
    Steps: e2e 실행 명령 수행
    Expected: signup -> verify -> login -> protected route 흐름 확인
    Evidence: .sisyphus/evidence/curated-e2e-happy-path.txt

  Scenario: e2e blocker removal
    Tool: Bash
    Steps: 기존 missing infrastructure 상태와 새 실행 결과 비교
    Expected: "missing e2e infrastructure" blocker 해소
    Evidence: .sisyphus/evidence/curated-e2e-blocker-removal.txt
  ```

  **Commit**: YES | Message: `test(e2e): add minimal auth happy path` | Files: [`frontend/**`, `playwright*`, `e2e/**`]

- [x] 3. Resolve Spring-stack freshness fallback

  **What to do**: `chub unavailable`인 Spring Boot / Spring Security / Springdoc 계열에 대해 공식 문서 fallback evidence를 수집하고 freshness gap을 수동 검토 가능한 상태로 닫는다.
  **Must NOT do**: unavailable 결과를 성공으로 위장하지 않는다.

  **Recommended Agent Profile**:
  - Category: `writing` — Reason: evidence/fallback packaging
  - Skills: []
  - Omitted: []

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 5,6 | Blocked By: —

  **References**:
  - `.sisyphus/evidence/curated-chub-results.md`
  - `blueprints/pinned-versions.yaml`
  - `backend/build.gradle.kts`

  **Acceptance Criteria**:
  - [ ] Spring Boot fallback evidence 존재
  - [ ] Spring Security fallback evidence 존재
  - [ ] Springdoc/OpenAPI fallback evidence 존재
  - [ ] 각 evidence에 source URL/버전/판정(status) 포함

  **QA Scenarios**:
  ```
  Scenario: freshness fallback completeness
    Tool: Bash
    Steps: fallback evidence 파일에서 spring-boot, spring-security, springdoc 항목 확인
    Expected: 3개 모두 존재
    Evidence: .sisyphus/evidence/curated-spring-freshness-check.txt

  Scenario: no fake freshness
    Tool: Bash
    Steps: chub unavailable 상태와 fallback evidence 비교
    Expected: unavailable을 숨기지 않고 fallback으로 보완했음
    Evidence: .sisyphus/evidence/curated-spring-freshness-audit.txt
  ```

  **Commit**: YES | Message: `docs(auth): add spring freshness fallback evidence` | Files: [`.sisyphus/evidence/**`, `docs/**`]

- [x] 4. Normalize generated artifacts before re-evaluation

  **What to do**: `backend/.gradle`, `backend/build` 등 generated artifact를 정리하고, 필요한 ignore/cleanup 기준을 고정한다.
  **Must NOT do**: evidence 원본까지 지우거나 source 변경과 cleanup을 섞지 않는다.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: cleanup + ignore hygiene
  - Skills: []
  - Omitted: []

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: 5 | Blocked By: 1,2,3

  **References**:
  - `backend/.gradle/**`
  - `backend/build/**`
  - `.sisyphus/evidence/procedure-4-test-matrix.txt`

  **Acceptance Criteria**:
  - [ ] generated artifact 정리 기준 명시
  - [ ] cleanup 후 workspace가 재실행 가능 상태 유지
  - [ ] cleanup evidence 존재

  **QA Scenarios**:
  ```
  Scenario: generated artifact cleanup
    Tool: Bash
    Steps: cleanup 전후 backend/.gradle, backend/build 상태 비교
    Expected: generated artifact가 source 변경과 분리됨
    Evidence: .sisyphus/evidence/curated-cleanup.txt

  Scenario: rerun safety
    Tool: Bash
    Steps: cleanup 후 backend/frontend/verify 핵심 명령 재실행
    Expected: cleanup이 실행 가능성을 깨지 않음
    Evidence: .sisyphus/evidence/curated-cleanup-rerun.txt
  ```

  **Commit**: YES | Message: `chore(repo): normalize generated artifacts` | Files: [`backend/.gitignore`, `backend/**`]

- [x] 5. Refresh curated evidence bundle

  **What to do**: blocker 제거 후 test matrix, fail-open audit, checklist correlation, decision trace를 새 상태에 맞춰 재생성한다.
  **Must NOT do**: 예전 blocker 상태를 그대로 두거나, 새 evidence 없이 판정을 바꾸지 않는다.

  **Recommended Agent Profile**:
  - Category: `writing` — Reason: evidence rebasing
  - Skills: []
  - Omitted: []

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: 6 | Blocked By: 1,2,3,4

  **References**:
  - `.sisyphus/evidence/procedure-4-test-matrix.txt`
  - `.sisyphus/evidence/procedure-5-fail-open-audit.txt`
  - `.sisyphus/evidence/procedure-5-checklist-correlation.txt`
  - `.sisyphus/evidence/procedure-6-decision-trace.txt`

  **Acceptance Criteria**:
  - [ ] old blocker wording이 최신 상태로 갱신됨
  - [ ] checklist correlation이 최신 evidence 경로만 참조
  - [ ] fail-open audit이 residual risk만 남김

  **QA Scenarios**:
  ```
  Scenario: evidence refresh consistency
    Tool: Bash
    Steps: refreshed evidence 간 모순 여부 검사
    Expected: 동일 항목에 PASS/BLOCKED 충돌 없음
    Evidence: .sisyphus/evidence/curated-evidence-consistency.txt

  Scenario: blocker regression audit
    Tool: Bash
    Steps: 기존 3 blocker가 evidence 상에서 제거/잔존 여부 확인
    Expected: 제거된 blocker는 cleared, 잔존 blocker는 explicit
    Evidence: .sisyphus/evidence/curated-blocker-regression.txt
  ```

  **Commit**: YES | Message: `docs(auth): refresh curated evidence bundle` | Files: [`.sisyphus/evidence/**`]

- [x] 6. Re-evaluate curated promotion

  **What to do**: 새 evidence 기준으로 `Draft -> Curated` 체크리스트를 다시 판정하고, `draft 유지` 또는 `curated 가능` 중 하나로 결정한다.
  **Must NOT do**: partial success를 curated success로 부풀리지 않는다.

  **Recommended Agent Profile**:
  - Category: `oracle` — Reason: binary go/no-go judgment
  - Skills: []
  - Omitted: []

  **Parallelization**: Can Parallel: NO | Wave 4 | Blocks: F1-F4 | Blocked By: 5

  **References**:
  - `docs/plans/auth-blueprint-promotion-checklist.md`
  - `.sisyphus/evidence/**`
  - `ACTIVE-LOOP.md`

  **Acceptance Criteria**:
  - [ ] final decision이 evidence file path로 추적 가능
  - [ ] `draft 유지` 또는 `curated 가능` 중 하나만 남음
  - [ ] 실패 시 다음 re-entry target 명시

  **QA Scenarios**:
  ```
  Scenario: curated decision traceability
    Tool: Bash
    Steps: 최종 판정과 evidence file path 연결성 검사
    Expected: 판단 근거가 전부 파일 경로로 추적 가능
    Evidence: .sisyphus/evidence/curated-decision-trace.txt

  Scenario: no-guess promotion
    Tool: Bash
    Steps: 최종 decision 문구에서 추정/감 표현 검사
    Expected: evidence 기반 binary 판정만 존재
    Evidence: .sisyphus/evidence/curated-no-guess.txt
  ```

  **Commit**: YES | Message: `docs(auth): re-evaluate curated promotion` | Files: [`ACTIVE-LOOP.md`, `.sisyphus/evidence/**`, `docs/**`]

## Final Verification Wave
- [x] F1. Unblocker Procedure Compliance Audit — oracle
- [x] F2. Curated Evidence Quality Review — unspecified-high
- [x] F3. Real Execution QA — unspecified-high
- [x] F4. Scope Fidelity Check — deep

## Commit Strategy
- frontend test path / e2e infra / freshness fallback / cleanup / evidence refresh / final decision을 각각 분리 커밋한다.
- generated artifact cleanup은 source feature 커밋과 합치지 않는다.

## Success Criteria
- 현재 3개 blocker가 모두 evidence로 해소되거나, 해소 실패 시 그 이유가 더 좁은 re-entry target으로 축소된다.
- curated 승격 여부가 이전보다 더 강한 근거로 binary 판정된다.
- 다음 implementer가 blocker 제거 순서를 다시 고민할 필요가 없다.
