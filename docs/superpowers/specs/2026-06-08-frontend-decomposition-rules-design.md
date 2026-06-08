# ax-template — 프론트엔드 분해 · 패키지 구조 강제 규칙 (설계 스펙)

- 날짜: 2026-06-08
- 상태: **DRAFT (설계 제안 — 검토/승인 대기).** 백엔드 DDD 분해 스펙(`2026-06-08-ddd-decomposition-rules-design.md`)의 프론트엔드 sibling. 승인 시 다음 단계 = 구현 플랜(writing-plans).
- 범위: 프론트엔드 React / Next.js App Router (`frontend/`). 백엔드 분해는 별도 스펙(이미 구현·검증 완료).
- 전제: 백엔드 스펙이 ArchUnit 하드 가드로 분해를 강제했듯, 프론트엔드는 **`eslint-plugin-ax`(8 룰)** 가 그 역할을 한다. 본 스펙은 그 플러그인을 프론트엔드의 "ArchUnit"으로 삼아 분해 규칙을 확장한다.

---

## 1. 문제 (갭)

`frontend/`는 이미 일부 구조 위생을 강제한다: `no-inline-component-definition`(인라인 컴포넌트 정의 금지)·`no-broad-barrel-imports`(광역 배럴 import 금지)·`no-app-local-ui-primitives`(UI 프리미티브는 `components/ui`에). 그러나 **모듈 분해 자체**는 강제되지 않는다. 현재 구조는 `app/`(App Router 라우팅) · `features/<feature>/<slice>/index.ts`(기능 슬라이스 + 배럴) · `components/{ui,auth,brand,showcase}`.

다음은 **규칙으로 강제되지 않는다** (= AI/포크 수신자가 일관성 없이 짤 수 있는 갭):

1. **기능 슬라이스 격리** — 한 `features/A`가 `features/B`의 **내부 파일**을 deep-import해도 막는 규칙이 없다. (백엔드 HG-FEAT-ISOLATION의 프론트엔드 대응이 부재.)
2. **import 방향(레이어)** — `components/ui`가 `features/`를 import하거나, `features/`가 `app/`을 import하는 역방향 결합을 막는 규칙이 없다. (백엔드 layering의 대응 부재.)
3. **published surface** — 기능의 공개 표면이 `index.ts` 배럴로 정의돼야 하는데, 외부에서 슬라이스 내부 경로를 직접 import하는 것을 막지 않는다. (백엔드 `@PublishedApi` default-deny의 대응 부재.)
4. **라우트 파일 두께** — `app/**/page.tsx`가 데이터 패칭 + 상태 + 비즈니스 로직을 직접 들고 있는 god-route를 막는 규칙이 없다. (백엔드 thin-controller의 대응 부재.)
5. **상태 경계** — 서버 상태(SWR/TanStack)를 클라이언트 스토어로 복제하거나, URL 상태로 둬야 할 것을 로컬 state에 두는 것을 막지 않는다.
6. **컴포넌트 분해 기준** — god-component(데이터 패칭 + 표현 + 분기 로직을 한 파일에) 적정성이 정의돼 있지 않다.

## 2. 방법론 (제안)

**Feature-Sliced Design (FSD) lite — Next.js App Router 적응.**

- **슬라이스 = 기능 경계**: `features/<feature>`. 한 기능은 자기 슬라이스 안에서 응집한다. 슬라이스 간 참조는 **공개 배럴(`index.ts`)만**, 내부 경로 deep-import 금지.
- **레이어 방향(단방향)**: `app/`(routing, thin) → `features/`(domain UI + hooks + data) → `components/ui` + `lib/`(shared kernel). 역방향 금지: `components/ui`는 `features/`/`app/`을 모르고, `features/`는 `app/`을 모른다.
- **published surface = 배럴**: 각 슬라이스의 `index.ts`가 공개 API. 외부는 `@/features/<f>`(배럴)만 import. `@/features/<f>/internal/...` deep-import 금지(자기 슬라이스 내부는 허용).
- **컨테이너/프레젠테이션 분리**: 데이터 패칭·side-effect는 컨테이너(또는 hook), 표현은 순수 프레젠테이션 컴포넌트. (백엔드 service↔controller 분리의 대응.)
- **라우트 thin**: `app/**/page.tsx`는 슬라이스 컴포넌트로 위임(thin). 데이터 패칭/비즈니스 로직 직접 보유 금지.

**접근법: A(eslint-plugin-ax 하드 룰) + 선택적 B(경로 컨벤션).** 새 프레임워크(예: Nx 모듈 경계) 미도입 — 카탈로그의 "프레임워크 강요 금지" 철학 + 기존 8 ESLint 룰과 일관. 기존 슬라이스를 깨지 않음 — 깨는 건 명시적 부채로 allowlist + 점진 마이그레이션(백엔드와 동일).

## 3. 컨벤션 마커 (선택적 B)

순수 디렉터리 위치로 대부분 판정 가능(백엔드의 마커보다 약함). 추가로 명시가 필요한 곳:

- **published surface** = 슬라이스 루트 `index.ts`의 named export (배럴). deep-import 판정의 기준.
- **`"use client"` 경계** = 서버/클라이언트 컴포넌트 구분(App Router). 데이터 패칭 컨테이너 vs 클라이언트 인터랙션 분리의 신호.
- (선택) `// @published-api` 주석 또는 `index.ts` re-export = 외부 노출 의도 표식.

근거: Feature-Sliced Design(공개 API = 슬라이스 배럴, 레이어 단방향 의존), Next.js App Router(서버/클라이언트 컴포넌트 경계, route group), React 공식(컴포지션·container/presentational), 사용자 web 규칙(`web/patterns.md` 상태 분리, `web/coding-style.md` feature 단위 조직).

## 4. 강제 티어 (eslint-plugin-ax = 프론트엔드 ArchUnit)

> 백엔드 TIER-0(즉시 block)/TIER-1(마커 의존)/TIER-2(인간 판단)의 프론트엔드 대응. ESLint `error` = block(CI/`eslint.own-blocks.config.mjs`).

### TIER-0 (즉시 error — 기존 슬라이스 GREEN, false-positive 0)
- **FE-FEAT-ISOLATION** (신규 ESLint 룰 `no-cross-feature-deep-import`) — `features/A`에서 `@/features/B/...`(B의 내부 경로) import 금지. 허용: `@/features/B`(배럴), `@/components/ui`, `@/lib`. default-deny. (백엔드 HG-FEAT-ISOLATION 대응.)
- **FE-LAYER-DIRECTION** (신규 `no-upward-layer-import`) — `components/ui`→`features`/`app`, `features`→`app` import 금지(역방향). (백엔드 layering 대응.)
- **FE-PUBLISHED-API** (신규 `no-feature-internal-import`) — 슬라이스 외부에서 `@/features/<f>/<anything-but-index>` deep-import 금지; 슬라이스 배럴만. (백엔드 published-API default-deny 대응. 기존 `no-broad-barrel-imports`와 보완.)
- **FE-NO-INLINE-COMPONENT** (기존 `no-inline-component-definition`) — 유지.
- **FE-UI-PRIMITIVE-PLACEMENT** (기존 `no-app-local-ui-primitives`) — 유지.

### TIER-1 (컨벤션 의존, advisory→error, forcing function)
- **FE-ROUTE-THIN** (신규) — `app/**/page.tsx`/`layout.tsx`가 직접 `fetch`/SWR/axios 호출하거나 N줄 초과 비즈니스 로직 보유 금지 → 슬라이스 컨테이너로 위임. (백엔드 thin-controller / HG-ANTI-SPLIT 대응; 휴리스틱이라 honest limit 명시.)
- **FE-STATE-BOUNDARY** (신규/advisory) — 서버 상태(SWR/TanStack 캐시)를 `useState`/client store로 복제 금지(`web/patterns.md`). 휴리스틱 → 우회 가능, 명시.
- **FE-CONTAINER-PRESENTATION** (advisory) — 데이터 패칭 hook을 호출하는 컴포넌트는 표현 로직을 프레젠테이션 자식으로 분리 권고. (기계화 한계 → TIER-2 강등 가능.)

### TIER-2 (인간 판단) + 스캐폴드
- 슬라이스 경계 적정성(한 기능이 정말 한 슬라이스인가 / 분할·병합) · god-component 적정성(TIER-1 휴리스틱 너머) · 컨테이너/프레젠테이션 분리 적정성 · 각 allowlist 예외 remediation 여부.
- **스캐폴드**: `frontend` 신규 기능 체크리스트(slice 디렉터리 + index.ts 배럴 + 컨테이너/프레젠테이션 + 라우트 thin + cross-feature는 배럴만 + 신규 ESLint 룰 통과).

## 5. allowlist 아티팩트 — `practices-react/feature_boundary_allowlist.yaml` (schema 검증)

백엔드 `aggregate_boundary_allowlist.yaml`와 동형:
```yaml
shared_layers: ["@/components/ui/**", "@/lib/**"]   # 안정 kernel만 wildcard
published_api:                                       # 슬라이스별 공개 배럴 표면
  auth: ["@/features/auth"]
exceptions:                                          # grandfather / composition (정확 경로, wildcard 금지)
  - from: "src/features/billing/..."
    to:   "src/features/payment/internal/..."
    kind: cross-feature-deep-import
    owner: <name>
    rationale: "..."
    expiry: 2026-12-31
    remediation_ticket: AX-FE-...
```
CI 검증(신규 `feature_boundary_allowlist_guard.sh`): (1) 모든 from/to가 **실재 경로로 resolve**, (2) **만료/미사용 항목 fail** → 예외가 영구 escape hatch가 되지 않게. (백엔드 가드와 동일 패턴.)

## 6. 비-vacuity 증명

백엔드의 `DddDecompositionViolationFixtureTest`(위반 fixture로 가드 fire 증명) 대응으로, 각 신규 ESLint 룰은 **`pass`/`fail` fixture 쌍**(기존 `practices-react/eslint-plugin-ax` 테스트 패턴)을 가진다. `fail` fixture가 룰을 trip 못 하면 빌드 BLOCK. (백엔드 CM3 "hard-guard 거짓음성 = fix-now"의 프론트엔드 적용.)

## 7. 롤아웃 (phase)

1. **Phase 0 (즉시):** allowlist YAML + schema 가드 생성. TIER-0 3개 신규 ESLint 룰(`no-cross-feature-deep-import`·`no-upward-layer-import`·`no-feature-internal-import`) 작성 + pass/fail fixture. 기존 슬라이스 전 GREEN 확인(현재 features/auth만 존재 → 적은 surface).
2. **Phase 1 (배럴 정규화):** 모든 슬라이스가 `index.ts` published 배럴을 갖도록 정규화 + allowlist 기록.
3. **Phase 2 (flip):** TIER-1 휴리스틱(FE-ROUTE-THIN 등) advisory→error.
4. **Phase 3 (지속):** TIER-2 review 룰 + 스캐폴드 + headline/AGENTS(practices-react) 갱신 + grandfather remediation.

## 8. 성공 기준

- TIER-0 3개 신규 ESLint 룰이 error 모드로 기존 슬라이스 전부 GREEN(false-positive 0).
- allowlist YAML + CI 검증 가드 존재, `run-all-guards`/`verify-completion`에 통합.
- 각 신규 룰의 `fail` fixture가 룰을 trip(비-vacuity 증명).
- headline(README/CLAUDE/plugin.json) ESLint rule 수 갱신 + practices-react AGENTS.md sentinel 동기화.
- "cross-feature deep-import"·"역방향 레이어 import"·"슬라이스 내부 deep-import"가 신규 코드에서 기계적으로 차단됨을 fixture로 증명.

## 9. evidence (근거)

- **Feature-Sliced Design** (feature-sliced.design): 슬라이스 공개 API = 배럴, 레이어 단방향 의존, cross-import 규칙.
- **Next.js App Router docs**: 라우트 세그먼트/route group, 서버·클라이언트 컴포넌트 경계, `app/` 라우팅 레이어.
- **React docs**: 컴포지션, 상태 끌어올리기/분리.
- **사용자 web 규칙**: `web/patterns.md`(서버/클라이언트/URL 상태 분리), `web/coding-style.md`(feature 단위 조직), `web/design-quality.md`.
- 기존 `eslint-plugin-ax` 8 룰(특히 `no-inline-component-definition`·`no-broad-barrel-imports`·`no-app-local-ui-primitives`)을 분해 enforcement의 출발점으로 차용.

## 10. 범위 밖 (out of scope)

- 백엔드 분해 — 이미 별도 스펙으로 구현·검증 완료.
- 디자인 시스템/시각 품질 규칙 — `web/design-quality.md` 별도 트랙.
- Nx/Turborepo 모듈 경계 도입 — 미래 옵션(repo에 fixture만).
- 기존 슬라이스 실제 remediation 코드 — 후속(본 스펙은 grandfather + ticket까지).

---

## 부록 — 백엔드 ↔ 프론트엔드 대응표

| 백엔드 (DDD spec) | 프론트엔드 (본 스펙) | 강제 도구 |
|---|---|---|
| ArchUnit 하드 가드 | eslint-plugin-ax 룰 | bytecode ↔ AST |
| HG-FEAT-ISOLATION | FE-FEAT-ISOLATION (cross-feature deep-import) | 신규 ESLint |
| layering (controller→service→repo) | FE-LAYER-DIRECTION (app→features→ui/lib) | 신규 ESLint |
| @PublishedApi default-deny | FE-PUBLISHED-API (슬라이스 배럴만) | 신규 ESLint |
| thin-controller | FE-ROUTE-THIN (thin page.tsx) | TIER-1 휴리스틱 |
| HG-ANTI-GODSERVICE-TX | god-component / FE-STATE-BOUNDARY | TIER-1 휴리스틱 |
| aggregate_boundary_allowlist.yaml | feature_boundary_allowlist.yaml | schema 가드 |
| ViolationProof / BijectionTest | ESLint pass/fail fixtures | 비-vacuity |
| NEW-DOMAIN-CHECKLIST §1b | 신규 frontend-feature 체크리스트 | 스캐폴드 |
