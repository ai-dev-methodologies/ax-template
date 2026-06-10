# BACKLOG — canonical convergence ledger

> **이 파일이 ax-template의 유일한 canonical backlog다.** 2026-06-10 전략 검토(5-lens + adversarial
> challenge)의 단일 권고에 따라 신설: 59개 dogfood-ledger 아티팩트 + 17개 산업 dogfood(IDW1-17)의
> 분산 findings를 한 곳으로 수렴한다. 새 backlog 항목은 반드시 여기 등재하고, 닫을 때 `[x]` + closure
> ref를 남긴다. 상세 근거는 원본 ledger(`docs/dogfood-ledger/`)가 보존한다 — 이 파일은 인덱스 + 우선순위.

## 북극성 (2) 재정의 — backlog convergence

기존 "100% policy completeness (zero gaps)"는 17개 산업 dogfood가 **매 산업마다 새 correctness
signature를 발견**(17/17)함으로써 경험적으로 반증되었다 — 발견 루프는 비종결적(non-terminating)이다.
따라서 종착점을 다음으로 재정의한다:

> **Backlog convergence: 이 파일의 P0–P3 항목 중 K건 closed / N건 전체 = 수렴률.
> 신규 산업 dogfood(IDW18+)는 수렴률 ≥ 70% 전까지 동결(freeze).**

- "새 도메인/룰 추가는 정상 활동"(vision #2)과의 관계: **추가가 정상인 것과 발견이 무한인 것은
  다르다.** 이 backlog 안 항목의 spec화·구현은 계속 정상 활동이며 vision과 충돌하지 않는다.
  동결 대상은 *새 산업을 열어 새 signature를 찾는 행위*뿐이다.
- 집계 기준(2026-06-10, disk-truth): dogfood-ledger YAML 260 findings = real_bug 89(전건
  closure_commit_sha 보유) + scope_deferral 166(trigger-bound, §P4) + methodology_gap 5.

## 현재 수렴률

| Tier | 전체 | closed | 수렴률 |
|---|---|---|---|
| P0 (expiry-bound / live defects) | 26 | 1 | 4% |
| P1 (generic signature backlog) | 54 | 0 | 0% |
| P2 (verification escapes) | 9 | 1 | 11% |
| P3 (industry-niche deferrals) | 31 | 0 | 0% |
| **P0–P3 합계 (수렴 분모)** | **120** | **2** | **~2%** |
| P4 (trigger-bound deferrals — 분모 제외) | 166 | — | by-design |

---

## P0 — expiry-bound 부채 + live catalog defects (최우선)

DDD allowlist 예외 20건은 **expiry 2026-12-31** — 만료 6개월 전(2026-07) 착수 필요.

- [ ] **P0-1 ~ P0-11** AX-DDD-AUTH-USER ×11 — auth↔user cross-aggregate coupling
      (`practices/evals/aggregate_boundary_allowlist.yaml`). Remediation: `UserLookupPort` SPI 공개
      → AuthServiceImpl/OAuthService가 port 경유 → token은 reference-by-id. defer_imw 판정
      (2026-06-08 design workflow) — IMW로 닫는다.
- [ ] **P0-12 ~ P0-20** AX-DDD-MEMBER-REPO ×9 — member-repo 직접 접근 grandfather.
      Pattern B(visibility 축소)는 예외를 retire하지 못함이 검증됨(HG-AGG-REPO는 visibility-agnostic)
      — mutate-through-root 감사 후 각 repo에 end-state 결정 필요.
- [ ] **P0-21** IDW11-G4 — provenance-DAG vs temporal-validity edge-supersession: 기존 두 룰이
      edge 교체 시나리오에서 서로 모순되는 live defect (ledger: IDW11 session).
- [ ] **P0-22** IDW11-G23 — `no-userid-param` 룰이 shared-terminal attribution(키오스크 POS 등
      한 세션 다중 실사용자)을 over-ban하는 live defect.
- [ ] **P0-23** IDW13-G14 — multi-tenant manifest가 정의되지 않은 `Tenant` entity를 참조하는
      live defect (blueprint dangling reference).
- [ ] **P0-24** IDW13-G15 — tenant quota `@Check`에 no-shrink-path: 한 번 늘린 quota를 줄일
      경로가 스키마상 없음 (live defect).
- [x] **P0-25** IDW17-G1 — threshold-terminal-derivation spec rescue + FULL 6-step closure.
      **closed 2026-06-10**: `specs/threshold-terminal-derivation-l0.yaml`(5 items) + rule
      `limit-crossing-drives-irreversible-terminal-and-blocks-derived-use`(14 CFR §43.10 live-fetch,
      Java 188) + `thresholdterminal` 도메인(@Check 함의 + sole-mutator FSM zero-outgoing-edges +
      same-tx crossing + fail-closed use + V042) + `testThresholdTerminal` GREEN(Compliance 5 +
      ViolationProof 5, 8-thread 동시성 keystone). spec rescue를 넘어 전체 closure로 완료.
- [ ] **P0-26** Payment#setState raw public setter → SubscriptionStateMachine처럼 sole-mutator로
      (현재 governed_state_mutators로 관리되는 governed 부채; `Payment.java:130`).

## P1 — generic signature backlog (cross-industry, 산업 dogfood가 발견)

각 항목은 해당 IDW 세션에서 "generic = 2개 이상 산업 적용 가능" 판정을 받은 것. 닫는 방법은
6-step 표준(METHODOLOGY + spec → rule(evidence) → domain/guard → ViolationProof → per-domain task →
R25). *이름이 세션 기록에만 있던 항목을 여기로 영구화했다.*

**결정-거버넌스 / 변경-통제 계열**
- [ ] P1-1 IDW10 decision-governance trio: 산정-근거 보존(quote-basis snapshot)
- [ ] P1-2 IDW10 decision-governance: 재산정 시 버전·사유 강제
- [ ] P1-3 IDW10 decision-governance: 수동 override 시 4-eyes + 근거
- [ ] P1-4 IDW16-G2 override-with-justification (gate 무시 시 정당화 기록 — EMR/금융 공통)
- [ ] P1-5 IDW16-G4 executed-matches-authorized (집행본 ↔ 승인본 일치 검증)
- [ ] P1-6 IDW17 four-eyes-signoff (서명 2인 분리, approval-workflow의 일반화)
- [ ] P1-7 IDW12-G4 reproducible-draw (감사 가능한 난수 추출 = capability-token의 negative)
- [ ] P1-8 IDW12-G6 blinding (역할별 필드 차폐 — 임상 외 HR/입찰에도 적용)
- [ ] P1-9 IDW16-G15 reproducible-classification (동일 입력 → 동일 분류 보장)

**기한/시한 계열**
- [ ] P1-10 IDW12 deadline-ladder (G14-17: 다단 기한 + 단계별 escalation)
- [ ] P1-11 IDW17 multi-axis-deadline (복수 축(시간/사용량/사이클) 중 선도래 기한)
- [ ] P1-12 IDW17 deadline-grounding (기한의 산출 근거를 도출식으로 고정)
- [ ] P1-13 IDW16-G3 closed-loop ack-obligation (critical-result 수신확인 의무: additive
      escalation, never auto-expire)

**수량/보존 계열 (conservation family 잔여)**
- [ ] P1-14 IDW14 estimate→actual supersession (추정치→실측치 대체 + 재계산)
- [ ] P1-15 IDW14 run-version recompute (산정 run 버전 + 멱등 재계산)
- [ ] P1-16 IDW14 delta-true-up (기간 경계 보정 정산)
- [ ] P1-17 IDW14 grid-completeness (고정 그리드 결측 검출)
- [ ] P1-18 IDW14 net-metering signed-dual-register (양방향 부호 레지스터)
- [ ] P1-19 IDW14 tri-state-sealed-period (open/closed/sealed 기간 3-상태)
- [ ] P1-20 IDW13-G9 internal-stage exactly-once (내부 스테이지 정확히-한-번 전이)
- [ ] P1-21 IDW13 billing trio: dunning (단계적 미납 처리)
- [ ] P1-22 IDW13 billing: aging (채권 연령 버킷)
- [ ] P1-23 IDW13 billing: cure-period (시정 유예 기간)
- [ ] P1-24 IDW15-G2 novation (계약 당사자 교체 보존)
- [ ] P1-25 IDW15-G3 DvP (동시이행 — 양 leg 원자 결제)
- [ ] P1-26 IDW15-G4 finality (결제 완결성 — 취소 불가 시점)
- [ ] P1-27 IDW15-G5 fail-ladder (실패 단계 사다리)
- [ ] P1-28 IDW15-G6 as-of-snapshot (기준시점 스냅숏 조회)
- [ ] P1-29 IDW15-G7 fan-out (1 run → N 산출 보존)
- [ ] P1-30 IDW15-G9 rebase (기준 재설정 + 이력 보존)
- [ ] P1-31 IDW15-G11 deemed-election (무응답 시 간주 선택)
- [ ] P1-32 IDW15-G12 external-reconciliation (외부 대사)

**매칭/정합 계열**
- [ ] P1-33 IDW16-G5 fuzzy-match governance (유사 매칭의 임계·감사)
- [ ] P1-34 IDW16-G7 merge/survivorship (중복 엔티티 병합 + 생존 필드 규칙)
- [ ] P1-35 IDW16-G11 multi-check-battery (다중 점검 배터리 — 전 항목 통과 게이트)
- [ ] P1-36 IDW16-G12/G13 positive-gates (필수 동반 조건 게이트 2종)
- [ ] P1-37 IDW17 one-mandate-fanout (하나의 지시 → N 작업 전개 + 완료 회수)
- [ ] P1-38 IDW8 filter/sort field-allowlist (정렬·필터 파라미터 화이트리스트 — 보안 후속)

**dispatch/예약 계열 (IDW9 잔여)**
- [ ] P1-39 IDW9-G6 / P1-40 G7 / P1-41 G8 / P1-42 G11 / P1-43 G12 / P1-44 G13 / P1-45 G16
      (timed-offer 일반화 잔여 7건 — 상세는 IDW9 세션 기록)

**IDW11/12/17 미명명 잔여 (이름 복원 필요 — 차기 정리 시 세분화)**
- [ ] P1-46 ~ P1-54 IDW11 generic 잔여(~9) · IDW12 잔여(~7) · IDW17 잔여(~9) 중 우선 9건 —
      각 IDW 재감사로 이름 복원 후 본 섹션에 정식 등재 (복원 전에는 1건씩만 카운트)

## P2 — verification escapes (검증 체계 자체의 갭)

- [ ] P2-1 **evidence_guard quote-truth 미검증** — 구조만 검사, 본문 대조 없음 → 인용 조작이 전
      gate 통과 (`evidence_guard.sh:89-116`). Remediation: 헤더에 "STRUCTURE not TRUTH" 명시 +
      랜덤 N건 snapshot 본문 대조 spot-check guard(advisory) 신설.
- [ ] P2-2 ESLint warn→error 승격: `no-server-state-in-local-state`, `no-god-route`
      (`eslint-plugin-ax/index.js:64-65`) — 측정 기반 기한 부여.
- [ ] P2-3 enforcement-surface map 문서화 — commit-blocking / push-blocking / manual 3분류를
      CLAUDE.md 또는 verify 문서에 명시 (75-guard 전체는 수동 run-all-guards 전용임을 포함).
- [ ] P2-4 R25 체크리스트에 frontend lint 단계 추가 (현재 backend만).
- [ ] P2-5 aggregate `./gradlew test` advisory의 root-cause(ContextCache isolation) 종결 —
      2g heap pin은 workaround.
- [x] P2-6 doc_headline_count_guard가 `skills/ax-transform/SKILL.md`를 미검사 → stale counts
      147/86 노출. **closed 2026-06-10 (이번 wave: counts 187/99 수정 + guard 확장).**
- [ ] P2-7 practices-react/DECISIONS.md 신설 (Java 측 438줄과 비대칭 — React 룰 provenance 부재).
- [ ] P2-8 검증 비용 시계열(perf-log.jsonl) + 40-도메인 외삽 시 CI sharding 설계.
- [ ] P2-9 NUMERIC(19,4) overflow seam (catalog-wide) — `@Digits(integer=15)` 두 입력의 합이 컬럼
      정밀도를 넘으면 unmapped DataIntegrityViolation → 500. register/costshare/netting/
      thresholdterminal 등 모든 NUMERIC(19,4) 도메인 공통. 도메인별 패치가 아니라 공통 가드
      (service-level bound 또는 공용 advice)로 닫을 것. (P0-25 adversarial review 발견, 2026-06-10)

## P3 — industry-niche deferrals (generic 아님 — 낮은 우선순위)

- [ ] P3-1 ~ P3-8 IDW6 logistics-niche ×8
- [ ] P3-9 ~ P3-11 IDW7 fintech-ledger-niche ×3
- [ ] P3-12 ~ P3-15 IDW8 HR/payroll-niche ×4
- [ ] P3-16 ~ P3-21 IDW10 insurance-niche ×6 (G9, G11-G15)
- [ ] P3-22 ~ P3-31 IDW13/14/15/16/17 niche 잔여 ×10 (각 세션 기록 참조)

## P4 — trigger-bound scope_deferrals (수렴 분모 제외; by-design)

dogfood-ledger YAML의 scope_deferral 166건은 **의도적 설계 보류**다 — 각각 "Expiry trigger: …"
조건이 명시되어 있고, fork-receiver의 상황이 그 trigger에 도달할 때만 재개봉한다. 닫아야 할 갭이
아니라 문서화된 경계이므로 수렴률 분모에서 제외한다. 인덱스(파일별 건수):

r97–r115 e-commerce/generic 시리즈 ×7씩(일부 6) = 124 · email-outbox 6 · r93 multi-tenant 10 ·
approval-workflow 5 · activity-feed 4 · favorites-bookmarks 4 · meta-catalog 1 외 —
상세 전문은 `docs/dogfood-ledger/*.yaml` (HIGH 45 / MEDIUM 다수 / LOW 일부).

## 운영 규약

1. **등재**: 새 발견은 분류(P0–P4) + 출처(ledger 경로 또는 세션) + 한 줄 정의로 등재.
2. **닫기**: `[x]` + closure commit sha (real_bug성 항목은 dogfood guard와 동일하게 sha 필수).
3. **동결 규칙**: IDW18+(신규 산업 dogfood)은 P0–P3 수렴률 ≥ 70% 전까지 시작하지 않는다.
   단, fork-receiver의 실사용 피드백으로 들어오는 발견은 동결과 무관하게 항상 등재·처리한다.
4. **수렴률 갱신**: 항목을 닫는 commit이 위 "현재 수렴률" 표를 함께 갱신한다.
