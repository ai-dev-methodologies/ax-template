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
| P0 (expiry-bound / live defects) | 26 | 26 | **100%** |
| P1 (generic signature backlog) | 62 | 62 | **100%** |
| P2 (verification escapes) | 20 | 16 | **80%** |
| P3 (industry-niche deferrals) | 47 | 6 | ~13% |
| **P0–P3 합계 (수렴 분모)** | **155** | **110** | **~71%** |

> 2026-06-27 Broadleaf 전면 재감사가 P1 +6·P2 +1 등재(75%→72%). 2026-06-28 `feat/commerce-invariant-closure`가 잔여 5 Broadleaf gap(P1-56~60: offer-eligibility·tax-application·currency-arithmetic·password-reset token-family·checkout saga doc)을 generic 도메인+외부표준 anchor로 전부 closed → P1 60/60, 수렴 **76%**. Broadleaf 재감사 8 confirmed gap 전수 종결.
> 2026-07-07 STO-arc 파생 잔여 6건(P1-61~62·P2-14~15·P3-32~33) 등재 → P1 60/62·P2 13/15·P3 0/33, 수렴 **~73%**.
> 2026-07-07 P3 인라인화 — P3-1~21 확정 요지·P3-22~40 IDW13-17 세션기록 대조(EMR G9 cross-list record-linkage→P1-33~34 closed 제외, 불확실 6건 "(closure 여부 미검증)" 표기, disk-truth 재집계) + P3-32→P3-41·P3-33→P3-42 재번호 → P3 0/42, 수렴 **~68%**.
> 2026-07-10 ultracode dogfood 감사 — P1/P2 3건은 PR #74로 즉시 봉합(backlog 미경유), 잔여 doc-drift·잠재버그 P3-43~46 등재 → P3 0/46, 수렴 **~66%**.
> 2026-07-10 P3-43~46 당일 봉합 — crud-security 참조 정정·guard 카운트 disk-truth 방법론 명시(89 run-all + 1 recency = 90)·settings.local.json 정정·ax-prove 이중 echo fix → P3 4/46, 수렴 **~69%**.
> 2026-07-13 backlog-convergence-wave (1/2) — P3-41/42 base-repo maintainer DECISION으로 결정-닫기(reversible) + 미등재 잔여 2건 정식 등재(P2-17 pre-push fixture 승격, P3-47 guard[76] zero-agent-events fixture) → 분모 152, 수렴 105/152 **~69%**. 후속 봉합(P1-61~62·P2-14~16·P2-1a)은 본 wave (2/2)에서.
> 2026-07-13 backlog-convergence-wave (2/2) — 7건 봉합 완료: P1-61(33b85a1 동시 issue keystone)·P1-62(1b5a92d evidence-backed doc closure — **P1 62/62 100% 도달**)·P2-14(34a1742 guard[87] neuter 어휘)·P2-15(0dde4c8 commit-msg 훅)·P2-16(180da35 lockfile+preflight)·P2-1a(a401a30 B3, 분모 불변)·P3-41/42(bc3e51c DECISION). P2-1a census가 Java-side 스냅숏 본문 87건 미커밋을 판명 → P2-18 정직 등재(분모 +1). 최종 수렴 **110/155 ≈ 71%** — 북극성(2) 동결 해제선(70%) 상회. ralplan 컨센서스(Planner+Architect+Codex Critic APPROVE) → ralph 실행, 전 봉합 R25/게이트 검증. cross-family 완료리뷰 2라운드에서 P1급(강제 우회) 2건 실증·봉합: guard[87] 따옴표 안 명령치환 우회 + manifest 0-item 조용한 공허통과(자기결함). 잔여 P2급 2건(P2-19 shape-내부 오도성·우회 fixture 승격, P2-20 evidence 캐비엇 기계강제)은 정직 등재 후 이월(분모 +2).
| P4 (trigger-bound deferrals — 분모 제외) | 166 | — | by-design |

---

## P0 — expiry-bound 부채 + live catalog defects (최우선)

DDD allowlist 예외는 **ecom-composition 1건만 잔존** (composition은 설계상 의도된 grandfather; expiry 2026-12-31). **P0 전 항목 closed (2026-06-10).**

- [x] **P0-1 ~ P0-11** AX-DDD-AUTH-USER ×11 — **closed 2026-06-10 (IMW, 3-wave)**:
      (A) RefreshToken/VerificationToken reference-by-id (`@ManyToOne UserEntity` →
      `@Column(name="user_id") UUID userId` — 스키마 동일, V028 FK 유지) + repo by-id;
      (B) ProviderLink+Repository+OAuthProvider를 user→auth로 이전(OAuth identity-link는
      auth-domain data) + by-id; (C) `user.UserAccountService` @PublishedApi use-case port
      (authenticate/register/registerOAuth/markEmailVerified/resetPassword/changePassword/조회;
      `UserAccountDto.hasPassword`가 raw hash 노출을 대체 — credential 검증은 aggregate 곁으로).
      allowlist 예외 21→10; AuthServiceImpl verifyEmail/resetPassword는 더 이상 god-service가
      아니게 되어 governed_god_service 4→2 (bijection이 강제). testPractices+testAsvs(26)+
      smoke E2E GREEN.
- [x] **P0-12 ~ P0-20** AX-DDD-MEMBER-REPO ×9 — **closed 2026-06-10 (wave-4)**: 어떤
      @AggregateMember도 repository를 갖지 않음. member READ는 ROOT repository의 명시적 JPQL
      @Query로 이관(derived-name은 root 타입에서 파싱 불가), member WRITE는 공유
      `common/MemberWriter`(EM persist/persistAndFlush/find — OptimisticLockingSupport 전례)
      seam을 root 서비스가 사용. ApprovalStepRepository는 dead(메서드 0/호출 0)라 단순 삭제.
      netting의 독립 cross-check(repo-SUM ≠ in-memory 경로)는 root-repo JPQL로 보존.
      bijection이 allowlist 정리를 강제 — **예외 10→1**(ecom composition만 잔존). 8개
      per-domain task + testPractices GREEN.
- [x] **P0-21** IDW11-G4 — **closed 2026-06-10**: provenance-dag 룰에 'edge supersession'
      composition note(정정/종료 = supersedes_edge_id를 단 NEW append row, rollup은 LIVE edge만) +
      temporal-validity 룰에 상호 노트(append-only 아티팩트는 supersession-append로 window 종료,
      일반 effective-dated 테이블은 UPDATE-open-window 유지) — 두 룰이 compose, 모순 해소.
- [x] **P0-22** IDW11-G23 — **closed 2026-06-10**: no-userid 룰에 'shared-terminal operator
      attribution은 금지 대상 아님' scope 절 — 인가 주체는 Authentication ONLY, attribution-only
      operator 필드는 4개 안전조건(a-d: 인가 비사용/리소스 선택 금지/operatorBadgeId 명명/감사
      병기) 하에 합법.
- [x] **P0-23** IDW13-G14 — **closed 2026-06-10**: manifest TenantCatalog 계약이 존재하지 않는
      'Tenant entity table' 대신 fork-receiver-defined catalog store(테이블/레지스트리/설정)를
      명시 — listActive() 시맨틱 + 합법적 scoping-bypass만 고정.
- [x] **P0-24** IDW13-G15 — **closed 2026-06-10**: plan-downgrade 계약을 spec 헤더 + 룰에 명시 —
      limit_value는 used>new-limit 상태에서도 하향 가능(합법·quiescent; 클레임이 이미
      used+delta<=limit로 거부), `CHECK(used<=limit_value)`는 다운그레이드를 표현 불가능하게
      만드는 TRAP으로 명명·금지.
- [x] **P0-25** IDW17-G1 — threshold-terminal-derivation spec rescue + FULL 6-step closure.
      **closed 2026-06-10**: `specs/threshold-terminal-derivation-l0.yaml`(5 items) + rule
      `limit-crossing-drives-irreversible-terminal-and-blocks-derived-use`(14 CFR §43.10 live-fetch,
      Java 188) + `thresholdterminal` 도메인(@Check 함의 + sole-mutator FSM zero-outgoing-edges +
      same-tx crossing + fail-closed use + V042) + `testThresholdTerminal` GREEN(Compliance 5 +
      ViolationProof 5, 8-thread 동시성 keystone). spec rescue를 넘어 전체 closure로 완료.
- [x] **P0-26** Payment#setState — **closed 2026-06-10**: PaymentStateMachine.apply(payment,event)
      sole-mutator seam + forceVoid 감사형 admin escape hatch(LEGAL 맵 불변 — /void 엔드포인트
      시맨틱 보존). setter package-private; 14 call-site 이관; governed_state_mutators 2→0
      (bijection 수용 = retire 증명). testPayment GREEN.

## P1 — generic signature backlog (cross-industry, 산업 dogfood가 발견)

각 항목은 해당 IDW 세션에서 "generic = 2개 이상 산업 적용 가능" 판정을 받은 것. 닫는 방법은
6-step 표준(METHODOLOGY + spec → rule(evidence) → domain/guard → ViolationProof → per-domain task →
R25). *이름이 세션 기록에만 있던 항목을 여기로 영구화했다.*

**결정-거버넌스 / 변경-통제 계열**
- [x] P1-1~3 IDW10 decision-governance trio — **closed 2026-06-10 (wave-5)**:
      `specs/decision-governance-l0.yaml`(5 items DG-BASIS/RECOMPUTE/OVERRIDE/CHAIN/CONCURRENT) +
      rule `computed-decision-versioned-basis-and-four-eyes-override`(Java 189; ASOP 41 §3.2 +
      NIST SP 800-192 SOD live-fetch) + `decisiongov` 도메인 — basis 불변 스냅숏, 재산정=reason
      필수 append(덮어쓰기 불가), override=정당화+4-eyes(approver≠actor, **@Check DB-backstop**),
      8-thread 동시성 keystone. MEMBER-REPO end-state를 탄생부터 적용(MemberWriter+root JPQL).
      testDecisionGov GREEN.
- [x] P1-4 IDW16-G2 override-with-justification — **closed 2026-06-10**: 동일 메커니즘
      (DG-OVERRIDE-001의 justification 강제)이 gate-무시 정당화 기록을 포괄.
- [x] P1-5 IDW16-G4 executed-matches-authorized — **closed 2026-06-16 (parallel wave, authzparity 도메인)**: SHA-256 parity hash로 실행 파라미터가 승인 envelope와 일치해야 집행 가능, 불일치 409 PARITY_MISMATCH + blocked 기록
- [x] P1-6 IDW17 four-eyes-signoff — **closed 2026-06-16 (authzparity)**: 고가 액션은 distinct 2인 승인(requester≠approver1≠approver2, @Check approver<>requester DB backstop)
- [x] P1-7~9 — **closed 2026-06-16 (parallel wave2/3), reproducibility 도메인**: 서버생성 seed(SecureRandom, CWE-330) 기록→동일 seed 재현 byte-identical(PROC-REPLAY, divergence 422) + classifier 버전 pin(동일 input+version 멱등, uq(input_hash,version,kind), 이력 무-재라벨) + role-blinding(@JsonIgnore raw, ADMIN unmask). V050, testReproducibility 14/14. 앵커 NIST SP 800-90A DRBG + SP 800-53 least-privilege.

**기한/시한 계열**
- [x] P1-10~13 — **closed 2026-06-11 (wave-6, ONE obligation 도메인)**:
      `specs/deadline-obligation-l0.yaml`(OBL-GROUND/AXIS/ACK/LADDER/CONCURRENT-001) + rule
      `deadline-obligation-grounded-multi-axis-ladder-closed-loop`(Java 190; 14 CFR 91.409
      (a)+(b) 2축 쌍 + PMC7510293 closed-loop, live-fetch) + `obligation` 도메인 — (12) 기한=
      기록된 도출식(raw deadline 필드 API에 부재, DerivationRecord append) / (11) multi-axis
      min(candidates)+사용량 advance 재도출 / (10) APPROACH→IMMINENT→BREACH exactly-once
      (uq(obligation,rung) DB backstop) additive / (13) ack(who/when)만 terminal — EXPIRED
      상태 자체가 없음, sweep은 절대 종결 불기록. 8-thread 동시 sweep keystone. V044,
      testObligation GREEN.

**수량/보존 계열 (conservation family 잔여)**
- [x] P1-14~17 + P1-19 — **closed 2026-06-11 (wave-8, ONE trueup 도메인)**:
      `specs/remeasurement-trueup-l0.yaml`(TUP-SUPERSEDE/RUNVERSION/DELTA/GRID/SEALED/CONCURRENT-001)
      + rule `remeasurement-supersession-versioned-recompute-and-trueup`(Java 192; IAS 8
      prospective-recognition verbatim ×2 + PJM Manual 29 §1.5 net-adjustment via pdftotext,
      live-fetch) + `trueup` 도메인 — (14) reading 불변 + supersession=새 행+forward pointer
      +slot_version+1, ACTUAL→ESTIMATED 강등 422 / (15) run 버전 per period + input basis
      (reading rows@versions) 기록 + basis-hash 동일시 멱등(phantom version 0) / (16) CLOSED
      기간 보정 = run-of-record 불가침, NET delta가 OPEN 기간으로 forward 포스팅, 보존
      run_of_record+Σpostings==latest (repo-SUM 독립 도출) / (17) 고정 그리드 결측 → 422
      (슬롯 명명), gap-fill은 명시적 + 방법 기록 / (19) OPEN→CLOSED→SEALED 단방향, SEALED
      fail-closed 409, @Check closed⇒run-of-record. 8-thread 동시 recompute keystone
      (ONE version + ONE posting, uq 백스톱). V046, testTrueUp 13/13 GREEN.
- [x] P1-18 — **closed 2026-06-16 (parallel wave2/3), netmetering 도메인**: 양방향 monotone register(import+/export−, 후퇴 422) + 파생 net=Σimport−Σexport(독립 recompute cross-check) + billing-period close 불변(409). V054, testNetMetering 10/10. 기존 register 패키지 무수정.
- [x] P1-20~23 — **closed 2026-06-16 (parallel wave, dunning 도메인)**: 일방향 dunning 사다리 REMINDER→NOTICE→FINAL_NOTICE→SUSPENDED exactly-once(uq(case,stage,kind)+PESSIMISTIC_WRITE keystone) / 결정적 aging 버킷(as-of+basis 기록) / cure-period grace(완납 시 CURED+ladder halt, lapse 재개). V047, testDunning GREEN
- [x] P1-24~27 — **closed 2026-06-16 (parallel wave, settlement 도메인)**: DvP 양 leg 원자 결제(partial 표현불가 @Check) / novation 당사자 교체 obligation 보존(append-only) / irrevocable finality(이후 novation/cancel 409 @Check) / fail-ladder PENDING→FAILED→RETRY→BUYIN. V048, testSettlement GREEN
- [x] P1-28~30 — **closed 2026-06-16 (parallel wave2/3), valuationrun 도메인**: as-of 스냅숏(run as-of≤T 최댓값, 불변 run·correction=새 run) + fan-out N output Σ==run total(독립 SUM cross-check) + rebase 새 baseline+prior 보존(forward pointer). V052, testValuationRun 12/12.
- [x] P1-31 / P1-35 / P1-37 — **closed 2026-06-16 (parallel wave2/3), mandate 도메인**: one-mandate-fanout(N child 원자생성, 완료=Σterminal==N 파생 recall, @Check partial 불가) + multi-check-battery(전 check 통과 전 SATISFIED 불가 422, per-check verdict 기록) + deemed-election(@Scheduled @Lazy-self sweep, deadline 무응답→DEEMED exactly-once). V051, testMandate 19/19.
- [x] P1-32 — **closed 2026-06-16 (parallel wave2/3), reconciliation 도메인**: feed vs 내부 MATCHED/BREAK/INTERNAL_ONLY/EXTERNAL_ONLY 분류(basis 기록) + BREAK는 명시 disposition 후에만 RESOLVED(422) + 멱등 재실행(uq(run,item), feed-hash). V053, testReconciliation 10/10. 앵커 PCAOB AS 2305.

**매칭/정합 계열**
- [x] P1-33~34 — **closed 2026-06-11 (wave-7, ONE recordlinkage 도메인)**:
      `specs/record-linkage-l0.yaml`(LINK-BAND/REVIEW/SURVIVOR/RESOLVE/CONCURRENT-001) + rule
      `record-linkage-banded-verdict-and-survivorship-merge`(Java 191; Coleridge 교과서
      Fellegi-Sunter 3-class verbatim + PMC2815491 merge 감사추적, live-fetch) + `recordlinkage`
      도메인 — score/per-field breakdown/threshold가 proposal row에 기록(bare verdict 표현불가),
      REVIEW 밴드는 인간 confirm/reject(who/when)만, AUTO도 동일 trail, merge는 per-field
      survivorship 기록 + loser tombstone(포인터, **delete 경로 자체가 없음**), chained resolve
      cycle-safe, ascending-id lock order. 8-thread confirm race keystone. V045,
      testRecordLinkage GREEN.
- [x] P1-36 IDW16-G12/G13 positive-gates — **closed 2026-06-16 (authzparity)**: 액션이 필수 동반 게이트 집합을 선언, 전 게이트 satisfied 기록 전엔 집행 불가(422) — missing companion은 executed-불가
- [x] P1-38 — **closed 2026-06-16 (wave4, queryguard 도메인)**: 리소스별 sort/filter 필드 allowlist(public→internal 매핑) — 비-allowlist 필드 422(필드 명명), 닫힌 direction/operator enum, raw 필드/SQL이 Sort.by·쿼리에 도달 불가. 앵커 OWASP API3:2023 + CWE-89/639.

**dispatch/예약 계열 (IDW9 잔여)**
- [x] P1-39~41 — **closed 2026-06-16 (parallel wave2/3), timedoffer 도메인**: timed-offer(deadline까지 OPEN, @Lazy-self sweep로 EXPIRED exactly-once) + exclusive-assignment(subject당 최대1 ACCEPTED, 동시 accept 1승 409, uq backstop) + re-offer ladder(append-only, prior 참조). V055, testTimedOffer 14/14. 기존 dispatch 패키지 무수정.
- [x] P1-43 — **closed 2026-06-16 (wave4, sensitiveaccess 도메인)**: @SensitiveField read는 반환 전 append-only access log(누가/언제/무엇/목적) 기록 — @Phi에서 분리한 generic read-audit, 무기록 reveal 불가. 앵커 NIST SP 800-53 AU-2/AU-3.
- [x] P1-44 / P1-45 — **closed 2026-06-16 (parallel wave5/6), accessgrant 도메인**: 시한부 ReBAC grant((subject,resource,relation)+validFrom/Until, 만료/철회 fail-closed 403, expired=재계산 predicate·저장 boolean 없음) + 다중 자격 만료 게이트(전 credential 현재유효 AND, 결손시 클래스 명명 403). V057, testAccessGrant 13/13. 앵커 NIST SP 800-53 AC-2/AC-3 + ABAC.
- [x] P1-42 — **closed 2026-06-16 (parallel wave7), inputplausibility 도메인**: 자가보고 server-unverifiable 입력의 plausibility 게이트(범위 + |delta|>rate*elapsed 변화율) + VerificationStatus는 SELF_REPORTED_UNVERIFIED만 존재(CONFIRMED 없음 = 구조적 불신), 거부도 REQUIRES_NEW로 기록. V064, testInputPlausibility 12/12. 앵커 OWASP input-validation + CWE-20/1284.

**IDW11/12/17 미명명 잔여 (이름 복원 필요 — 차기 정리 시 세분화)**
- [x] P1-46~50 — **closed 2026-06-16 (parallel wave5/6) (IDW11/17 residual 복원→5 도메인)**: (46) inventoryreservation 2축 available=on_hand−reserved + reserve→commit→release hold(보존 @Check, 8-thread keystone) / (47) orgscope 계층 org-tree containment-scope authz(노드 grant가 subtree로 cascade, 형제/조상 403) / (48) variancegate standard-vs-actual 비대칭 tolerance-band 게이트 + breach disposition(verdict 무재작성) / (49) statemutation state-conditional mutability(현 상태가 mutable 필드집합 결정, monotone tightening, TOCTOU 재검) / (50) recurringinterval 완료 시 윈도우 reset(다음을 완료시점부터, OVERDUE 재계산). V059~063, 각 per-domain GREEN. 앵커 APICS/SPC/NIST RBAC-hierarchy 등.
- [x] P1-51~52 — **closed 2026-06-16 (parallel wave7) (IDW12/11 residual 복원→2 도메인)**: (51) calendardeadline 법정기한 calendar vs business-day 산술(주말+versioned 공휴일 제외, roll convention 기록, overdue 재계산; V065) / (52) orderquantization 비보존 round-UP-to-order-multiple(MOQ/lot-size, orderQty=max(MOQ,ceil(req/mult)*mult), overage 명시 기록 — rounded-split의 비보존 대척; V066). testCalendarDeadline/testOrderQuantization GREEN. 앵커 ISDA business-day convention / APICS lot-sizing.
- [x] P1-53~54 — **closed 2026-06-16 (parallel wave8 FINAL, IDW11 residual 복원→2 도메인)**: (53) uomconversion cross-dimension UoM 변환 — same-dim 순수비율 vs cross-dim은 recorded material density bridge 필수(불가시 422 INCOMPATIBLE_DIMENSIONS), basis+versioned, 멱등 byte-identical; V067 / (54) divisibility per-material INTEGER_ONLY(분수 422 거부 — 반올림 아님, orderquantization 대척) vs FRACTIONAL(max-scale 422); V068. testUomConversion/testDivisibility GREEN. 앵커 NIST SP 811 §7 / APICS UoM. **→ P1 54/54 (100%) 도달.**

> **Broadleaf 흡수여부 전면 재감사 (2026-06-27, ultracode 43-agent) 신규 등재** — 29 per-item 중 28 HONEST/refuted, critic 2 → 총 8 confirmed. #1 bundle(아래 P1-55 closed)·#2 hi-lo(covered_elsewhere로 ledger 해소)·#7 guard-granularity(P2-13 closed) 는 본 세션 처리. 나머지 5건 ↓.
- [x] P1-55 — **closed 2026-06-27 (Broadleaf re-audit, 본 세션)**: bundle/kit composite-item CONSERVING price roll-up — `bundle-pricing-l0` 신설. ITEM_SUM 모드 bundle price == Σ(child.unitPrice × qty) + Σ fees, BUNDLE 모드 고정가, taxability/availability child 파생. 독립 구현(CompositeItem/CompositeComponent/BundlePricingService sole-mutator) + RestAssured Σ-of-parts 보존 테스트 + ViolationProofTest + V076. 앵커 BundleOrderItemImpl.getRetailPrice + ASC 606/IFRS 15 transaction-price allocation. composition 방향(banded-pricing-l0 decomposition의 대척).
- [x] P1-56 — offer qualifier→target/segment eligibility *(Broadleaf re-audit 2026-06-27, MEDIUM)*: BOGO qualifier→target min-qty 매칭(OfferItemCriteria/QuantityBasedRule) + customer-xref/segment eligibility 게이트(CustomerOffer). promotion-l0는 'applicable offers를 INPUT으로' 받음(math만) — WHO/WHICH 적격성 미커버. done-when: promotion-l0 확장 or offer-eligibility-l0 (qualifier 미달→target 미할인 + 비적격 고객 무offer 테스트). **[closed 2026-06-28, feat/commerce-invariant-closure]**
- [x] P1-57 — tax-exempt skip + idempotent tax recompute *(Broadleaf re-audit 2026-06-27, MEDIUM)*: (1) tax-exempt customer/item SKIP(SimpleTaxProvider) (2) 재pricing 시 단일 COMBINED TaxDetail UPDATE-or-CREATE-or-REMOVE(중복/stranding 없음). pricing-l0는 rate injected(정당히 out-of-scope) — exempt-skip + idempotent recompute 미커버. done-when: pricing-l0 확장 (재pricing×2→정확히 1 tax row; exempt→0 tax). **[closed 2026-06-28, feat/commerce-invariant-closure]**
- [x] P1-58 — cross-currency arithmetic fail-closed guard *(Broadleaf re-audit 2026-06-27, LOW)*: 서로 다른 통화 Money 혼합은 등록된 conversion 없이 fail-closed여야(Broadleaf Money.add throw). ax Money=bare long minor-units + currency String이라 silent cross-currency add 구조적 가능. done-when: Money currency-tag(mismatch throw) 또는 ArchUnit 룰 + ViolationProof. (exchange-RATE 변환은 feature/SKIP.) **[closed 2026-06-28, feat/commerce-invariant-closure]**
- [x] P1-59 — password-reset token-FAMILY invalidation *(Broadleaf re-audit 2026-06-27, LOW)*: reset 성공 시 그 유저 outstanding unused reset 토큰 전부 원자 무효화(Broadleaf CustomerServiceImpl.invalidateAllTokensForCustomer). ax ASVS-V2.7.3는 소비된 1개만 single-use. done-when: auth-asvs-l1 확장 (토큰 2개→token1 reset→token2 거부). beyond-ASVS-baseline. **[closed 2026-06-28, feat/commerce-invariant-closure]**
- [x] P1-60 — checkout saga rule-only + parity doc 교정 *(Broadleaf re-audit 2026-06-27, LOW doc)*: SAGA-COMPENSATE-002는 흡수·anchoring됐으나 RULE-only(runtime backend test 없음, parity[79] REVIEW-TIER 허용). checkout.md가 saga-orchestration-l0(domain_mode: full_trio)을 'review-tier spec'으로 오기술. done-when: checkout.md 교정(full_trio + verification.mechanism=rule) + 선택적 runtime saga IT. **[closed 2026-06-28, feat/commerce-invariant-closure]**

**tokenized-securities 잔여 (STO-arc 파생, 2026-07-07)**
- [x] **P1-61** — **closed 2026-07-13 (backlog-convergence-wave)**: tokenized-securities `issue()` 동시성 keystone — `concurrentIssue_exactlyOneWins_registerConserved()` (`TokenizedSecuritiesComplianceTest`, @Tag ISSUE-002) 추가: 동일 DRAFT 토큰에 ×2 동시 issue → 정확히 1×200 + 1×409 `TS_ALREADY_ISSUED`(순서 무관) + 사후 Σholdings==totalUnits(이중 seed 없음). 기존 `CountDownLatch`+`ExecutorService` keystone idiom(ThresholdTerminal/DecisionGov) 미러. 기존 `findByTokenCodeForUpdate` pessimistic lock이 이미 구조 보장 — 첫 실행 GREEN, 프로덕션 무수정(기계적 단언 추가가 done-when 그 자체). testTokenizedSecurities 34 tests 0 failures.
- [x] **P1-62** — **closed 2026-07-13 (backlog-convergence-wave, evidence-backed doc closure)**: `fromHolderId`↔인증주체 바인딩은 **이미 구현·검증 완료 상태**였음 — `SecurityTokenRegisterService.transfer():110`이 mutation 전 `holderAuthorization.controls(callerPrincipal, fromHolderId)` 호출(deny-by-default `OwnershipHolderAuthorization` SPI), 비통제 holder → 403 `TS_NOT_HOLDER_CONTROLLER`. 행위 검증: `TokenizedSecuritiesComplianceTest#transferFromUncontrolledHolder_isRejected_403_registerUnchanged`(@Tag HOLDER-AUTHZ-001, issue 후 probe라 409 shadowing 없음) + `TokenizedSecuritiesViolationProofTest:135`(빈 ownership repo에서 default-SPI deny). 실제 갭은 stale 문서뿐 — spec 헤더·CLAUDE.md 매트릭스의 "Phase 1 미구현" 표기를 "binding realized today, Phase 1 잔여 = ERC-3643 ON-CHAIN identity(fork-receiver 관심사)"로 정정. spec id `HOLDER-AUTHZ-001` 유지(STO- rename은 @Tag 참조 파괴라 기각). TDD waiver(정직 명시): 불변식이 이미 TDD-covered라 신규 테스트 없음 — doc-only closure. testTokenizedSecurities GREEN.

## P2 — verification escapes (검증 체계 자체의 갭)

- [x] P2-13 broadleaf module-exhaustion guard[80] grain-conditional disk-truth — **closed 2026-06-27 (Broadleaf re-audit, 본 세션)**: guard[80]의 disk-truth가 Maven 모듈 + core 21 패키지까지만 enumerate → ABSORBED `common`(56 sub-pkg)·`core/broadleaf-profile`(8) 한 단계 아래는 미검사 → 진짜 불변식(예: common/id hi-lo)이 silent 가능 → "ZERO silent gaps"는 grain-conditional이었음. Fix: guard를 **4-level**로 확장(common/* + profile/core/* disk-enumerate), BROADLEAF-COMPLETENESS.md에 두 테이블(56+8 분류) 추가 + headline을 grain-scope로 교정 + phantom 'core'/'admin' core-row 제거(module_count 23→21). Falsification 증명(common row 제거→BLOCK). common/id = covered_elsewhere(ax DB-native ID, #2 해소).
- [x] P2-1 evidence_guard quote-truth 미검증 — **closed 2026-06-10**: evidence_guard 헤더에
      "STRUCTURE, not TRUTH" 명시 + `evidence_quote_spotcheck_guard.sh` 신설(77th guard,
      run-all-guards [74]). 랜덤 샘플 대신 **결정적 전수** quote-vs-snapshot 대조(R25 멱등성) —
      HTML strip + entity/typography 정규화 후 substring. live는 advisory(기존 정합 backlog
      95/190건 — 대부분 live-page 검증 quote vs partial snapshot digest 불일치), fixtures는
      --strict로 non-vacuity 증명. 후속: 95건 소진 후 --strict 승격.
- [x] P2-1a — **closed 2026-07-13 (backlog-convergence-wave, B3 strict-subset; 분모 불변 sub-item)**: upstream-quote mismatch burn-down. 착수 census(ledger 기록)에서 실측 170 미매칭 = QUOTE_NOT_IN_SNAPSHOT 83 + SNAPSHOT_FILE_MISSING 87로 백로그 기재(~95)보다 2배 규모 판명. **QUOTE 83건 전량 오프라인 소진**: vercel-64는 index-only 스냅숏을 로컬 설치 skill(0.44.0, version drift 0.40.0→0.44.0 헤더 정직 갱신)에서 64룰 verbatim 전량 regen, 나머지 19건은 룰 quote를 기존 스냅숏 본문의 실제 verbatim 문장으로 재앵커(스냅숏=기록된 사실원). **[74] 승격**: run-all-guards live가 `--strict --allow-missing-snapshot` — QUOTE 불일치는 이제 HARD-FAIL, SNAPSHOT_FILE_MISSING만 advisory WARN(신규 플래그, B3 fixture 쌍이 non-vacuity 증명: mismatch+missing→exit 1 / only-missing→exit 0). **정직 재스코프**: 87건은 `practices/upstream/`에 스냅숏 본문이 커밋된 적 없어(manifest v1.1은 sha/bytes만 기록) 복원에 네트워크 fetch가 필요 — R25 오프라인 결정론을 깨지 않기 위해 full `--strict` 대신 B3로 닫고 잔여를 P2-18로 등재. 원 done-when의 "--strict exit 0"은 quote-truth 차원에서 충족(QUOTE 0건), 미커밋 스냅숏 본문 차원은 P2-18로 이관.
- [x] P2-1b — **closed 2026-06-24** *(P2-1 잔여, 분모 불변)*: external-URL evidence entries.
      `source_type: external` 항목(URL+citation만, 스냅숏 없음)은 어떤 blocking gate도 내용을
      검증하지 않아 조작된 외부 인용이 통과 가능 — 이 차원은 live fetch만 검증 가능.
      도구 신설 `practices/scripts/external_url_spot_audit.sh` (periodic ADVISORY, R25 guard 아님 —
      네트워크 비결정성): 341 고유 external URL을 3-bucket 분류(OK / SUSPICIOUS = reachable인데
      URL이 claim한 id가 페이지에 없음 = confirmed-fabricated 후보 / UNREACHABLE = bot-block·404·
      timeout, fabrication 아님). baseline sweep(verifiable subset = rfc/cwe/datatracker 42 URL,
      URL이 자기 id를 claim해 강신호): **OK 42 · UNREACHABLE 0 · SUSPICIOUS 0 = confirmed-fabricated 0**
      (done-when 충족). 비-verifiable 호스트는 reachability-only(id 없어 내용-판정 불가, 설계상 OK).
- [x] P2-2 — **closed 2026-06-24**: ESLint warn→error 승격 (`no-server-state-in-local-state`,
      `no-god-route`). 측정 게이트 충족 = 아래 frontend 부채 정리 wave 이후 6개 reference 앱
      전부 `eslint . --max-warnings 0` 0-위반(rule이 gaming 아닌 real decomposition으로
      satisfiable함을 입증). 두 곳 승격: 실효 config `frontend/eslint.config.mjs` + recommended
      preset `practices-react/eslint-plugin-ax/index.js`. `eslint --print-config`로 severity=2(error)
      라이브 검증(vacuous 아님). 이제 새 god-route / server-state-in-useState 회귀가 HARD-FAIL.
- [x] P2-3 — **closed 2026-06-24**: enforcement-surface map 문서화 + surface별 binary 테스트 커버리지.
      (1) CLAUDE.md "Enforcement surfaces" 5표면 분류표(2026-06-22). (2) surface별 binary-test-coverage
      map 추가 — 각 차단 표면이 "실제로 차단함"을 falsification 증명으로 backstop: pre-commit 주력
      게이트 evidence_guard에 신규 `ax-prove-evidence-gate-blocks-agent.sh`(agent가 placeholder evidence
      날조→evidence_guard BLOCK→실제 출처 anchor→PASS, actor=agent 기록), 기존 `agent_block_proof_guard`를
      두 증명(problemdetail+evidence) backstop으로 일반화(guard count 80 불변, fixture에 evidence stub
      쌍 추가). pre-push=recency `--fixtures`, run-all-guards=problemdetail 증명+`--include-fixtures`,
      per-domain=각 task binary+ViolationProofTest by-construction. PreToolUse는 session-bound이라
      by-construction 예외(shell 호출 불가, 트리거 게이트는 proven)로 honest 명시. run-all-guards
      132 invocations GREEN.
- [x] P2-4 — **closed 2026-06-24**: R25 체크리스트에 frontend lint 단계 추가 (이전엔 backend만).
      blocked-on 부채(아래)가 0-위반으로 정리된 뒤 `practices/verification-checklist.yaml`에
      `id: frontend-lint` step 추가 — `npm run lint`(= `eslint . --max-warnings 0`, working_dir
      `frontend`, expected_exit 0, npx 자동설치 회피). 이제 React 카탈로그 룰 회귀가 backend 회귀와
      동일하게 R25를 HARD-FAIL시킨다(pre-push recency guard 포함). fix_playbook에 canonical 수정법
      (컴포넌트 feature 이전 + barrel / route→container 추출) 명시.
- [x] P2-12 — **closed 2026-06-24**: frontend reference 앱이 자체 React ESLint 룰을 위반
      (enforcement-asymmetry의 frontend판, 2026-06-16 발견) — `eslint .` census였던 **48 problems
      (8 errors `ax/no-upward-layer-import` + 40 warnings `ax/no-god-route`)을 0으로** 종결.
      **8 errors**: feature-UI 컴포넌트 5개(studio media-thumb/media-card/reaction-button, publish
      article-editor, consumer comment-thread/favorite-toggle)를 `features/<f>/components/`로 이전 +
      barrel 생성 + importer를 barrel 경유로 rewire. flat feature 레이아웃(`features/<f>/hooks.ts`)에서
      `@/features/<B>/hooks`는 isBarrel→cross-feature 허용이라 violation-trade 없이 clean(초기 "tangle"
      우려는 룰 정독으로 반증). **40 warnings**: 6앱 전 god-route를 behaviour-preserving move로
      `features/<f>/components/<name>-screen.tsx` 컨테이너로 추출(route는 thin delegate), 앱별 executor
      병렬 처리 + 앱별 `tsc --noEmit` exit 0 self-verify. 독립 검증: `eslint . --max-warnings 0` exit 0,
      6앱 tsc 전부 clean. unused `shortId`/`Button`은 원본 route에도 있던 pre-existing dead import의
      verbatim 이동(회귀 아님, surgical 원칙상 유지). 동시 unblock된 warn→error 승격 + R25 frontend-lint
      step도 함께 종결(각 별도 항목). blind 일괄 refactor 금지 원칙 준수 — 패턴 입증(8 errors) 후
      앱별 tsc+eslint mechanical 검증으로만 진행.
- [x] P2-5 — **closed 2026-06-16**: ContextCache 401 증상 종결 확인. root-cause = 136 @SpringBootTest 중 고유-config(BillingFlowIT auto-verify=true 등) context가 ContextCache LRU(기본 32)에서 evict→stale @LocalServerPort 401. fix = 해당 2 IT에 @DirtiesContext(BEFORE_CLASS)(R22). **증상 소멸 검증**: 이번 세션 전 R25에서 aggregate `./gradlew test`는 외부 fixture PortabilityCyclic 1건만 실패(우리 코드 0). 2g heap은 별개(OOM 방지). 관측 기반 추적은 perf-log/sharding 설계 문서로 이관.
- [x] P2-6 doc_headline_count_guard가 `skills/ax-transform/SKILL.md`를 미검사 → stale counts
      147/86 노출. **closed 2026-06-10 (이번 wave: counts 187/99 수정 + guard 확장).**
- [x] P2-7 — **closed 2026-06-16**: practices-react/DECISIONS.md 신설 — 카탈로그-레벨 acceptance trail(Java DECISIONS.md 미러): 3-phase build pipeline(multi-source→4check audit→codex consensus) + 99룰 패밀리(rerender/js/rendering/server/async/bundle/client/nextjs/advanced)별 출처(Vercel RBP+MDN+CWV) + 14 ESLint + category ACCEPT/REJECT/DEFER 결정 + freshness 한계. per-rule provenance 블록과의 row/catalog 트레일 비대칭 해소.
- [x] P2-8 — **closed 2026-06-16**: `docs/VERIFICATION-PERF-AND-SHARDING.md` 신설 — perf-log 스키마(runs.jsonl에 steps[] per-step 초 + domain_task_count 추가, additive) + CI sharding 설계(78 test{Domain} 태스크를 관측비용 기준 N 샤드 분할, run-once 게이트는 shard0, R25=전 샤드 green). 이번 세션 실측(per-domain 15-16min@30도메인→78태스크/136 @SpringBootTest, contention 17-23min) + daemon-kill/OOM 교훈(--stop 금지, --no-daemon, heap pin) 반영.
- [x] P2-11 — **closed 2026-06-16 (P2 wave)**: ExportWorker.drainPending(@Scheduled 틱)이 bare this.processOne 호출로 @Transactional(REQUIRES_NEW) 프록시 우회(per-job 격리가 prod에서만 죽고 테스트는 green) → @Lazy ExportWorker self 주입 + self.processOne()로 프록시 경유. testReportExport GREEN.
- [x] P2-10 — **closed 2026-06-16 (P2 wave)**: netting addObligation에 run당 gross obligation 상한 부재(무제한 적재 시 락-hold/메모리) → MAX_OBLIGATIONS_PER_RUN=100k + countObligations 체크(run row PESSIMISTIC_WRITE 락 하에 race-safe) → 초과 시 422 NETTING_TOO_MANY_OBLIGATIONS. testNetting GREEN.
- [x] P2-9 NUMERIC(19,4) overflow seam (catalog-wide) — **closed 2026-06-10**:
      `GlobalProblemDetailAdvice.handleNumericOverflow` — DIVE/TSE/JpaSE wrapper 3종을 검사하되
      root-cause SQLState ∈ {22003(PG), 22001(H2 "Value too long")}일 때만 422
      `VALUE_OUT_OF_RANGE`(urn:problem:value-out-of-range)로 매핑, 그 외 rethrow(기존 동작 보존,
      controller-local DIVE 핸들러 우선순위 유지). E2E proof: thresholdterminal 합산 overflow →
      422 + rollback 검증. KEY: H2는 NUMERIC overflow를 22001로 보고(22003 아님); RestAssured
      JsonPath는 15+자리 수를 Double로 파싱(정밀도 손실) → BIG_DECIMAL NumberReturnType 필요.
- [x] P2-14 — **closed 2026-07-13 (backlog-convergence-wave)**: guard[87] neuter surgicality 기계 강제 — PIT-style **고정 neuter-operator 어휘(6-shape allowlist)** 구현: no-op(true/:/comment)·sentinel-string(NEUTER_*)·condition-constant(False/false)·truncation(| head -N)·over-broad-glob(*))·variable-substitution. shape 매칭과 무관하게 `exit`/`return`/`kill`·`&&`/`||` 연결 exit 등 control-flow escape 토큰 즉시 BLOCK — `exit 0` short-circuit neuter는 등재 시점 차단. 자기 증명 3쌍째 추가: `vacuous_shortcircuit_manifest.yaml`(neuter='exit 0' → REJECT → exit 1)을 run-all-guards [87] self-proof에 배선. 기존 6 manifest 항목 전부 어휘 통과·live PASS 유지. **honest residual(LIMITS 명시)**: 어휘는 토큰/구조 매칭이지 semantic 검증이 아님 — 6-shape 밖 적대적 neuter는 author-responsibility로 남음(Java PIT[84] 대비 약점 문서화). 출처: 2026-07-07 adversarial review open question.
- [x] P2-15 — **closed 2026-07-13 (backlog-convergence-wave)**: private_boundary_guard [86] 커밋 메시지 스캔 — 두 경로로 봉합: (a) **실제 `.githooks/commit-msg` 훅**(install-hooks.sh 활성화, pre-commit과 동일 opt-in) → guard 신규 `--commit-msg-file PATH` 모드가 작성 중 메시지를 layer-1 marker 스캔, 매치 시 커밋 차단; (b) **default-mode advisory backstop** — REPO_ROOT가 실제 git repo면 `git log -1 --format=%B`(HEAD 메시지) 재스캔(훅 미설치 클론도 R25 guard sweep에서 포착; plain-dir fixture는 silently skip이라 기존 7 fixture 무영향). 비공허성: `fail_commit_msg` fixture(marker 메시지→exit 1 / clean→exit 0) + fixture_kill_manifest 7번째 항목(guard[87] non-vacuous 증명 — 2-input guard 지원을 위한 backward-compatible `fixture_arg2`/`fixture2` optional 필드 추가, T5 어휘 로직 무손상·기존 6항목 byte-identical 검증). honest residual(LIMITS 명시): backstop은 HEAD 1건만 — HEAD 이전 히스토리 메시지는 스캔 밖; layer-2 시크릿 휴리스틱은 커밋 메시지 비적용(문서화된 경계). 출처: 2026-07-01 adversarial review m1.
- [x] P2-16 환경 재현성 — frontend lockfile 부재 + 미문서화 toolchain 의존성. (a) `frontend/`에 lockfile이 커밋된 적 없고(`git log --all -- frontend/package-lock.json` 빈 결과) `.gitignore`에도 없어 의도적 제외가 아닌 누락. CI(`.github/workflows/practices-sentinel.yml:80`)도 `npm ci`가 아닌 `npm install`을 사용 → 게이트가 통과한 의존성 트리와 fork-receiver가 실제 설치하는 트리가 불일치 가능. public fork-base catalog로서 fork마다 다른 트랜지티브 버전이 설치되며, 48개 하드 가드가 도는 토대 자체가 비결정적. (b) R25 실행에 `yq`·PyYAML(python `import yaml`)·JDK21이 필요하나 어디에도 명세 없음 — 신규 환경에서 PyYAML 부재 시 모든 `fail_*` fixture가 ModuleNotFoundError로 exit 1을 내어 **의도한 이유가 아닌 이유로 통과**(fixture 공허화); `pass_*` fixture 덕에 전체 FAIL로 잡혔으나 fail-fixture 단독으로는 무력. done-when: `frontend/package-lock.json` 커밋 + CI/문서 설치 명령을 `npm ci`로 전환 + `engines.node` 명시 + toolchain 의존성(`yq`/PyYAML/JDK21) 문서화 및 verify-completion.sh 선행 체크(부재 시 SKIP 아닌 명시적 BLOCK). 출처: 2026-07-08 신규 서버 부트스트랩 — `npm ci` 실패 및 R25 1차 FAIL(ModuleNotFoundError: yaml)로 발견. **closed 2026-07-13 (backlog-convergence-wave)**: (a) 두 lockfile 커밋 — `frontend/package-lock.json` + `practices-react/eslint-plugin-ax/package-lock.json`(둘 다 branch package.json 대비 `npm ci --dry-run` 정합 검증 후 커밋), `frontend/package.json`에 `engines.node >=26`, sentinel CI의 유일한 npm 설치 라인(:80, eslint-plugin-ax)을 `npm ci`로 전환. **정직 명시(A1)**: frontend lockfile은 재현 설치용 커밋일 뿐 상시 게이트가 clean `npm ci`를 수행하지 않음(R25 frontend-lint는 기존 node_modules에 lint만; frontend CI step 자체가 없음) — eslint-plugin-ax만 CI가 `npm ci` 상시 수행. (b) verify-completion.sh **toolchain preflight(BLOCK, exit 2)**: yaml parser(무조건)·JDK21(backend step 예약 시)·node/npm(frontend-lint 예약 시만 — `--step` 필터 존중, fork-receiver backend-only 실행 비차단) + 테스트 심 `AX_PREFLIGHT_FAKE_MISSING`(문서화) — 4-row mask matrix 검증(yaml→2, jdk→2, node+frontend→2, node+backend-only→통과). toolchain 전제(JDK21/PyYAML|yq/node/bash/git)는 CLAUDE.md "R25 toolchain prerequisites" + NEW-DOMAIN-CHECKLIST에 문서화.
- [ ] P2-18 upstream 스냅숏 본문 87건 미커밋 (P2-1a B3 잔여) — quote 87건(practices/ Java-side 85 + practices-react billing-vendor 2, distinct 스냅숏 id 47개)이 참조하는 `{id}.snapshot.md` 본문 파일이 커밋돼 있지 않음(manifest는 id/source/sha/bytes만 기록) → [74]가 quote truth를 오프라인 검증 불가(현재 `--allow-missing-snapshot`로 advisory WARN). done-when: 각 manifest source URL에서 본문 fetch(+sha 대조)→해당 `upstream/{id}.snapshot.md` 커밋→[74]에서 `--allow-missing-snapshot` 제거(full strict 복귀). 네트워크 작업이라 R25 밖 별도 세션에서 수행. 출처: 2026-07-13 backlog-convergence-wave T6 census.
- [ ] P2-19 guard[87] neuter 어휘 — shape 내부 semantic 오도성 + 우회 시험 fixture 승격 *(2026-07-14 cross-family 리뷰 잔여, P2급)* — 어휘가 토큰/구조 매칭이라 허용 shape **안에서** 저자가 논리를 오도하는 neuter(예: variable-substitution에서 엉뚱한 변수 선택)는 여전히 사람 리뷰 의존(LIMITS에 정직 명시). 또한 이번 세션에서 실증한 우회 시험 4종(`exit 0` / `exec true # NEUTER_*` / 따옴표 안 `$(...)` 명령치환 / 파이프라인 중간 `| head -1 foo`)은 커밋된 self-proof 2건 + 임시 매니페스트로만 검증했고 나머지 2종은 fixture 미커밋. done-when: 4종 우회 매니페스트를 커밋된 self-proof fixture로 승격 + PIT식 typed operator enum(자유형 문자열 neuter 폐지) 검토. 출처: 2026-07-14 backlog-convergence-wave 리뷰 R2/R3.
- [ ] P2-20 evidence 캐비엇 표기의 기계 강제 *(2026-07-14 cross-family 리뷰 잔여, P2급)* — internal_design 룰이 외부 인용을 "일반 원칙만 anchor하고 금지 자체는 ax-template layer decision"이라는 캐비엇과 함께 다는 패턴을 이번에 도입했으나(현재 4룰), 이 캐비엇 표기는 사람이 손으로 붙이는 관행이고 guard가 강제하지 않는다 → 캐비엇 없이 과대주장 인용을 다시 넣어도 evidence_guard는 통과. done-when: provenance_class=internal_design + source_type=external 조합에 캐비엇 문구(또는 전용 필드 `anchors: generic_principle_only`) 필수화 + 비공허성 fixture. 출처: 2026-07-14 리뷰 F2.
- [ ] P2-17 pre-push 10종 행위테스트의 커밋된 fixture 승격 — 2026-07-10 pre-push 재설계(per-ref `--expect-sha` 검증) 시 검증한 10종 행위 시나리오가 세션 내 시뮬레이션으로만 존재하고 커밋된 fixture가 없어, 후속 pre-push 변경이 해당 행위를 회귀시켜도 기계적으로 잡히지 않음. done-when: 10종 시나리오를 커밋된 pass/fail fixture 세트로 승격 + run-all-guards 또는 전용 테스트 하네스에서 실행. 출처: 2026-07-10 세션 잔여(project memory) → 2026-07-13 backlog-convergence-wave에서 등재.

## P3 — industry-niche deferrals (generic 아님 — 낮은 우선순위)

> 2026-07-07 인라인화 시 세션기록 대조로 재집계. P3-1~21 확정 요지 인라인. P3-22~40은 IDW13-17
> 후보 20건 대조: EMR G9 cross-list record-linkage(P1-33~34 recordlinkage로 닫힘 확인)만 제외,
> 불확실 6건 "(closure 여부 미검증)" 표기 포함. 기존 P3-32→P3-41, P3-33→P3-42 재번호.

- [ ] P3-1 logistics: geo-query-l0 — PostGIS/GiST 공간 인덱스 특화 질의
- [ ] P3-2 logistics: two-sided statement-reconciliation — counterparty-billed vs own 대사
- [ ] P3-3 logistics: 익명-IP rate-limit key (RATELIMIT-5 XFF spoofing 표면)
- [ ] P3-4 logistics: chain-contiguity — leg N dest == leg N+1 origin
- [ ] P3-5 logistics: geofence debounce/hysteresis (min-dwell + confirm)
- [ ] P3-6 logistics: two-party-acceptance handoff (bilateral offer/accept)
- [ ] P3-7 logistics: dual/triple-timestamp — occurred/captured/recorded 구분
- [ ] P3-8 logistics: orthogonal-exception-dimension (DSR gate shape 재사용)
- [ ] P3-9 fintech-ledger: filter/sort field-allowlist의 query-side mass-assignment 확장
- [ ] P3-10 fintech-ledger: faceted facet-count 집계
- [ ] P3-11 fintech-ledger: 파생 키 기반 멱등 statement 생성
- [ ] P3-12 HR/payroll: clamped/saturating running-balance
- [ ] P3-13 HR/payroll: deterministic recomputable-run
- [ ] P3-14 HR/payroll: read-disclosure-audit (열람 공시)
- [ ] P3-15 HR/payroll: attribute-resolved approval routing
- [ ] P3-16 insurance: G9 asserted-event-date coverage 입력
- [ ] P3-17 insurance: G11 appeal-decider-independence
- [ ] P3-18 insurance: G12 amount-tiered decision authority (전결)
- [ ] P3-19 insurance: G13 duplicate-claim/same-loss key
- [ ] P3-20 insurance: G14 statutory-deadline substantive consequence (지연이자)
- [ ] P3-21 insurance: G15 threshold-triggered regulatory filing (STR류)
- [ ] P3-22 telecom: G4 E.164 number-range governance — 번호 블록의 range 소유권 정책
- [ ] P3-23 telecom: G8 late/out-of-order additive-fact ingestion — 지연/비순서 팩트의 역산 적재
- [ ] P3-24 energy: G12 rate-asymmetric conservation — import/export 비대칭 요율 하에서도 보존 성립 (closure 여부 미검증: P1-18 netmetering 부분 커버 가능성)
- [ ] P3-25 energy: G13 period-boundary carried-net — 기간 경계에서 누적 net을 다음 기간으로 이월
- [ ] P3-26 energy: G14 reproducible computed-aggregate binding — 집계 계산이 입력에 결정론적으로 바인딩 (closure 여부 미검증: P1-7~9 reproducibility 부분 커버 가능성)
- [ ] P3-27 energy: G15 piecewise deadband obligation-vs-actual — 구간별 데드밴드 내 의무 vs 실측 비교
- [ ] P3-28 energy: G16 per-subject recurring count-budget reset — 주체별 기간 카운트 예산의 주기적 리셋 (closure 여부 미검증: P1-50 recurringinterval 부분 커버 가능성)
- [ ] P3-29 energy: G17 count-threshold eligibility-degradation FSM — 카운트 임계 도달 시 자격 강등 FSM (closure 여부 미검증: P0-25 thresholdterminal 부분 커버 가능성)
- [ ] P3-30 capital-markets: G8 withholding-tax split — 지급금에서 원천세 split-posting
- [ ] P3-31 capital-markets: G10 cash-in-lieu — 주식 대신 현금 지급(단수주 처리)
- [ ] P3-32 EMR: G8 natural-key-uniqueness-on-create — 자연키 중복 생성 거부(idempotent create)
- [ ] P3-33 EMR: G10 set-membership MECE conservation — 집합 멤버십이 MECE(상호배타·전체포함) 성립
- [ ] P3-34 EMR: G14 provisional-now/attested-later — 현재는 provisional, 이후 attestation 으로 확정
- [ ] P3-35 EMR: G16 corrected-record re-fires-ack — 정정 레코드가 ack 워크플로우 재트리거
- [ ] P3-36 EMR: G17 as-of ordered-coverage-fallback — as-of 기준 커버리지 폴백 순서 (closure 여부 미검증: P1-28~30 valuationrun as-of 부분 커버 가능성)
- [ ] P3-37 EMR: G18 two-identifier concordance — 두 식별자 체계 간 일치성 보장
- [ ] P3-38 aviation: G10 two-sided temporal exclusivity — 양방향 시간 독점성(중복 스케줄 불가)
- [ ] P3-39 aviation: G11 sign-to-content binding (attestation hash) — 서명이 내용에 바인딩 (closure 여부 미검증: P1-5 authzparity SHA-256 부분 커버 가능성)
- [ ] P3-40 aviation: G13 time/cycle-bounded conditional waiver — 시간/주기 한정 조건부 면제
- [x] P3-41 — **closed 2026-07-13 (DECISION, base-repo maintainer, reversible)**: private_boundary_guard [86] 잔여 documented gaps — (a) `/src/test/` 경로 layer-2 제외, (b) 레포 root dotfiles(`.env` 등) 스캔 경로 밖. **DECISION**: 두 gap 모두 **public base의 documented scope로 수용(ACCEPT)** — public base는 실제 시크릿을 싣지 않으며 honest-gap 주석이 `private_boundary_guard.sh:57-65`에 이미 명시됨. 더 엄격한 prod 보안 정책을 가진 fork-receiver는 자기 fork에서 (a)·(b)를 활성화해 재개봉한다. 결정은 maintainer-reversible(영구 보증 아님). 출처: 2026-07-01 adversarial review m2 → 2026-07-13 backlog-convergence-wave에서 결정 기록.
- [x] P3-42 — **closed 2026-07-13 (DECISION, base-repo maintainer, reversible)**: tokenized-securities F1/F2 read-surface authz 비대칭 심의 — `GET /tokens/{id}/holders`(인증 사용자) vs `GET /tokens/eligible-investors/{userId}`(ROLE_ADMIN). **DECISION**: 비대칭을 **base의 spec'd design intent로 유지(RETAIN)** — `READ-HOLDER-001`에 설계 의도로 명시돼 있고 holder 표시는 의도된 owner-read 표면. privacy-민감 배포 fork는 spec item 갱신 + `testTokenizedSecurities` READ-HOLDER 단언 수정으로 좁힌다. 결정은 maintainer-reversible. 출처: 2026-07-07 dogfood-closure review minor 3 → 2026-07-13 backlog-convergence-wave에서 결정 기록.
- [x] P3-43 — **closed 2026-07-10**: doc-drift `specs/crud-l0.yaml` 죽은 참조 4곳 — SKILL.md:70·CLAUDE.md:399·CLAUDE.md:437·README.md:221이 존재하지 않는 `specs/crud-l0.yaml`을 AI entry-point 표에 노출. R5가 CRUD 내용을 `specs/crud-security.yaml`로 흡수했으나 사용자-facing 표에는 미반영 (R9 Codex Critic soft #2에서 기지적됐으나 PRD 문서에만 기록됨). done-when: 4곳 전부 `crud-security.yaml`로 치환. 출처: 2026-07-10 ultracode dogfood 감사 (adversarial-confirmed). closure: 4곳 전부 `crud-security.yaml` 치환.
- [x] P3-44 — **closed 2026-07-10**: doc-drift CLAUDE.md guard 카운트 "90 guards" vs 실측 87 distinct guard scripts — run-all-guards.sh의 165 invocations 수치는 정확하나 distinct 스크립트 수가 표기와 불일치. done-when: CLAUDE.md 카운트를 실측으로 정정하거나 카운트 산출을 기계화(guard census 스크립트)해 드리프트 재발 차단. 출처: 2026-07-10 ultracode dogfood 감사 (adversarial-confirmed). closure: disk-truth 재집계로 방법론 명시 — run-all-guards=89 scripts(practices/evals 87 + practices-react/evals 2)·165 invocations, +pre-push recency 1 = 총 90 hard guards; run-all 특정 서술 89로 정정, stale "80개" 제거. hero "90 hard guards"는 총계로 정확해 유지.
- [x] P3-45 — **closed 2026-07-10**: doc-drift CLAUDE.md enforcement-surface 표의 PreToolUse hook 파일명 `.claude/settings.json` — 레포에 해당 파일 부재; 실제 git-tracked 설정은 `.claude/settings.local.json`. done-when: 표 파일명 정정. 출처: 2026-07-10 ultracode dogfood 감사 (adversarial-confirmed). closure: `.claude/settings.local.json`으로 정정 + pre-push 행 트리거를 "커밋 ship push"로 정밀화(PR #76 반영).
- [x] P3-46 — **closed 2026-07-10**: ax-prove-evidence-gate-blocks-agent.sh 잠재 버그 — `agent_events()`의 `grep -c ... || echo 0`가 zero-match 시 grep이 "0" 출력 후 exit 1 → `0\n0` 이중 출력으로 `[ -lt ]` arithmetic error (ledger에 actor=agent 항목 0건인 클론에서 결정론 재현; 스크립트는 PROVEN 출력·exit 0 유지라 thesis는 불훼손). done-when: count 산출을 `grep -c || true` + 후처리 또는 awk로 교체 + zero-entry 클론 재현 테스트. 출처: 2026-07-10 ultracode dogfood 감사 (refuter가 root cause 격리). closure: agent_events()를 grep -c 캡처 + `${n:-0}` fallback으로 교체; zero-agent-entry ledger end-to-end 재현 테스트 — arithmetic error 0, 카운트 0→2 정상, exit 0.
- [ ] P3-47 guard[76] agent_block_proof_guard zero-agent-events 전용 fixture — ledger에 `actor=agent` 이벤트가 0건인 fresh clone 상태에서 guard[76]의 non-vacuity 판정 경로를 커버하는 전용 fixture 부재(P3-46 봉합 시 zero-entry 재현 테스트는 세션 내 수행, fixture 미커밋). done-when: zero-agent-events ledger fixture 커밋 + guard[76] 경로에서 pass/fail 판정 검증. 출처: 2026-07-10 세션 잔여(project memory) → 2026-07-13 backlog-convergence-wave에서 등재.

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
