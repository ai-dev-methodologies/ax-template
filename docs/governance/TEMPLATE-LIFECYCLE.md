# TEMPLATE LIFECYCLE

작성일: 2026-04-02
프로젝트: ax-template
상태: CANONICAL
목적: 템플릿 후보 수집부터 curated/stable 승격, stale 강등, refresh 재판정까지의 **강제 워크플로우**를 정의한다.

관련 문서:
- `docs/TEMPLATE-GOVERNANCE.md`
- `docs/plans/auth-blueprint-candidate-inventory.md`
- `docs/designs/auth-blueprint.md`
- `docs/plans/auth-blueprint-implementation-plan.md`
- `docs/governance/ACTIVE-LOOP-TEMPLATE.md`

---
## 1. 이 문서의 역할
이 문서는 “어떻게 하면 AI가 대충하다 멈추지 못하게 할 것인가”에 대한 운영 문서다.

핵심 원칙:
> 체크리스트를 통과하지 못하면 다음 단계로 못 간다.

즉 이 문서는 단순 설명서가 아니라 **승격 게이트 문서**다.

---
## 2. 상태 머신
```text
candidate -> draft -> curated -> stable -> stale -> refreshed
```

### candidate
- 후보는 모였지만 아직 선택/정규화 전
- 필수 산출물: candidate inventory entry

### draft
- reference selection을 마치고 internal canonical draft를 만드는 단계
- 필수 산출물: reference selection 기록, draft manifest

### curated
- 구조, 기본값, 테스트 baseline, reject rule, verify rule까지 정리됨
- 필수 산출물: scaffold + architecture baseline + quality companion + manifest

### stable
- 최소 1회 내부 PoC 또는 실제 consumer usage 검토 통과
- 필수 산출물: evidence of real use, stable 승격 기록

### stale
- 최신 공식 문서 mismatch, repeated reject, anti-pattern drift, critical trigger 발생
- 필수 산출물: stale reason, stale timestamp, re-entry target

### refreshed
- refresh workflow를 수행하고 재검증을 통과한 상태
- 필수 산출물: refresh diff, re-validation evidence, status decision

---
## 3. 전체 루프
```text
Candidate Collection
  ↓
Reference Selection
  ↓
Canonical Draft Creation
  ↓
Curated Promotion Check
  ↓
Real Use / PoC Check
  ↓
Stable Promotion
  ↓
Upstream Watch / Drift Detection
  ↓
If drift: stale
  ↓
Refresh Review
  ↓
refreshed or remain stale
```

---
## 4. 단계별 정의
## Stage 1. CANDIDATE COLLECTION
### 입력
- 공식 문서
- approved GitHub repo
- practical reference
- internal prior assets
- engineering blog / article corpus

### 필수 체크리스트
- [ ] 후보가 inventory에 기록되었다
- [ ] source type이 기록되었다
- [ ] why interesting이 기록되었다
- [ ] red flags가 기록되었다
- [ ] discovery signal과 selection score를 분리했다

### 실패 조건
- 공식 문서가 없다
- 유지보수 흔적이 없다
- license / 운영 적합성 문제가 명백하다
- why interesting 없이 stars만 근거로 올렸다

### 실패 시 되돌아갈 곳
- 후보는 즉시 `REJECT`
- inventory에는 reject reason을 남긴다

### 완료 기준
- 후보가 `KEEP`, `HOLD`, `REJECT` 중 하나로 inventory에 남아 있다

---
## Stage 2. REFERENCE SELECTION
### 입력
- candidate inventory

### 필수 체크리스트
- [ ] React shortlist가 따로 정리되었다
- [ ] Spring shortlist가 따로 정리되었다
- [ ] generator/tool 후보가 따로 정리되었다
- [ ] 왜 이 후보를 남겼는지 기록했다
- [ ] 왜 다른 후보를 버렸는지 기록했다
- [ ] pinned version 초안이 존재한다

### 실패 조건
- repo 하나를 그대로 canonical로 선언했다
- React와 Spring 후보를 섞어서 비교했다
- tool 후보와 repo 후보를 같은 축으로 판단했다
- reject reason이 없다

### 실패 시 되돌아갈 곳
- `Stage 1. CANDIDATE COLLECTION`

### 완료 기준
- reference-selection 문서가 존재한다
- draft 만들 입력 조합이 확정되었다

---
## Stage 3. CANONICAL DRAFT CREATION
### 입력
- reference selection

### 필수 체크리스트
- [ ] scaffold 초안이 존재한다
- [ ] architecture baseline 초안이 존재한다
- [ ] quality companion 초안이 존재한다
- [ ] manifest 초안이 존재한다
- [ ] official_doc_refs / approved_github_refs / practical refs가 채워졌다
- [ ] must_not / reject_if / anti-pattern이 정의되었다
- [ ] testing baseline이 정의되었다
- [ ] verify checkpoints가 정의되었다

### 실패 조건
- 규칙은 있는데 manifest가 없다
- 코드 뼈대는 있는데 reject rule이 없다
- source provenance 없이 canonical draft를 만들었다
- testing baseline 없이 curated로 올리려 한다

### 실패 시 되돌아갈 곳
- `Stage 2. REFERENCE SELECTION`

### 완료 기준
- draft가 `candidate`에서 `draft` 상태로 넘어갈 수 있다

---
## Stage 4. CURATED PROMOTION CHECK
### 입력
- canonical draft

### 필수 체크리스트
- [ ] `chub` freshness check 통과
- [ ] build / lint / type / test / verify 기준 정의 또는 실행 근거 존재
- [ ] reject simulation 통과
- [ ] zero-warning 목표를 충족하거나 명시적 예외가 있다
- [ ] config/template assets shape가 complete하다
- [ ] ACTIVE-LOOP ledger가 전부 체크됐다

### 실패 조건
- `chub` unavailable / timeout / mismatch인데도 진행
- reject simulation 없이 curated 선언
- verify 기준이 없거나 fail-open
- ACTIVE-LOOP 누락

### 실패 시 되돌아갈 곳
- `Stage 3. CANONICAL DRAFT CREATION`

### 완료 기준
- 상태를 `curated`로 올릴 수 있다

---
## Stage 5. STABLE PROMOTION CHECK
### 입력
- curated template

### 필수 체크리스트
- [ ] 최소 1회 internal PoC 결과 기록
- [ ] 또는 실제 consumer usage 검토 기록
- [ ] 반복 사용 가능성 평가 기록
- [ ] 운영/유지보수 관점 피드백 반영
- [ ] stable 승격 사유 기록

### 실패 조건
- 실사용/PoC 없이 stable 선언
- 문서 검토만으로 stable 선언
- drift나 reject가 이미 있는데 stable 유지

### 실패 시 되돌아갈 곳
- `Stage 4. CURATED PROMOTION CHECK`

### 완료 기준
- stable evidence가 존재한다
- stable timestamp와 reviewer가 기록된다

---
## Stage 6. UPSTREAM WATCH
### 입력
- stable template
- tracked upstream sources

### 필수 체크리스트
- [ ] semantic release tag 추적
- [ ] structural file 변경 추적
- [ ] CVE / advisory 추적
- [ ] issue spike 추적
- [ ] 6개월 meaningful commit 부재 여부 추적
- [ ] review cadence에 맞춰 확인했다

### 실패 조건
- release만 보고 docs mismatch를 안 본다
- structural change를 놓친다
- stale trigger가 발생했는데 stable 유지

### 실패 시 되돌아갈 곳
- 상태를 `stale`로 강등
- `Stage 7. REFRESH REVIEW`로 이동

### 완료 기준
- watch result와 status decision이 남아 있다

---
## Stage 7. REFRESH REVIEW
### 입력
- stale template

### 필수 체크리스트
- [ ] stale 원인 문서화
- [ ] refresh diff 검토
- [ ] `chub` freshness 재검증
- [ ] reject simulation 재실행
- [ ] 필요한 경우 test / verify 재실행
- [ ] 유지 / refreshed / deprecated 중 하나 결정

### 실패 조건
- stale 이유를 기록하지 않음
- refresh했지만 re-validation 없음
- refreshed 선언했지만 evidence 없음

### 실패 시 되돌아갈 곳
- `stale` 유지
- 필요한 경우 `Stage 2` 또는 `Stage 3`로 회귀

### 완료 기준
- 상태가 `refreshed` 또는 `deprecated`로 명확히 결정된다

---
## 5. Loop 강제 규칙
### Rule 1. Active Ledger 없으면 workflow 무효
모든 template 작업은 `ACTIVE-LOOP.md`를 복사해 root 또는 작업 디렉토리에 두고 진행한다.
이 파일이 없으면 작업은 valid하지 않다.

### Rule 2. 체크리스트를 통과하지 못하면 다음 단계 금지
각 stage는 필수 체크리스트가 모두 채워지기 전에는 다음 단계로 넘어갈 수 없다.

### Rule 3. fail-open 금지
`chub` unavailable / timeout / mismatch, reject simulation 미실행, verify 누락은 모두 **진행 중단** 사유다.

### Rule 4. status는 evidence로만 바뀐다
- curated는 curated evidence로만
- stable은 PoC/실사용 evidence로만
- refreshed는 refresh evidence로만
바꿀 수 있다.

### Rule 5. external repo는 언제나 후보일 뿐
external repo를 그대로 canonical automation source로 쓰는 행동은 금지한다.

---
## 6. 최소 필수 문서 세트
이 루프를 제대로 닫으려면 최소 아래 문서가 필요하다.
- `docs/TEMPLATE-GOVERNANCE.md`
- `docs/governance/TEMPLATE-LIFECYCLE.md`
- `docs/governance/ACTIVE-LOOP-TEMPLATE.md`
- `docs/plans/*-candidate-inventory.md`
- `docs/plans/*-reference-selection.md`
- `docs/plans/*-promotion-checklist.md`
- `docs/governance/upstream-watchlist.yaml`

현재 auth blueprint 기준 canonical 파일:
- `docs/plans/auth-blueprint-candidate-inventory.md`
- `docs/plans/auth-blueprint-reference-selection.md`
- `docs/plans/auth-blueprint-promotion-checklist.md`
- `docs/governance/upstream-watchlist.yaml`

---
## 7. Immediate Next Actions for ax-template
1. `auth-blueprint-reference-selection.md` 작성
2. `auth-blueprint-promotion-checklist.md` 작성
3. `upstream-watchlist.yaml` 작성
4. 실제 template 작업 시 `ACTIVE-LOOP-TEMPLATE.md`를 `ACTIVE-LOOP.md`로 복사하여 사용
