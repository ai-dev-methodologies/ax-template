# Auth Blueprint Reference Selection

작성일: 2026-04-02
프로젝트: ax-template
상태: DRAFT
목적: candidate inventory를 바탕으로 React 측, Spring 측, generator/tool 측의 1차 reference implementation을 선택하고 그 이유와 reject 근거를 기록한다.

기준 문서:
- `docs/TEMPLATE-GOVERNANCE.md`
- `docs/plans/auth-blueprint-candidate-inventory.md`
- `docs/governance/TEMPLATE-LIFECYCLE.md`

---
## 1. 선택 원칙
우리는 가장 유명한 후보를 고르지 않는다.
우리는 아래를 만족하는 후보를 고른다.
- official truth와 충돌이 적다
- boring default다
- verify/test 연결이 쉽다
- SI delivery에 맞는다
- internal canonical template로 정규화하기 쉽다

---
## 2. React-side 1차 reference
### 구조 / auth state / UX reference
- **Primary:** `full-stack-fastapi-template`
- **Secondary:** `ixartz/SaaS-Boilerplate`

### 선택 이유
- `full-stack-fastapi-template`은 decoupled SPA + generated TS client discipline이 강하다
- `ixartz/SaaS-Boilerplate`는 auth UX, RBAC, testing 관점 참고가치가 높다

### reject / hold 근거
- `ixartz/SaaS-Boilerplate`는 Next.js App Router / managed auth 의존성이 커서 generic React baseline 그대로 쓰기엔 과하다
- `BearStudio/start-ui-web`은 TanStack Start / oRPC / Better Auth 조합이 V1 boring default 기준에서 너무 공격적이다
- `wasp-lang/open-saas`는 proprietary DSL / lock-in 때문에 reject한다

---
## 3. Frontend generator/tool 1차 reference
### Typed client generation
- **Primary:** `orval`
- **Secondary:** `kubb`

### 선택 이유
- `orval`은 OpenAPI source-of-truth 구조와 직접 맞물린다
- `kubb`는 비교군으로는 유의미하지만 운영 단순성은 추가 검토 필요

### reject / hold 근거
- generator/tool은 repo 템플릿과 같은 방식으로 채택하지 않는다
- React reference와 별도 축으로 평가한다

---
## 4. Spring-side 1차 reference
### Protocol / built-in truth
- **Primary:** Spring Authorization Server samples

### Enterprise baseline / generated output
- **Primary:** JHipster generated output

### Built-in implementation reference
- **Primary:** Baeldung Spring Security examples

### hold/reject
- `eazybytes/spring-security`는 학습용 progression 참고 가치만 있다
- custom filter와 교육용 코드 비율이 높아 canonical default로는 부적합하다
- low-traction wrapper repos는 boring default 위반 가능성이 커서 reject한다

---
## 5. 조합 전략
최종 internal canonical draft는 아래 조합으로 만든다.
- React 구조 reference: `full-stack-fastapi-template`
- React UX/testing 보완: `ixartz/SaaS-Boilerplate`
- React typed client generation: `orval`
- Spring official auth truth: Spring Authorization Server samples
- Spring enterprise baseline: JHipster generated output
- Spring 세부 built-in reference: Baeldung Spring Security examples

즉 single repo 하나를 복사하지 않고, **조합된 internal canonical draft**를 만든다.

---
## 6. 다음 체크포인트
이 문서가 완료로 간주되려면 아래가 추가로 채워져야 한다.
- pinned version 초안
- official_doc_refs
- approved_github_refs
- why selected / why rejected 근거의 line-item 보강
- promotion checklist 진입 준비
