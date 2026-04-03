# ACTIVE LOOP TEMPLATE

이 파일은 실제 template 작업을 시작할 때 `ACTIVE-LOOP.md`로 복사해서 사용한다.

원칙:
- 각 단계 진입 전 `status`와 checklist를 업데이트한다
- 체크되지 않은 단계는 완료 주장 금지
- 실패하면 `re-entry target`으로 되돌아간다

---
## Metadata
- work item:
- template family:
- current state: candidate / draft / curated / stable / stale / refreshed
- started_at:
- reviewer:

---
## Stage 1. Candidate Collection
status: [ ] not-started [ ] in-progress [ ] done [ ] failed
re-entry target on failure: REJECT or inventory update

- [ ] 후보가 inventory에 기록되었다
- [ ] source type이 기록되었다
- [ ] why interesting이 기록되었다
- [ ] red flags가 기록되었다
- [ ] discovery signal과 selection score를 분리했다

failure notes:

---
## Stage 2. Reference Selection
status: [ ] not-started [ ] in-progress [ ] done [ ] failed
re-entry target on failure: Candidate Collection

- [ ] React shortlist가 따로 정리되었다
- [ ] Spring shortlist가 따로 정리되었다
- [ ] generator/tool shortlist가 따로 정리되었다
- [ ] keep / hold / reject reason이 모두 기록되었다
- [ ] pinned version 초안이 존재한다

failure notes:

---
## Stage 3. Canonical Draft Creation
status: [ ] not-started [ ] in-progress [ ] done [ ] failed
re-entry target on failure: Reference Selection

- [ ] scaffold 초안 존재
- [ ] architecture baseline 초안 존재
- [ ] quality companion 초안 존재
- [ ] manifest 초안 존재
- [ ] official_doc_refs / approved_github_refs / practical refs 채움
- [ ] must_not / reject_if / anti-pattern 정의
- [ ] testing baseline 정의
- [ ] verify checkpoints 정의

failure notes:

---
## Stage 4. Curated Promotion Check
status: [ ] not-started [ ] in-progress [ ] done [ ] failed
re-entry target on failure: Canonical Draft Creation

- [ ] `chub` freshness check 통과
- [ ] build 기준 확인
- [ ] lint 기준 확인
- [ ] type 기준 확인
- [ ] test 기준 확인
- [ ] verify 기준 확인
- [ ] reject simulation 통과
- [ ] fail-open 항목 없음

commands / evidence:

failure notes:

---
## Stage 5. Stable Promotion Check
status: [ ] not-started [ ] in-progress [ ] done [ ] failed
re-entry target on failure: Curated Promotion Check

- [ ] internal PoC 결과 존재
- [ ] 또는 실제 consumer usage 검토 존재
- [ ] 반복 사용 가능성 평가 기록
- [ ] stable 승격 사유 기록

links to evidence:

failure notes:

---
## Stage 6. Upstream Watch
status: [ ] not-started [ ] in-progress [ ] done [ ] failed
re-entry target on failure: stale -> Refresh Review

- [ ] semantic release 확인
- [ ] structural file 변경 확인
- [ ] CVE / advisory 확인
- [ ] issue spike 확인
- [ ] meaningful commit cadence 확인
- [ ] stale trigger 여부 판단

watch notes:

---
## Stage 7. Refresh Review
status: [ ] not-started [ ] in-progress [ ] done [ ] failed
re-entry target on failure: stale 유지 or Stage 2/3 회귀

- [ ] stale 원인 문서화
- [ ] refresh diff 검토
- [ ] `chub` 재검증
- [ ] reject simulation 재실행
- [ ] 필요시 test / verify 재실행
- [ ] refreshed / deprecated 결정

refresh notes:

---
## Final Gate
- [ ] 현재 상태가 evidence와 일치한다
- [ ] 다음 상태로 승격할 근거가 있다
- [ ] unresolved 항목이 없다
- [ ] ACTIVE-LOOP를 닫아도 되는지 확인했다

final decision:
- [ ] reject
- [ ] candidate 유지
- [ ] curated 승격
- [ ] stable 승격
- [ ] stale 강등
- [ ] refreshed 승격
- [ ] deprecated

reviewer sign-off:
