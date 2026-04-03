# ACTIVE LOOP

원칙:
- 체크되지 않은 단계는 완료 주장 금지
- 실패하면 지정된 re-entry target으로 되돌아간다
- 현재 작업은 `auth blueprint`의 curated 승격 준비 단계다

---
## Metadata
- work item: auth blueprint canonical draft
- template family: scaffold + architecture baseline + quality companion
- current state: draft
- started_at: 2026-04-02
- reviewer: assistant

---
## Stage 1. Candidate Collection
status: [x] done
re-entry target on failure: REJECT or inventory update

- [x] 후보가 inventory에 기록되었다
- [x] source type이 기록되었다
- [x] why interesting이 기록되었다
- [x] red flags가 기록되었다
- [x] discovery signal과 selection score를 분리했다

Evidence:
- `docs/plans/auth-blueprint-candidate-inventory.md`

failure notes:
- 없음

---
## Stage 2. Reference Selection
status: [x] done
re-entry target on failure: Candidate Collection

- [x] React shortlist가 따로 정리되었다
- [x] Spring shortlist가 따로 정리되었다
- [x] generator/tool shortlist가 따로 정리되었다
- [x] keep / hold / reject reason이 모두 기록되었다
- [x] pinned version 초안이 존재한다

Evidence:
- `docs/plans/auth-blueprint-reference-selection.md`
- `blueprints/pinned-versions.yaml`

failure notes:
- 없음

---
## Stage 3. Canonical Draft Creation
status: [~] in-progress
re-entry target on failure: Reference Selection

- [ ] scaffold 초안 존재
- [ ] architecture baseline 초안 존재
- [ ] quality companion 초안 존재
- [ ] manifest 초안 존재
- [x] official_doc_refs / approved_github_refs / practical refs 채움
- [x] must_not / reject_if / anti-pattern 정의
- [x] testing baseline 정의
- [x] verify checkpoints 정의

Evidence already available:
- `docs/designs/auth-blueprint.md`
- `docs/plans/auth-blueprint-implementation-plan.md`
- `docs/TEMPLATE-GOVERNANCE.md`
- `blueprints/auth-manifest.yaml`
- `contracts/auth-openapi.yaml`
- `verify/manifest.schema.json`

failure notes:
- scaffold / architecture baseline / quality companion의 실제 asset draft는 아직 남아 있다
- curated 승격 전 Stage 3의 남은 자산 정리가 필요하다

---
## Stage 4. Curated Promotion Check
status: [ ] not-started
re-entry target on failure: Canonical Draft Creation

- [ ] `chub` freshness check 통과
- [ ] build 기준 확인
- [ ] lint 기준 확인
- [ ] type 기준 확인
- [ ] test 기준 확인
- [ ] verify 기준 확인
- [ ] reject simulation 통과
- [ ] fail-open 항목 없음

Evidence target:
- `docs/plans/auth-blueprint-promotion-checklist.md`
- `.sisyphus/evidence/README.md`
- 실행 로그 / 체크 결과

failure notes:
- curated evidence 경로는 정의됐지만 실제 실행 로그는 아직 없다

---
## Stage 5. Stable Promotion Check
status: [ ] not-started
re-entry target on failure: Curated Promotion Check

- [ ] internal PoC 결과 존재
- [ ] 또는 실제 consumer usage 검토 존재
- [ ] build evidence 존재
- [ ] lint evidence 존재
- [ ] type evidence 존재
- [ ] test evidence 존재
- [ ] verify evidence 존재
- [ ] zero-warning 목표 설명 가능
- [ ] stable 승격 사유 기록

failure notes:
- curated 이전 단계라 아직 진입 불가

---
## Stage 6. Upstream Watch
status: [~] in-progress
re-entry target on failure: stale -> Refresh Review

- [x] watch 대상 목록 존재
- [x] cadence 정의됨
- [x] stale trigger 정의됨
- [ ] 실제 watch 운영 로그가 존재함

Evidence:
- `docs/governance/upstream-watchlist.yaml`

failure notes:
- watchlist는 생성됐지만 아직 운영 로그/결과는 없다

---
## Stage 7. Refresh Review
status: [ ] not-started
re-entry target on failure: stale 유지 or Stage 2/3 회귀

- [ ] stale 원인 문서화
- [ ] refresh diff 검토
- [ ] `chub` 재검증
- [ ] reject simulation 재실행
- [ ] 필요시 test / verify 재실행
- [ ] refreshed / deprecated 결정

failure notes:
- stable 이전이라 아직 해당 없음

---
## Final Gate
- [ ] 현재 상태가 evidence와 일치한다
- [ ] 누락된 필수 문서가 없다
- [ ] fail-open 항목이 없다
- [ ] 다음 상태로의 승격 근거가 충분하다

현재 판정:
- [ ] reject
- [x] draft 유지
- [ ] curated 승격
- [ ] stable 승격
- [ ] stale 강등
- [ ] refreshed 승격
- [ ] deprecated

reviewer sign-off:
- 현재 상태는 `draft`가 맞다
- foundations trio의 직접 산출물 4개는 생성 완료됐다
- 다음 즉시 작업은 scaffold / architecture baseline / quality companion draft와 curated evidence 실행이다
