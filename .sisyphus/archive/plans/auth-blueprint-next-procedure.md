# ax-template Auth Blueprint Next Procedure

## TL;DR
> **Summary**: 완료된 Stage 3 draft asset을 기준으로, 상태 정리 → 실행 계획 활성화 → 구현/evidence 실행 → curated 승격 판정까지의 전체 절차를 고정한다.
> **Deliverables**:
> - Stage 3 closeout/state sync
> - Stage 4 실행 명령/환경 readiness 고정
> - backend/frontend/verify/test/evidence 실행 결과
> - curated gate 판단 패키지
> **Effort**: Medium
> **Parallel**: YES - 4 waves
> **Critical Path**: Stage 3 closeout → command/env freeze → execution tasks 4-7 → curated gate pack → sign-off

## Context
### Original Request
사용자는 “다음 진행사항 계획”이 아니라, 지금 상태 이후의 **전체 절차와 목록**을 먼저 보고 판단하길 원한다.

### Interview Summary
- `auth-blueprint-stage3-assets`는 이미 8/8 완료다.
- 그러나 `ACTIVE-LOOP.md`는 아직 Stage 3를 in-progress로 두고 있고, Stage 4 Curated Promotion Check는 not-started 상태다.
- 사용자는 다음 설명에서 Stage/번호/다음 액션이 뒤섞이지 않도록 전체 순서를 명확히 보길 원한다.

### Oracle / Metis Review (gaps addressed)
- Stage 3 closeout, 새 실행 진입, evidence 실행, curated 판정을 한 단계로 뭉개면 false positive가 난다.
- `chub` freshness / reject simulation / ACTIVE-LOOP evidence는 문서 존재만으로 닫히지 않는다.
- 다음 계획은 반드시 **명령 고정**, **evidence 저장 위치**, **Go/No-Go 판정 규칙**, **실패 시 되돌아갈 단계**를 포함해야 한다.

## Work Objectives
### Core Objective
현재의 canonical draft 자산을 실제 실행 evidence로 연결해 `draft -> curated` 판정을 할 수 있는 상태까지 이동한다.

### Deliverables
- `ACTIVE-LOOP.md`와 실제 상태를 맞춘 Stage 3 closeout
- `auth-blueprint-execution.md` 기준 active execution 범위 확정
- build/lint/type/test/verify/reject/chub evidence 묶음
- `auth-blueprint-promotion-checklist.md` 기준 curated gate 판단 결과

### Definition of Done
- Stage 3 산출물 완료 사실이 상태 문서와 모순되지 않는다.
- 실행에 필요한 command matrix와 환경 전제조건이 고정된다.
- execution plan의 후반부(Tasks 4-8)를 통해 구현 + test + evidence가 생성된다.
- `Draft -> Curated` 체크리스트의 실행 기반 항목이 evidence로 검토 가능하다.
- 최종 sign-off에서 `curated 가능` 또는 `draft 유지`가 evidence 기반으로만 판단된다.

### Must Have
- closeout와 execution을 분리
- evidence 생성과 상태 변경을 분리
- reject simulation / fail-open audit / chub freshness를 명시적 gate로 취급
- 실패 시 되돌아갈 re-entry target 정의

### Must NOT Have
- evidence 없이 curated 승격 선언
- Stage 3 완료 여부와 Stage 4 실행 착수를 같은 커밋/같은 판정으로 처리
- 구현 중에 아키텍처/정책을 다시 설계하는 fail-open 흐름
- checklist 항목을 vague summary로 대체

## Verification Strategy
> ZERO HUMAN INTERVENTION — 모든 판단은 agent-executed evidence로만 이뤄진다.
- Test decision: tests-after + evidence pack
- QA policy: 각 단계는 실행 명령, 결과 파일, 판정 규칙을 가진다
- Evidence root: `.sisyphus/evidence/`

## Execution Strategy
### Parallel Execution Waves
Wave 1: 상태 정리 및 실행 전제조건 고정
Wave 2: 구현/스캐폴드/검증 엔진 실행 (`auth-blueprint-execution.md` Tasks 4-6)
Wave 3: 테스트/verify/curated evidence 실행 (`auth-blueprint-execution.md` Tasks 7-8)
Wave 4: sign-off, promotion decision, closeout

### Dependency Matrix
- T1 closes out current Stage 3 state and blocks any promotion claim.
- T2 freezes execution commands and environment prerequisites before code/test/evidence work.
- T3-T5 resume the existing execution plan and produce runnable backend/frontend/verify/test baselines.
- T6 runs curated gate evidence and checklist correlation.
- T7 performs final sign-off and decides `curated` or `draft 유지`.

## TODOs

- [x] 1. Stage 3 closeout and state sync

  **What to do**: `auth-blueprint-stage3-assets.md` 완료 사실을 기준으로 `ACTIVE-LOOP.md`, 현재 active state(`.sisyphus/boulder.json`), 그리고 필요한 상태 문서의 모순을 정리한다.
  **Must NOT do**: 아직 없는 실행 evidence를 근거로 curated 승격을 미리 반영하지 않는다.

  **Recommended Agent Profile**:
  - Category: `writing` — Reason: 상태/문서 동기화
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 2,3,4,5,6,7 | Blocked By: —

  **References**:
  - `ACTIVE-LOOP.md`
  - `.sisyphus/plans/auth-blueprint-stage3-assets.md`
  - `.sisyphus/boulder.json`

  **Acceptance Criteria**:
  - [ ] Stage 3 completed facts and loop status are no longer contradictory
  - [ ] 현재 active work가 무엇인지 문서 기준으로 한 줄로 설명 가능하다
  - [ ] curated / stable 관련 상태는 evidence 없이 바뀌지 않는다

  **QA Scenarios**:
  ```
  Scenario: state sync audit
    Tool: Bash
    Steps: Stage 3 완료 산출물 존재 여부와 ACTIVE-LOOP 상태 라인을 대조한다
    Expected: Stage 3 관련 모순 0개
    Evidence: .sisyphus/evidence/procedure-1-state-sync.txt

  Scenario: premature promotion guard
    Tool: Bash
    Steps: ACTIVE-LOOP와 promotion checklist에서 curated/stable 체크 여부를 검사한다
    Expected: execution evidence 없이 curated/stable 체크 없음
    Evidence: .sisyphus/evidence/procedure-1-promotion-guard.txt
  ```

  **Commit**: NO | Message: `docs(auth): sync stage3 closeout state` | Files: [`ACTIVE-LOOP.md`, `.sisyphus/boulder.json`]

- [x] 2. Freeze execution command matrix and environment prerequisites

  **What to do**: build/lint/type/test/verify/reject/chub를 어떤 명령으로 돌릴지, 어떤 디렉터리/환경/자격정보가 필요한지 고정한다.
  **Must NOT do**: 실행 도중에 명령을 바꾸거나, 필요한 비밀값/환경이 없는 상태를 뒤늦게 발견하게 두지 않는다.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: 실행 절차 고정
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 3,4,5,6 | Blocked By: 1

  **References**:
  - `.sisyphus/plans/auth-blueprint-execution.md`
  - `blueprints/pinned-versions.yaml`
  - `.sisyphus/drafts/auth-blueprint-quality-companion.md`
  - `.sisyphus/evidence/README.md`

  **Acceptance Criteria**:
  - [ ] build/lint/type/test/verify/reject/chub command가 문서화된다
  - [ ] 필요한 env/secret/mock dependency가 누락 없이 식별된다
  - [ ] evidence 파일명 규칙이 checklist 항목과 1:1 대응된다

  **QA Scenarios**:
  ```
  Scenario: command matrix completeness
    Tool: Bash
    Steps: 실행 대상 7종(build/lint/type/test/verify/reject/chub)의 명령 정의 여부를 점검한다
    Expected: 미정의 항목 0개
    Evidence: .sisyphus/evidence/procedure-2-command-matrix.txt

  Scenario: env readiness audit
    Tool: Bash
    Steps: 각 명령의 prerequisite(디렉터리, 의존성, secret, fixture) 목록 존재 여부를 확인한다
    Expected: hidden blocker 0개 또는 blocker가 명시적으로 기록됨
    Evidence: .sisyphus/evidence/procedure-2-env-audit.txt
  ```

  **Commit**: NO | Message: `docs(auth): freeze execution command matrix` | Files: [`.sisyphus/...`, `docs/...`]

- [x] 3. Resume implementation wave from `auth-blueprint-execution.md` Tasks 4-6

  **What to do**: 기존 execution plan을 active plan으로 취급하고 backend/frontend/verify skeleton 작업을 실제로 수행한다.
  **Must NOT do**: 이 단계에서 새 정책을 다시 설계하지 않는다.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: multi-surface implementation continuation
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 6 | Blocked By: 2

  **References**:
  - `.sisyphus/plans/auth-blueprint-execution.md` (Tasks 4-6)
  - `.sisyphus/drafts/auth-blueprint-scaffold-asset-set.md`
  - `.sisyphus/drafts/auth-blueprint-architecture-baseline.md`

  **Acceptance Criteria**:
  - [ ] backend/frontend/verify skeleton이 execution plan acceptance를 충족한다
  - [ ] 구현이 Stage 3 baseline 문서를 다시 뒤집지 않는다
  - [ ] evidence 생성 가능한 구조가 생긴다

  **QA Scenarios**:
  ```
  Scenario: scaffold realization check
    Tool: Bash
    Steps: execution plan task 4-6 산출물 파일 존재 여부를 검사한다
    Expected: skeleton 누락 없음
    Evidence: .sisyphus/evidence/procedure-3-scaffold-realization.txt

  Scenario: baseline drift audit
    Tool: Bash
    Steps: 구현 결과가 scaffold/architecture baseline의 must_not와 충돌하는지 점검한다
    Expected: drift 0개
    Evidence: .sisyphus/evidence/procedure-3-drift-audit.txt
  ```

  **Commit**: YES | Message: `feat(auth): resume execution wave skeletons` | Files: [`backend/...`, `frontend/...`, `verify/...`]

- [x] 4. Execute quality/test wave from `auth-blueprint-execution.md` Task 7

  **What to do**: Spring integration / React auth-state / verify triplet / key-flow E2E를 실제 테스트 자산과 실행 evidence로 만든다.
  **Must NOT do**: key-flow evidence 없이 quality companion이 충족된 것처럼 처리하지 않는다.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: test/evidence convergence
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: 5,6 | Blocked By: 3

  **References**:
  - `.sisyphus/plans/auth-blueprint-execution.md` (Task 7)
  - `.sisyphus/drafts/auth-blueprint-quality-companion.md`

  **Acceptance Criteria**:
  - [ ] 4축 테스트 자산과 실행 결과가 존재한다
  - [ ] golden/violation/false-positive verify 결과가 존재한다
  - [ ] key-flow E2E evidence가 quality baseline과 일치한다

  **QA Scenarios**:
  ```
  Scenario: test matrix execution
    Tool: Bash
    Steps: backend/frontend/verify/e2e 실행 명령을 수행하고 로그를 저장한다
    Expected: 각 축별 결과가 success/failure로 명확히 기록된다
    Evidence: .sisyphus/evidence/procedure-4-test-matrix.txt

  Scenario: verify triplet execution
    Tool: Bash
    Steps: golden/violation/false-positive fixture를 verify 엔진에 통과시킨다
    Expected: golden pass, violation reject, false-positive 0
    Evidence: .sisyphus/evidence/procedure-4-verify-triplet.txt
  ```

  **Commit**: YES | Message: `test(auth): execute quality baseline evidence` | Files: [`backend/...`, `frontend/...`, `verify/...`, `.sisyphus/evidence/...`]

- [x] 5. Run curated gate execution checks

  **What to do**: `chub` freshness, reject simulation, fail-open audit, build/lint/type/test/verify result 집계를 curated gate 전용 evidence로 만든다.
  **Must NOT do**: 일부 evidence만 있고 나머지가 비어 있는 상태를 curated 가능으로 해석하지 않는다.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: governance + evidence packaging
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: 6 | Blocked By: 2,4

  **References**:
  - `docs/plans/auth-blueprint-promotion-checklist.md`
  - `.sisyphus/drafts/auth-blueprint-curated-evidence-map.md`
  - `ACTIVE-LOOP.md`

  **Acceptance Criteria**:
  - [ ] `chub` freshness 결과가 저장된다
  - [ ] reject simulation 결과가 저장된다
  - [ ] build/lint/type/test/verify/fail-open evidence가 checklist와 연결된다

  **QA Scenarios**:
  ```
  Scenario: curated gate evidence completeness
    Tool: Bash
    Steps: checklist의 Draft -> Curated 항목과 evidence 파일 목록을 대조한다
    Expected: required evidence 누락 없음
    Evidence: .sisyphus/evidence/procedure-5-curated-completeness.txt

  Scenario: fail-open audit
    Tool: Bash
    Steps: freshness/reject/verify/build/lint/type/test 중 비어 있는 항목을 검사한다
    Expected: fail-open 항목 0개
    Evidence: .sisyphus/evidence/procedure-5-fail-open-audit.txt
  ```

  **Commit**: YES | Message: `docs(auth): assemble curated gate evidence` | Files: [`.sisyphus/evidence/...`, `docs/...`, `ACTIVE-LOOP.md`]

- [x] 6. Make the promotion decision

  **What to do**: 수집된 evidence만 근거로 `draft 유지` 또는 `curated 승격 가능`을 판정한다.
  **Must NOT do**: “거의 됐다” 식의 추정으로 상태를 올리지 않는다.

  **Recommended Agent Profile**:
  - Category: `oracle` — Reason: evidence-based go/no-go judgment
  - Skills: `[]`
  - Omitted: `[]`

  **Parallelization**: Can Parallel: NO | Wave 4 | Blocks: 7 | Blocked By: 5

  **References**:
  - `ACTIVE-LOOP.md`
  - `docs/plans/auth-blueprint-promotion-checklist.md`
  - `.sisyphus/evidence/...`

  **Acceptance Criteria**:
  - [ ] 판정이 evidence 파일에 직접 연결된다
  - [ ] curated 승격 또는 draft 유지 중 하나만 선택된다
  - [ ] 실패 시 re-entry target이 기록된다

  **QA Scenarios**:
  ```
  Scenario: promotion decision traceability
    Tool: Bash
    Steps: 최종 판정 문장과 evidence 파일 참조를 대조한다
    Expected: 판단 근거가 모두 파일 경로로 추적 가능하다
    Evidence: .sisyphus/evidence/procedure-6-decision-trace.txt

  Scenario: no-guess promotion audit
    Tool: Bash
    Steps: curated/stable 판정이 추정 문구 없이 evidence 근거로만 작성됐는지 검사한다
    Expected: guess-based wording 0개
    Evidence: .sisyphus/evidence/procedure-6-no-guess.txt
  ```

  **Commit**: YES | Message: `docs(auth): record curated promotion decision` | Files: [`ACTIVE-LOOP.md`, `docs/...`, `.sisyphus/evidence/...`]

## Final Verification Wave
- [x] F1. Procedure Compliance Audit — oracle
- [x] F2. Evidence Quality Review — unspecified-high
- [x] F3. Real Execution QA — unspecified-high
- [x] F4. Scope Fidelity Check — deep

## Commit Strategy
- 상태 정리/문서 정합성은 별도 commit 가능
- 실행 산출물과 해당 evidence는 같은 commit으로 묶는다
- promotion decision은 마지막 별도 commit으로 남긴다

## Success Criteria
- 사용자는 다음 실행의 전체 순서와 게이트를 혼동 없이 볼 수 있다.
- implementer는 Stage 3 closeout, execution, evidence, 승격 판정을 뒤섞지 않고 순차 수행할 수 있다.
- curated 가능 여부는 문서 존재가 아니라 실행 evidence로만 판정된다.
