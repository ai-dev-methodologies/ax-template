# Auth Blueprint Promotion Checklist

작성일: 2026-04-02
프로젝트: ax-template
상태: CANONICAL_CHECKLIST
목적: auth blueprint를 `candidate -> curated -> stable`로 승격할 때, 대충 승격되지 못하도록 강제하는 체크리스트다.

기준 문서:
- `docs/TEMPLATE-GOVERNANCE.md`
- `docs/governance/TEMPLATE-LIFECYCLE.md`
- `docs/governance/ACTIVE-LOOP-TEMPLATE.md`

---
## A. Candidate -> Draft
- [ ] candidate inventory에 기록됨
- [ ] source type 기록됨
- [ ] why interesting 기록됨
- [ ] red flags 기록됨
- [ ] KEEP/HOLD/REJECT 판정 존재
- [ ] React/Spring/tool 후보 분리됨

실패 시:
- draft 진입 금지
- candidate inventory 단계로 되돌림

---
## B. Draft -> Curated
- [ ] reference-selection 문서 존재
- [ ] scaffold draft 존재
- [ ] architecture baseline draft 존재
- [ ] quality companion draft 존재
- [ ] manifest draft 존재
- [ ] official_doc_refs 채워짐
- [ ] approved_github_refs 채워짐
- [ ] practical refs 채워짐
- [ ] must_not 정의
- [ ] reject_if 정의
- [ ] anti-pattern 정의
- [ ] testing baseline 정의
- [ ] verify checkpoints 정의
- [ ] `chub` freshness check 통과
- [ ] reject simulation 통과
- [ ] ACTIVE-LOOP evidence 존재

실패 시:
- curated 승격 금지
- canonical draft 단계로 되돌림

---
## C. Curated -> Stable
- [ ] internal PoC evidence 존재
- [ ] 또는 실제 consumer usage 검토 존재
- [ ] build evidence 존재
- [ ] lint evidence 존재
- [ ] type evidence 존재
- [ ] test evidence 존재
- [ ] verify evidence 존재
- [ ] zero-warning 목표 설명 가능
- [ ] stable 승격 사유 기록됨
- [ ] reviewer sign-off 존재

실패 시:
- stable 승격 금지
- curated 유지

---
## D. Stable -> Stale
아래 중 하나면 stale 후보다.
- [ ] chub/latest-doc mismatch
- [ ] critical CVE 미해결
- [ ] anti-pattern drift
- [ ] repeated reject 증가
- [ ] 6개월 meaningful commit 부재
- [ ] release 이후 issue spike

실패 시:
- stable 유지 아님
- stale 결정 기록 필요

---
## E. Stale -> Refreshed
- [ ] stale 원인 문서화
- [ ] refresh diff 기록
- [ ] `chub` 재검증
- [ ] reject simulation 재실행
- [ ] 필요 시 test / verify 재실행
- [ ] refreshed / deprecated 결정 기록

실패 시:
- refreshed 승격 금지
- stale 유지

---
## Final Gate
- [ ] 현재 상태가 evidence와 일치한다
- [ ] 누락된 필수 문서가 없다
- [ ] fail-open 항목이 없다
- [ ] 다음 상태로의 승격 근거가 충분하다
