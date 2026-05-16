# ax-template — `/ax-transform` Skill Package Source

## Project Identity

**ax = AI transformation.** 이 repo는 Claude Code skill **`/ax-transform`** 의 source. AI agent가 새 프로젝트를 부트스트랩하거나 기존 프로젝트를 AI 친화적으로 전환할 때 활성화하는 skill 전체 패키지.

핵심 가치는 "코드"가 아니라 **AI agent가 빠르게 이해하고 안전하게 작업할 수 있는 인프라**. 인증/CRUD 구현은 skill이 자신을 자신에게 적용한 reference workload — skill의 동작 시연.

**Skill 진입점**: top-level `SKILL.md` (frontmatter `name: ax-transform`). AI agent가 skill invoke 시 첫 읽음.

### 이 skill이 제공하는 것

1. **Spec Trio** — `specs/` + `contracts/` + `blueprints/`. AI가 코드보다 spec을 먼저 읽도록 강제하는 contract-first 구조. AI 환각 차단의 1차 방어선.
2. **practices/ catalog** — Java/Spring best-practices 64룰. evidence-anchored (외부 URL/quote 필수)라 AI가 임의로 룰을 발명하지 못함.
3. **Verification feedback loop** — `./gradlew test{Domain}` 단일 명령으로 binary pass/fail. AI가 자기 결과를 self-verify 가능.
4. **AGENTS.md sentinel** — AI agent가 진입 시 즉시 컨텍스트 받음. sha256 anchoring으로 catalog와 동기화 보장.
5. **4 hard gates** — spec_ref / substance / time_decay / evidence. AI 결과물이 외부 사실에 anchor 안 되면 통과 불가.

### 이 skill이 강제하지 않는 것 (fork받은 팀이 결정)

- **Git workflow** — branch protection, PR, force-push 정책. main 직접 commit, trunk-based, GitFlow 모두 가능
- **Deployment / release** — 어떻게 배포하든 catalog 품질과 무관
- **Code review** — 1인 maintainer, 팀 review, AI review 어떤 방식이든 OK
- **CI 정책** — sentinel CI는 catalog quality probe로만 제공. merge gate 여부는 fork받는 팀이 결정
- **언어/프레임워크 확장** — 현재 Java/Spring catalog만. 다른 stack 추가는 동일 패턴 (spec → rule → evidence → test) 따라 확장

→ 한 줄: **catalog 품질**은 skill이 보장, **인간 협업 정책**은 fork받은 팀 자율.

### Why this matters for AI

AI agent (Claude Code 등)가 코드를 작성할 때 가장 큰 risk:
- 본인이 모르는 규칙을 임의로 만들어냄 (환각)
- 코드만 짜고 검증 안 함
- 외부 docs / RFC 사실관계 misalignment

이 template의 모든 게이트 (spec_ref / substance / evidence / time_decay / testPractices)는 정확히 이 risk를 막기 위해 존재. 사람 review 없이도 AI 작업물의 **외부 사실 anchoring**이 binary로 검증됨.

## Core Methodology

### 검증 피드백 루프 (Validated)
```
규칙 정의 (YAML spec) → TDD 구현 → @Tag 테스트로 기계적 검증
     ↑                                        ↓
     └────── 실패 시 수정 후 재검증 ──────────┘
```

- `./gradlew testAsvs` — auth 도메인 (26 ASVS items)
- `./gradlew testCrud` — CRUD 도메인 (Spec Trio 시연)
- `./gradlew testPractices` — 64 practices rules
- `./gradlew testPortability` — advisory; 외부 fixture에 룰 적용

### 외부 참조 정규화

외부 best practice (OWASP, Spring Docs, RFC, JEP 등)를 그대로 사용하지 않는다.
반드시 **내부 canonical 구조로 흡수**:
- OWASP ASVS → `specs/auth-asvs-l1.yaml` 항목으로 pinned
- Spring Security 패턴 → `blueprints/auth-manifest.yaml` 정책으로 정규화
- API 규약 → `contracts/auth-openapi.yaml` 스키마로 계약화
- Spring/JEP 등 → `practices/upstream/*.snapshot.md` 로 snapshot + 룰의 `evidence:` block에서 URL/quote 참조

### Evidence-anchored rule provenance

`practices/rules/*.md`의 모든 룰은 `evidence:` block 필수. 두 형태:
- `upstream_id` — `practices/upstream/_MANIFEST.yaml` 의 fetched snapshot + section + quoted substring
- `source_type: external` — RFC / JEP / vendor docs / peer-reviewed paper + citation + URL

`evidence_guard.sh`가 binary로 검증. 빈 evidence / placeholder / 존재하지 않는 snapshot 모두 BLOCK.

## Anti-Patterns (금지)

### 거버넌스 무한루프 ❌

과거 실패: 30+ 문서와 18 세션을 소비했지만 코드 0줄. 원인: draft→curated→stable 승격 게이트가 데드락.

**금지 사항**:
- `TEMPLATE-GOVERNANCE.md` 같은 승격 절차 문서 생성 금지
- "curated promotion check" 같은 게이트 프로세스 금지
- "evidence bundle" 같은 검증-위한-검증 문서 금지
- 구현 없이 문서만 생산하는 계획 수립 금지

**대신**: 코드를 먼저 쓰고, `./gradlew test{Domain}`으로 검증하고, 부족하면 spec을 보강한다.

### MockMvc 전용 테스트 ❌

MockMvc 테스트는 프로젝트 패키지 구조에 결합되어 이식 불가. 검증에는 **RestAssured (black-box HTTP)** 사용.

### Fork받은 팀의 정책을 skill이 강제 ❌

ax-template는 skill 패키지 source — fork-base. 다음은 skill이 강제하면 안 됨:
- Git branch / PR / merge 정책
- 배포 / 릴리스 정책
- 팀 코드 리뷰 정책
- catalog 품질을 넘는 CI gate

skill은 **catalog quality probe만 제공**. fork받은 팀이 자신의 정책으로 채택할지 무시할지 결정.

## Build & Test

```bash
# Backend
cd backend && ./gradlew build         # 빌드
cd backend && ./gradlew test          # 전체 테스트
cd backend && ./gradlew testAsvs      # auth ASVS 검증
cd backend && ./gradlew testCrud      # CRUD spec 검증
cd backend && ./gradlew testPractices # practices/ 64룰 검증
cd backend && ./gradlew testPortability  # advisory: 외부 fixture에 룰 적용

# Frontend
cd frontend && npm run build
cd frontend && npm run test
```

## Architecture

```
ax-template/
├── SKILL.md                   # /ax-transform skill 진입점 (frontmatter: name/description)
├── CLAUDE.md                  # 본 문서 — project identity + methodology
├── METHODOLOGY.md             # 5-step blueprint playbook
├── specs/                     # 검증 스펙 (핵심 — spec-first)
│   ├── auth-asvs-l1.yaml
│   ├── crud-l0.yaml
│   ├── spring-practices-l0.yaml
│   └── portable-test-template/
├── contracts/                 # OpenAPI 계약 (핵심)
│   └── auth-openapi.yaml
├── blueprints/                # 정책 매니페스트 (핵심)
│   └── auth-manifest.yaml
├── practices/                 # AI-targeted catalog (skill 핵심 자산)
│   ├── rules/                 # 64룰, 21 categories
│   ├── upstream/              # 외부 사실 snapshot
│   ├── evals/                 # 4 hard gates + advisory probes
│   ├── AGENTS.md              # AI agent 진입점 (sha sentinel)
│   ├── SKILL.md               # practices 서브시스템 skill
│   ├── MAINTAINER.md
│   └── DECISIONS.md           # rule provenance trail
├── backend/                   # Spring Boot reference workload (skill의 self-application)
├── frontend/                  # React reference workload
├── verify/                    # 검증 스크립트 (선택적 — fork받은 팀이 채택 여부 결정)
└── docs/archive/              # 과거 거버넌스 문서 (참고용)
```

## Domains (reference workloads)

| 도메인 | Spec | 엔드포인트 | 항목 |
|---|---|---|---|
| Auth | `specs/auth-asvs-l1.yaml` | 14 (signup, login, OAuth Google/Naver/Kakao 등) | 26 ASVS items |
| CRUD | `specs/crud-l0.yaml` | 5 (CRUD-001~005) | 7 security tests |
| Practices | `specs/spring-practices-l0.yaml` | — | 64 rules / 21 categories |

각 도메인은 동일한 패턴: spec YAML → `@Tag` test → `./gradlew test{Domain}` binary verification.

## Methodology

이 프로젝트의 방법론은 `METHODOLOGY.md`에 문서화. 핵심:

**Spec Trio** (Compliance Spec + API Contract + Policy Manifest) → TDD 구현 → 단일 명령 검증 (`./gradlew test{Domain}`).

새 도메인 추가 시 `METHODOLOGY.md` 5단계 + Dry-Run Checklist를 따른다.

## Verification Scripts (선택적 — fork받은 팀이 사용 여부 결정)

```bash
verify/run-all.sh           # build + 전체 test{Domain}
verify/run-checklist.sh     # YAML 기반 자동화 체크리스트
verify/ci-gate.sh           # CI 머지 게이트 (선택적)
verify/report-kpi.sh        # KPI 리포트
```

→ template이 제공하는 도구. fork받은 팀의 git/CI 정책에 어떻게 통합할지는 자율.

## RBAC (reference workload)

| 역할 | 접근 범위 |
|------|----------|
| ADMIN | `/api/admin/**` + 모든 인증 엔드포인트 |
| MANAGER | 모든 인증 엔드포인트 |
| MEMBER | 모든 인증 엔드포인트 |

JWT에 `role` claim. SecurityConfig에서 `hasAuthority("ROLE_ADMIN")` 검사.
