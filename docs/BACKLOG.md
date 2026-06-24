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
| P1 (generic signature backlog) | 54 | 54 | **100%** |
| P2 (verification escapes) | 12 | 12 | **100%** |
| P3 (industry-niche deferrals) | 31 | 0 | 0% |
| **P0–P3 합계 (수렴 분모)** | **123** | **92** | **~75%** |
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

## P2 — verification escapes (검증 체계 자체의 갭)

- [x] P2-1 evidence_guard quote-truth 미검증 — **closed 2026-06-10**: evidence_guard 헤더에
      "STRUCTURE, not TRUTH" 명시 + `evidence_quote_spotcheck_guard.sh` 신설(77th guard,
      run-all-guards [74]). 랜덤 샘플 대신 **결정적 전수** quote-vs-snapshot 대조(R25 멱등성) —
      HTML strip + entity/typography 정규화 후 substring. live는 advisory(기존 정합 backlog
      95/190건 — 대부분 live-page 검증 quote vs partial snapshot digest 불일치), fixtures는
      --strict로 non-vacuity 증명. 후속: 95건 소진 후 --strict 승격.
- [ ] P2-1a upstream-quote mismatch burn-down *(P2-1 잔여, 분모 불변)* —
      `evidence_quote_spotcheck_guard.sh` [74]는 현재 ADVISORY (~95 미매칭,
      대부분 `practices-react/upstream/vercel-react-best-practices.snapshot.md`
      index-only digest vs 실제 인용문 불일치). done-when:
      `bash practices/evals/evidence_quote_spotcheck_guard.sh --strict` exits 0
      **AND** guard [74]가 `run-all-guards.sh`에서 `--strict` 모드로 승격.
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
