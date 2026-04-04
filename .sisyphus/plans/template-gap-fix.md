# Template Gap Fix — GAP-REPORT 기반 spec/contract/manifest 보강

## TL;DR

> GAP-REPORT에서 발견한 7개 갭을 ax-template의 spec/contract/manifest에 반영한다. 코드 변경 없음 — 문서/스펙 파일만 업데이트.
>
> **Estimated Effort**: Quick (1-2h)

---

## TODOs

- [ ] 1. Manifest 보강 — JWT, 세션, CORS, 테스트 시딩 정책 추가
- [ ] 2. ASVS spec 보강 — rate limit 값 직접 내장, 검증 방식 명확화
- [ ] 3. OpenAPI contract 보강 — 공통 에러 스키마 추가
- [ ] 4. Portable test template 제공 — RestAssured 기반 예시

---

## Success Criteria
- [ ] manifest에 JWT/세션/CORS/테스트시딩 정책 존재
- [ ] ASVS spec V2.2.1 notes에 "5 per 15 min" 직접 명시
- [ ] OpenAPI에 공통 ErrorResponse 스키마 존재
- [ ] 검증 프로젝트에서 다시 testAsvs 통과 (기존 테스트 깨지지 않음)
