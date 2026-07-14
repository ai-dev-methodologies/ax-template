# Backlog-100 wave triage (2026-07-14)

45개 열린 항목 전수 census(6-lane 병렬, read-only) 결과와 실행 계약. 원본 근거는 census lane
산출물(세션 기록)이며, 이 문서가 wave의 canonical 실행 계획이다.

## 판정 요약

| 판정 | 건수 | 항목 |
|---|---|---|
| VERIFIED-COVERED (closure만, 코드 0) | 7 | P3-2·9·13·14·25·26·32 — 본 커밋에서 closed |
| PARTIAL (기존 표면 확장) | 12 | P3-6·7·8·11·16·24·27·28·29·36·39·40 |
| NEW (신규 spec family) | 20 | P3-3·4·5·10·12·15·17~23·30·31·33~35·37·38 |
| DEGRADED-IMPL (정직한 축소 구현) | 1 | P3-1 (PostGIS/GiST는 H2 검증 불가 → bounding-box prefilter + haversine postfilter로 spec, GiST 고유 동작 무주장) |
| P2 + guard fixture | 5 | P2-17(L)·P2-18(L, network)·P2-19(M)·P2-20(M)·P3-47(S) |

핵심 census 발견:
- **spec-only 잠복 갭 3건**: monotonic-event-ingest-l0 / signed-artifact-l0 / tamper-evident-log-l0는
  spec만 있고 backend 실현·테스트·gradle task 0. P3-7·P3-39가 각각 realize로 닫는다(신규 발명 아님).
- P3 항목의 G-번호는 세션별 번호라 IDW 번호와 무관 — 판정은 전부 불변식 내용 기준.
- IDW13~17 원본 ledger 파일 미커밋 — BACKLOG 한 줄이 canonical 잔존 기록(복원 불가, 문서화로 수용).

## 실행 lane (target-surface 기준 그룹핑 — 같은 기존 도메인을 두 lane이 못 건드리게)

| Lane | 항목 | 표면 | V-range |
|---|---|---|---|
| A obligation | P3-20(지연이자 consequence) + P3-40(waiver) + P3-21(threshold filing) | deadline-obligation 확장 ×2 + 신규 1 | V084-086 |
| B energy | P3-24(NETM-RATE) + P3-27(piecewise deadband) + P3-28(periodic count budget) + P3-29(tiered ladder) | netmetering 확장 + 신규 3 | V087-090 |
| C authority | P3-15(WF-ROUTE) + P3-18(amount-tiered) + P3-17(appeal independence) | approval-workflow 확장 + 신규 2 | V091-093 |
| D logistics | P3-1(geo degraded) + P3-4(leg contiguity) + P3-5(geofence) + P3-6(bilateral handoff) | 신규 4 | V094-097 |
| E ingest | P3-7(monotonic-event-ingest realize + captured axis) + P3-8(orthogonal-exception-gate 추출) + P3-23(additive-fact ledger) | spec realize + 추출 + 신규 | V098-100 |
| F fintech | P3-3(RATELIMIT-5 IP/XFF) + P3-10(facet-count) + P3-11(derived-key statement) + P3-12(saturating balance) | ratelimit 확장 + 신규 3 | V101-104 |
| G capmkt | P3-30(withholding split) + P3-31(cash-in-lieu) + P3-33(MECE) + P3-38(interval exclusivity) | 신규 4 | V105-108 |
| H attestation | P3-34(provisional/attested) + P3-35(correction re-fire) + P3-36(as-of fallback 항목 추가) + P3-37(ci/di concordance) + P3-39(signed-artifact realize) | valuationrun·identityverification 확장 + 신규/realize | V109-112 |
| I intake | P3-16(DATE plausibility) + P3-19(duplicate-submission key) + P3-22(range-ownership) | plausibility 확장 + 신규 2 | V113-115 |
| P2a guards | P2-19 우회 fixture 3종 승격(typed enum은 net-negative로 defer 기록) + P2-20 `anchors: generic_principle_only` 필드 강제 + P3-47 zero-agent-events fixture | practices/evals | — |
| P2b pre-push | P2-17 결정로직 lib 추출 + 10 시나리오 커밋 fixture + guard 등재 | .githooks + practices/evals | — |
| P2c snapshots | P2-18 Java-side 47 id 본문 fetch→커밋→quote burndown→[74] full strict | practices/upstream (network) | — |

## 충돌 방지 규약 (전 lane 예외 없음)

1. **공유 파일은 main 루프 전담**: `backend/build.gradle.kts`(task 등록), `practices/verification-checklist.yaml`,
   `docs/BACKLOG.md`, `practices/evals/run-all-guards.sh`(wiring), git commit 전부.
2. 실행 lane은 자기 도메인 파일만 생성/수정: `specs/`, `backend/src/main·test`의 자기 패키지, 자기 V###.
3. V-range는 위 표 고정 — 범위 밖 번호 사용 금지(Flyway 충돌 방지). 미사용 번호 gap 허용.
4. 새 도메인 필수 산출물은 `docs/NEW-DOMAIN-CHECKLIST.md` 준수(ComplianceTest + **ViolationProofTest 필수**).
5. lane 완료 후 main 루프가 task 등록→tag-scoped 실행→적대 리뷰→커밋.
