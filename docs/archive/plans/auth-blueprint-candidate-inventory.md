# Auth Blueprint Candidate Inventory

작성일: 2026-04-02
프로젝트: ax-template
상태: DRAFT
목적: React + Spring Boot 인증/보안 블루프린트의 후보 레퍼런스를 discovery 단계에서 구조적으로 수집하고, shortlist / reject / hold 판단을 위한 비교표를 만든다.

기준 문서:
- `docs/TEMPLATE-GOVERNANCE.md`
- `docs/designs/auth-blueprint.md`
- `docs/plans/auth-blueprint-implementation-plan.md`

---
## 1. 이 문서의 역할
이 문서는 외부 repo를 그대로 채택하기 위한 문서가 아니다.
이 문서는 **후보를 충분히 모으고, 어떤 후보를 남기고 어떤 후보를 버릴지 근거를 남기는 discovery 문서**다.

핵심 원칙:
- stars는 discovery signal이지 adoption reason이 아니다
- React 후보와 Spring 후보는 분리해서 평가한다
- generator/tool 후보도 별도 축으로 평가한다
- 최종 자동화 기준은 external repo가 아니라 internal canonical template다

---
## 2. 평가 기준
### Discovery signal
- stars
- 최근 maintenance
- release cadence
- 실제 커뮤니티 출현 빈도
- 내부 corpus에서 반복 등장 여부

### Selection score
1. official truth alignment
2. boring default fit
3. security posture
4. contract/OpenAPI fit
5. testing/verify fit
6. reject-rule fit
7. SI delivery fit
8. maintenance heartbeat

평가 상태:
- `KEEP` : shortlist 유지
- `HOLD` : 보류, 더 검토 필요
- `REJECT` : canonical reference 후보에서 제외

---
## 3. React-side candidates
| Candidate | Type | Why interesting | Strengths | Red flags | Status |
|---|---|---|---|---|---|
| `full-stack-fastapi-template` | practical reference | decoupled SPA + generated TS client 구조 참고 | OpenAPI 기반 generated client discipline, auth state 흐름 참고 가치 높음 | backend가 FastAPI라서 전체 blueprint reference로는 부적합 | KEEP |
| `ixartz/SaaS-Boilerplate` | practical reference | auth UX, RBAC, testing, SaaS 구조 참고 | 테스트/품질 기준, auth boundary, role 기반 화면 패턴 참고 가능 | Next.js App Router와 managed auth 의존성이 강해 generic React 기준으론 과함 | HOLD |
| `orval` | generator/tool | OpenAPI source-of-truth에서 프론트 타입/클라이언트 생성 | React 측 canonical 흐름의 핵심 후보, typed client 생성에 적합 | repo 자체 템플릿은 아님, 생성 산출물 운영 방식 검토 필요 | KEEP |
| `kubb` | generator/tool | OpenAPI 기반 코드 생성 대안 | 생성 계층 선택지 제공, tooling 비교군으로 적절 | ecosystem fit / 운영 단순성은 추가 확인 필요 | HOLD |
| `BearStudio/start-ui-web` | practical reference | strict type-safe frontend/auth 흐름 참고 | Better Auth, 테스트 discipline, modern tooling 참고 가치 | TanStack Start / oRPC 조합이 ax-template V1 기준으론 너무 새롭고 덜 boring함 | REJECT |
| `wasp-lang/open-saas` | practical reference | 높은 DX와 full-stack auth abstraction 참고 | auth abstraction의 이상형을 참고할 수 있음 | proprietary DSL / vendor lock-in / black box auth 구조 | REJECT |

### React-side interim view
React 쪽은 단일 repo 하나보다 아래 조합이 유력하다.
- auth state / UX 구조: `full-stack-fastapi-template`, `ixartz/SaaS-Boilerplate` 참고
- typed client generation: `orval` 중심 검토, `kubb` 비교군 유지

즉 React의 reference는 **repo 1개 선정**보다 **구조 reference + generator tool 선정** 조합으로 갈 가능성이 높다.

---
## 4. Spring Boot-side candidates
| Candidate | Type | Why interesting | Strengths | Red flags | Status |
|---|---|---|---|---|---|
| Spring Authorization Server samples | official reference | 공식성 최상위, Spring modern auth 기준 | built-in 흐름, OAuth2/OIDC, refresh token 구조, 테스트 discipline | app 자체가 auth server 역할까지 가면 V1 scope 과대 가능 | KEEP |
| JHipster generated output | generated enterprise reference | enterprise delivery 감각, schema/testing/auditing 참고 | robust test conventions, security config, real-world enterprise defaults | generator noise 많고 그대로 쓰면 과함 | KEEP |
| Baeldung Spring Security examples | practical reference | built-in 중심 세부 구현 참고 | Nimbus JWT, Spring native patterns, 단일 문제별 reference 좋음 | unified blueprint가 아니라 파편화된 예제 | KEEP |
| `eazybytes/spring-security` | educational/community reference | Spring Security 6 구간별 구현 흐름 참고 | progression이 명확하고 provider integration 예제 있음 | custom filter/교육용 코드 비율 높아 canonical default로는 위험 | HOLD |
| low-traction wrapper repos | niche/community reference | JPA-backed convenience wrapper 등 참고 가능 | 일부 기능 공백 메우는 아이디어 제공 | low traction, custom abstraction, boring default 위반 가능성 큼 | REJECT |

### Spring-side interim view
Spring 쪽은 아래 축이 유력하다.
- protocol / built-in truth: Spring official samples
- enterprise 운영 기본값: JHipster generated output
- 세부 구현 reference: Baeldung examples

즉 Spring의 reference는 **공식 reference + generated enterprise output + 세부 built-in example** 조합이 유력하다.

---
## 5. Tooling / generation candidates
| Candidate | Type | Role | Why it matters | Status |
|---|---|---|---|---|
| `orval` | generator | OpenAPI -> React client/hooks | 현재 구조와 가장 직접적으로 맞물림 | KEEP |
| `kubb` | generator | OpenAPI -> typed codegen alternative | 비교군으로 가치 있음 | HOLD |
| Spring OpenAPI ecosystem tools | contract tooling | backend contract export / validation | backend와 frontend 계약 일치에 필수 | KEEP |

---
## 6. Shortlist recommendation (current)
### React shortlist
- `full-stack-fastapi-template` (structure reference)
- `ixartz/SaaS-Boilerplate` (UX/testing reference)
- `orval` (primary generator candidate)
- `kubb` (secondary generator candidate)

### Spring shortlist
- Spring Authorization Server samples (official truth)
- JHipster generated output (enterprise baseline)
- Baeldung Spring Security examples (implementation reference)
- `eazybytes/spring-security` (hold, educational reference only)

---
## 7. Biggest mistakes to avoid
1. stars 많은 repo를 그대로 canonical로 착각하는 것
2. React 후보와 Spring 후보를 한 표에서 섞어버리는 것
3. generator/tool과 repo reference를 같은 종류로 평가하는 것
4. official truth보다 community convenience를 우선하는 것
5. stable 승격 전에 automation source로 사용해버리는 것

---
## 8. Next actions
1. shortlist 후보 각각에 대해 source provenance 채우기
2. 각 후보의 boring default / security / testing / contract fit 점수 부여
3. React reference 조합과 Spring reference 조합 각각 1차 결정
4. internal canonical draft 설계
5. 이후 `candidate -> curated -> stable` 승격 절차 진행
