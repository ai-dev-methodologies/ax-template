# ax-template — `/ax-transform` Skill Package Source

## 🔒 MANDATORY before declaring any task done (R25 — NON-NEGOTIABLE)

Before any AI agent / persona / slash command / fork-receiver may state
"task complete", they MUST run:

```bash
bash practices/scripts/verify-completion.sh
```

Exit 0 ⇒ task may be declared done.
Exit 1 ⇒ task is NOT done; apply the printed `fix_playbook` and re-run.

This is enforced mechanically by:
- `practices/verification-checklist.yaml` (machine-readable contract)
- `practices/scripts/verify-completion.sh` (executor + audit log writer)
- `practices/evals/completion_checklist_recency_guard.sh` (49th hard guard;
  pre-push hook BLOCKS the push if no recent audit log entry matches HEAD)

No `--skip` flags. No `--no-verify` blanket bypass — an emergency `git commit
--no-verify` does NOT exempt R25; `verify-completion.sh` MUST still PASS at HEAD
before the task is declared done (the pre-push recency guard enforces this for
pushes). No "I'll run it later." The catalog enforces the loop.

Optional retry orchestrator for AI agents:
```bash
bash practices/scripts/verify-and-fix-loop.sh         # interactive, 3 attempts
bash practices/scripts/verify-and-fix-loop.sh --non-interactive   # CI/headless
```

**R25 toolchain prerequisites** — `verify-completion.sh` runs a fail-closed toolchain
preflight (exit 2, BLOCK — not a silent skip) before executing the plan, gated on the
*resolved* step set (respects `--step` filtering):
- **JDK 21** — required whenever a backend/gradle step is scheduled. build.gradle.kts
  toolchain = `JavaLanguageVersion.of(21)`. Resolve via `JAVA_HOME` or PATH; the macOS
  `/usr/bin/java` stub (no runtime) fails the check. On this maintainer machine:
  `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`.
- **python3** — required unconditionally (checklist parsing, guard helpers, collapse planner).
- **PyYAML or yq** — either parses the checklist yaml (always required; the parser falls back
  to `yq -o=json` when PyYAML is absent). **PyYAML specifically** is additionally required
  whenever any catalog-guard step is scheduled (any command under an `evals/` directory) — yq
  is NOT a substitute there: the guards themselves embed `import yaml` with no yq path, and
  several SILENTLY SKIP without it, which would report a PASS for guards that never ran.
- **node + npm** — required ONLY when the `frontend-lint` step is scheduled. A
  backend-only run (`--step backend-build`, etc.) is NOT blocked by missing node.
- **bash + git** — baseline.

Reproducibility posture: `frontend/package-lock.json` is committed for reproducible
installs, but NO standing gate performs a clean `npm ci` on it — the R25 `frontend-lint`
step runs `npm run lint` against the existing `frontend/node_modules`; no frontend CI
step exists. The `practices-react/eslint-plugin-ax` sentinel CI job DOES run `npm ci`
against its committed lockfile.

---

## 🔒 PUBLIC / PRIVATE 경계 — fork-receiver 특화·민감 정보 격리 (R26 — NON-NEGOTIABLE)

ax-template은 **public fork-base catalog**다. fork-receiver(회사·팀)의 특화·민감 정보가 이
public 트리에 유입되면 안 된다. 경계는 **SPI seam**으로 강제한다 — generic interface는 public,
실제 구현은 private fork.

**Public (이 repo에 들어가도 되는 것):**
- chain-agnostic / vendor-agnostic generic 불변식 — 외부 **공개** 표준·법률·RFC·EIP·논문에 anchor
- SPI **interface** + test-double (예: `InvestorEligibility` / `HolderAuthorization` / `OnChainAnchor`)
- 검증 자산 (spec / contract / blueprint / test / guard / R25 / PIT)

**Private (fork-receiver 측 — 이 repo에 commit 절대 금지):**
- 회사 식별자: 회사명·브랜드·사업자정보·내부 URL·**입사/채용/사업 전략 맥락**
- 실제 시크릿·자격증명·엔드포인트·벤더명
- SPI **실제 구현**: 실제 체인 adapter, KYC/커스터디/정산 provider, 회사 고유 비즈니스 규칙
- 고객·계약·운영 데이터

**규율 (새 도메인 흡수 시, 예외 없음):**
1. **분류 먼저** — "이 불변식이 generic인가, fork-receiver 특화인가"를 코드 작성 **전에** 판정한다.
2. generic → public catalog + SPI **interface**. 특화 → SPI 뒤 **private fork**. 불확실 → generic
   interface로 두고 구현은 fork로 미룬다 (= "공통으로 만든 뒤 입사/도입 후 보강"; vision의 핵심 패턴).
3. **기능뿐 아니라** 주석·문서·커밋 메시지·파일명까지 fork-receiver 식별자 **0**을 유지한다.
   "왜/누구를 위해 만들었나"의 맥락이 특정 회사를 가리키면 그건 private이다.

**위반 회고 (이 규칙의 출처):** tokenized-securities 도메인의 *기능* 코드는 generic(SPI seam으로 분리)
이었으나, *맥락 문서·커밋 메시지*에 fork-receiver 회사명·도입 전략이 박힌 채 public push됨 → scrub +
history rewrite로 제거. 교훈을 R26으로 codify해 다음 흡수부터 기계적으로 차단한다.

**강제됨: `private_boundary_guard.sh` [86]** — 두 층으로 공개 트리를 기계적으로 검사한다:
- **층1 opt-in marker**: `.ax-private-markers`의 활성 ERE 패턴(회사명·브랜드·코드네임)으로
  `backend/src`·`frontend/src`·`specs`·`contracts`·`blueprints`·`practices/rules`·`docs`를 스캔.
  public base는 이 파일을 주석만으로 유지 → 층1 매칭 0. fork-receiver가 자기 식별자를 등록하면 즉시 활성화.
- **층2 generic 시크릿 휴리스틱**: PEM private key header / AWS AKIA key / API-key assignment /
  JWT 3-segment 패턴을 항상 스캔. false-positive allowlist(`EXAMPLE`·`placeholder`·`REDACTED`·`your-`·`xxxx` 및 `src/test/` 경로)로 테스트 데이터 오탐 방지.
- **비공허성 fixture 동봉**: `fail_marker`(AcmeCorp→exit 1) · `fail_secret`(RSA PEM→exit 1) · `pass_clean`(allowlist 통과→exit 0) — 세 fixture가 guard의 실제 차단을 증명한다.

---

## 📒 ALWAYS LOG to the ax-ledger (capture → 복기 → improve → feedback)

ax-template's enforcement must be **observable and self-improving**. Every interaction with the gates
is captured to a per-project, per-user trace (`.ax-ledger/`, gitignored) so it can be reviewed and fed
back into the catalog. This is a STANDING directive for any AI agent / persona working in a fork:

- **Gate runs auto-log** — `verify-completion.sh` records `gate_run`/`violation` itself. Nothing to do.
- **You MUST log, the moment it happens:**
  - a request you refuse because it would break an enforced rule/method →
    `bash practices/scripts/ax-ledger-log.sh request_rejected rule=<R##|rule-id> detail="…" severity=block actor=user`
  - any attempt to skip a gate (no-verify / skip flags / manual override) →
    `bash practices/scripts/ax-ledger-log.sh bypass_attempt detail="…" severity=block`
  - a meaningful milestone → `bash practices/scripts/ax-ledger-log.sh progress gate=<task> outcome=pass detail="…"`
- **Review (복기) on request / at session end:** `bash practices/scripts/ax-ledger-review.sh [--since <today>]`
  surfaces recurring friction + the improvement direction. Recurring friction is a catalog signal —
  classify it and feed it back via `ax-ledger-resolve.sh` + `practices/DECISIONS.md`.

The ledger is never a merge gate (judgment, not binary) — but **capture is always-on; nothing is lost.**
Full process: `skills/ax-ledger/SKILL.md`.

---

## 🪙 ultracode / multi-agent 실행 시 토큰 효율화 (STANDING)

ultracode(Workflow 멀티에이전트 오케스트레이션)로 요청받은 작업은 **서브태스크별로 모델과
추론(reasoning) 수준을 작업 난이도에 맞게 선택**한다. 전부 최상위 모델로 돌리지 않는다:

- 기계적 수집 / grep / 카운트 검증 → `haiku`
- 표준 분석 / 리뷰 렌즈 → `sonnet`
- 아키텍처 판단 / adversarial challenge / 최종 synthesis → `opus` (또는 세션 모델 상속)
- 렌즈 수·verify 투표 수도 작업 크기에 비례시킨다 (간단 확인 1–2 lens, "철저히"일 때만 5-lens+다수결)
- 위임한 탐색을 메인 루프에서 중복 수행하지 않는다 (결과만 사용)

---

## 🧬 Broadleaf-absorption 방법론 — 기계적 강제 (STANDING, 예외 없음)

외부 e-commerce 레퍼런스(현재 Broadleaf Commerce)의 **기능·불변식(invariant)을 흡수**해
카탈로그를 키울 때는 **빠르게가 아니라 정확하게·명확하게** 간다. 단 하나의 예외 없이
**기계적으로 강제 검증**된다. 방법론은 두 문서에 정의: `METHODOLOGY.md`(5-step core:
spec→rule→TDD→test→verify) + `docs/BROADLEAF-ABSORPTION.md`(흡수 pipeline).

**파이프라인 (매 vertical, 예외 없이):**
mine(opus) → **anti-re-find census**(기존 spec/rule grep — 이미 있으면 흡수 안 함) →
spec(`*-l0.yaml`) → **evidence-anchored rule**(외부 file:line **한 줄 인용** + 외부표준,
`evidence_guard`가 날조 BLOCK) → **독립 구현**(우리 클래스/구조, 이식 0) →
**behavioral 테스트 + verification-goal parity**(Broadleaf 테스트의 *의도*를 대조,
코드 아님) → **mandatory 적대적 review**(opus refute-by-default, commit 전) → R25 → commit.

**흡수는 "동일 동작"이 아니라 "불변식 성립"을 보장한다.** Broadleaf와 byte/behavior
동치가 아니라, 흡수한 correctness 불변식이 우리 코드에서 binary로 성립함 + 외부 사실
anchoring을 보장한다 (종종 Broadleaf 결함을 고쳐 더 엄격하게 흡수).

**기계적 강제 표면 (R25 안에서 binary):**
- **`broadleaf_no_port_guard.sh` [78]** — 라이선스 안전. Broadleaf는 **Fair Use License v1.0**
  (OSI/permissive 아님) → 우리 구현 트리(`backend/src`·`frontend/src`)에 Broadleaf 소스 이식
  **0** 강제 (import/package/FUL-header 0). 클론은 repo 밖, 절대 커밋 금지. 룰 evidence의
  **한 줄 인용**만 허용(fair-use 근거).
- **`broadleaf_absorption_parity_guard.sh` [79]** — 방법론 완전성 + verification-goal parity.
  흡수한 모든 vertical은 `docs/broadleaf-parity/<vertical>.md` 완전 기록 필수
  (broadleaf_source·spec_items·rule·behavioral_test·adversarial_review + Broadleaf 테스트
  의도→우리 단언 매핑 ≥1행). 참조 산출물(spec/rule/test) 실재까지 검증 — 기록이 거짓말 불가.
- **per-domain `./gradlew test{Domain}` + ViolationProofTest + adversarial review + R25** — 기존 게이트.

**원칙:** 정확성·명확성 > 속도. 새 vertical은 위 파이프라인 + 두 가드 + parity 기록 없이는
"완료" 선언 불가 (R25가 강제). 적대적 review가 green-but-hollow를 잡는다 — green 테스트 +
자기보고 ≠ correct.

---

## ⭐ Project Vision (READ FIRST — 절대 잊지 말 것)

**ax-template은 React (front) + Spring Boot (backend) full-stack 개발의
composition kit이다.** 각 component (Spec Trio · practices · practices-react ·
reference workloads · ESLint plugin · testPractices · AGENTS.md · 4 hard gates ·
/ax-transform skill) 를 **조합**해서 새 프로젝트를 시작하는 **fork-base
template**. 모든 layer에서 **규칙을 기계적으로 강제하는 선 순환 시스템**.

```
fork ax-template
    ↓ (25 L4 domains + 11 active recipes + 233 Java rules + 105 React rules + 15 ESLint rules + 109 hard guards + AGENTS.md sentinel)
새 도메인 추가 — METHODOLOGY.md의 5-step 따라
    ↓
AI agent가 Spring + React 코드 작성
    ↓
ESLint plugin + ./gradlew test{Domain} + Spec Trio + AGENTS.md + 4 hard gates 가 자동 enforce
    ↓
규칙 밖 AI output BLOCKED (commit / push / CI)
    ↓
catalog로 새 도메인 rule 추가 — 다음 fork 더 강력
    ↓
loop
```

**핵심 원칙 (이 framing을 흔들지 말 것):**

1. **Java/Spring 측과 React/Next.js 측은 둘 다 active equal partner**. 어느
   한 쪽을 archive로 강등하거나 frozen으로 표시하지 않는다.
2. **새 도메인 / 새 규칙 추가는 정상 활동**. catalog 확장은 시스템 가치 증가.
   "stop adding rules" 류 권고는 이 vision과 충돌 — 받아들이지 않는다.
3. **README hero는 "composition kit + 선 순환 시스템"**. 단일 npm 패키지나
   단일 ESLint plugin product로 좁히지 않는다. adoption probability 같은 single
   product 메트릭으로 평가하지 않는다.
4. **`/react-best-practices` 스타일 코드 템플릿 + 강력한 규칙 + 명확한 개발
   방향** = 어떤 개발이든 이 템플릿과 규칙 내에서만 동작하는 코드 생성.

**종착점 (north star) — 2026-06-10 재정의:**

- **(1) 기계적 강제**: 룰/정책/템플릿 밖 구현 0 허용 — 자체 HEAD에서는 4 hard gates +
  hard guards + per-domain task + R25로 달성·유지. fork-receiver 측 강제는 vision상
  영구 optional(자율성 원칙) — 이 비대칭은 결함이 아니라 설계다.
- **(2) backlog convergence** (구 "100% 완전성"의 재정의): 17개 산업 dogfood(IDW1-17)가
  매 산업 새 correctness signature를 발견함으로써 "갭 0"은 비종결적임이 경험적으로
  증명되었다. 따라서 종착점은 **`docs/BACKLOG.md`(canonical backlog)의 P0–P3 수렴률**로
  측정한다. **신규 산업 dogfood(IDW18+)는 수렴률 ≥ 70% 전까지 동결.** 원칙 2와의 관계:
  backlog 내 항목의 spec화·구현·룰 추가는 계속 정상 활동이다 — 동결 대상은 *새 산업을
  열어 새 signature를 찾는 행위*뿐.

→ 한 줄: **Korean enterprise standard stack (React + Spring Boot) 위에 AI agent가
규칙 안에서만 동작하는 코드를 짤 수 있게 하는 composition kit + 자체 강화
catalog 시스템.**

상세 내용은 README.md (composition-kit framing 정식 문서) 참조.

---

## Project Identity

**ax = AI transformation.** 이 repo는 Claude Code skill **`/ax-transform`** 의 source. AI agent가 새 프로젝트를 부트스트랩하거나 기존 프로젝트를 AI 친화적으로 전환할 때 활성화하는 skill 전체 패키지.

핵심 가치는 "코드"가 아니라 **AI agent가 빠르게 이해하고 안전하게 작업할 수 있는 인프라**. 인증/CRUD 구현은 skill이 자신을 자신에게 적용한 reference workload — skill의 동작 시연.

**Skill 진입점**: `skills/ax-transform/SKILL.md` (frontmatter `name: ax-transform`). Plugin manifest는 `.claude-plugin/plugin.json` + `.claude-plugin/marketplace.json`(R109, D-4) — 두 파일이 존재하므로 plugin 채널 설치가 실제로 가능하다: `claude plugin marketplace add ai-dev-methodologies/ax-template` → `claude plugin install ax-transform@ax-transform` (세션 내 `/plugin marketplace add …` → `/plugin install …` 동일). 설치 전제·확인·트러블슈팅의 정본은 `docs/USAGE-GUIDE.md` §2, 채널 개념 비교는 `docs/PLUGIN-CHANNEL.md`.

### 이 skill이 제공하는 것

1. **Spec Trio** — `specs/` + `contracts/` + `blueprints/`. AI가 코드보다 spec을 먼저 읽도록 강제하는 contract-first 구조. AI 환각 차단의 1차 방어선.
2. **practices/ catalog** — Java/Spring best-practices 233룰 + practices-react 102룰 + ESLint 15룰. evidence-anchored (외부 URL/quote 필수)라 AI가 임의로 룰을 발명하지 못함.
3. **Verification feedback loop** — `./gradlew test{Domain}` 단일 명령으로 binary pass/fail. AI가 자기 결과를 self-verify 가능.
4. **AGENTS.md sentinel** — AI agent가 진입 시 즉시 컨텍스트 받음. sha256 anchoring으로 catalog와 동기화 보장.
5. **4 hard gates** — spec_ref / substance / time_decay / evidence. AI 결과물이 외부 사실에 anchor 안 되면 통과 불가.

### 이 skill이 강제하지 않는 것 (fork받은 팀이 결정)

- **Git workflow** — branch protection, PR, force-push 정책. main 직접 commit, trunk-based, GitFlow 모두 가능
- **Deployment / release** — 어떻게 배포하든 catalog 품질과 무관
- **Code review** — 1인 maintainer, 팀 review, AI review 어떤 방식이든 OK
- **CI 정책** — sentinel CI는 catalog quality probe로만 제공. merge gate 여부는 fork받는 팀이 결정
- **언어/프레임워크 확장** — Java/Spring 카탈로그(233 rules) + React/Next.js 카탈로그(105 rules + 15 ESLint rules) 둘 다 active. 다른 stack(Kotlin/Go/Python 등) 추가는 동일 패턴 (spec → rule → evidence → test) 따라 확장.

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
- `./gradlew testPractices` — 233 practices rules
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

evidence **내용 진위**는 3계층으로 backstop된다(structure만으로는 조작 quote/URL이 통과하므로):
- `evidence_guard.sh` (blocking) — 구조(upstream_id 해석, section/quote/citation/url 비어있지 않음).
- `evidence_quote_spotcheck_guard.sh` [74] (advisory, offline) — `upstream_id` quote가 snapshot body에
  실제 substring으로 존재하는지 결정론적 sweep.
- `external_url_spot_audit.sh` (periodic ADVISORY, network — R25 guard 아님) — `source_type: external`
  URL을 live-fetch해 OK / SUSPICIOUS(reachable인데 URL이 claim한 id 부재 = 조작 후보) / UNREACHABLE
  3-bucket 분류. P2-1b baseline: verifiable subset 42 URL에서 confirmed-fabricated 0.

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

### "단일 명령 binary pass/fail" — 정확한 의미

catalog claim **"`./gradlew test{Domain}` — 단일 명령 binary pass/fail"** 은
**per-domain task** 에 적용된다. **R22 baseline (2026-05-21)**: per-domain
task 전 GREEN + aggregate `./gradlew test` 는 advisory PortabilityCyclic
한 건을 제외하면 GREEN. 도메인별 상태:

> 아래 표는 **대표 스냅숏(R22 baseline)** 이며 전수 목록이 아니다. 전체 per-domain task의 canonical 출처는 backend/build.gradle.kts(115 register<Test>) — `cd backend && ./gradlew tasks`로 조회. 신규 도메인은 verification-checklist.yaml + build.gradle.kts에 자동 등재된다.

| Per-domain task                | 상태       | 비고 |
|--------------------------------|----------|---|
| `./gradlew testCrud`           | GREEN    | 7/7 PASS |
| `./gradlew testAsvs`           | GREEN    | 26 ASVS items PASS |
| `./gradlew testPractices`      | GREEN    | 233 rules PASS |
| `./gradlew testRateLimit`      | GREEN    | RATELIMIT 전 PASS |
| `./gradlew testNotification`   | GREEN    | NOTIFICATION 전 PASS |
| `./gradlew testPayment`        | GREEN    | PAYMENT 29 items PASS |
| `./gradlew testIdentityVerification` | GREEN | 19/19 PASS. R54 backend residual closure: VerifiedIdentity entity (ci/di/name/dob/verifiedAt/providerName + ElementCollection metadata) + VerifiedIdentityRepository + PassAdapter/KcbAdapter (IdentityVerificationProvider canonical shape) + IdentityVerificationService (sole mutator + AuditLogService.record publish for SUCCESS/HMAC_FAIL/UNKNOWN_PROVIDER/EXTRACTION_FAIL) + IdentityVerificationException (UNKNOWN_PROVIDER→400 / HMAC_FAIL→401 / EXTRACTION_FAIL→422) + IdentityVerificationAdminController (GET /api/admin/identity-verification @PreAuthorize ROLE_ADMIN + CacheControl.noStore) + V025 migration. 7 spec items / 4 families (CALLBACK × 3, PROVIDER × 2, AUDIT × 1, ADMIN × 1). spec `domain_mode: backend_only` 유지 — frontend 없음 (fork-receiver 자율성 보존). VerifiedIdentityViolationProofTest 4 structural assertions (no-rrn field, immutable columns, no-public-setter, audit-action constant). |
| `./gradlew testBilling`        | GREEN    | 17/17 PASS. R21 backend impl: Subscription/Plan/BillingEvent + SubscriptionStateMachine + SubscriptionController + BillingAdminController + BillingWebhookController + ArchUnit BOUNDARY/STATE checks. SecurityConfig 가 `/api/webhooks/billing` permitAll, `/api/subscriptions/**` authenticated. 마지막 spec-only RED 도메인 closure |
| `./gradlew testFeatureFlags`   | GREEN    | 11/11 PASS |
| `./gradlew testWebhook`        | GREEN    | 13/13 PASS |
| `./gradlew testScheduledTask`  | GREEN    | 5/5 PASS |
| `./gradlew testAuditLog`       | GREEN    | 11/11 PASS |
| `./gradlew testFileStorage`    | GREEN    | 12/12 PASS |
| `./gradlew testSearch`         | GREEN    | 8/8 PASS |
| `./gradlew testReportExport`   | GREEN    | 23/23 PASS. R29 backend impl: ExportJob/Status/Format + ExportJobStateMachine (sole mutator) + ExportWorker (@Scheduled poller + processOne for synchronous test path) + CsvWriter (RFC 4180 + UTF-8 BOM) + XlsxWriter (Apache POI SXSSF streaming) + FormulaInjectionGuard (shared CWE-1236 neutralizer for CSV + XLSX). 11 spec items / 4 families (AUTHZ, LIFECYCLE, INJECT, FORMAT). 의존성 추가: org.apache.poi:poi-ooxml:5.2.5 |
| `./gradlew testApiKey`         | GREEN    | 16/16 PASS. R30 backend impl: ApiKey + ApiKeyStatus(ACTIVE/REVOKED) + ApiKeyScope(READ/WRITE) + ApiKeyHasher (SHA-256 hex + MessageDigest.isEqual constant-time) + ApiKeyService (atomic rotate) + ApiKeyAuthenticationFilter (X-API-Key, skips management surface for KEY-AUTHZ-001) + ScopeProbeController. 12 spec items / 4 families (AUTHN, STORAGE, LIFECYCLE, AUTHZ). Filter MUST disable FilterRegistrationBean auto-registration to avoid running outside the security chain. /api/auth/me reads Jwt principal → not API-key-friendly; tests use scope-probe/whoami (Authentication.getName()) instead. |
| `./gradlew testApprovalWorkflow` | GREEN  | 26/26 PASS (R31 + iter1+2 dogfood closure). R31 backend impl: ApprovalRequest + ApprovalStep entities (OneToMany cascade) + ApprovalRequestStatus(DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED) + ApprovalStepStatus(PENDING/APPROVED/REJECTED) + ApprovalRequestStateMachine + ApprovalStepStateMachine (both sole-mutator) + ApprovalService (single @Transactional cascades). 15 spec items / 5 families (LIFECYCLE × 4, AUTHZ × 3, STEP × 5, QUERY × 2, PAYLOAD × 1). 한국 enterprise 결재 패턴 (sequential ordered, 결재선). Iter1 dogfood closure: payload immutability (JPA updatable=false), duplicate approver guard, self-approve guard, VIOLATION proof tests (5). |
| `./gradlew testTagCategorization` | GREEN | 27/27 PASS (R32 + iter1 VIOLATION proof). R32 backend impl: Tag + TagAttachment entities (polymorphic via entity_type/entity_id pair) + TagSlugger (NFKD normalize + ASCII filter + hyphenate + Korean fallback) + TagService + TagController (@PreAuthorize ROLE_ADMIN on definition mutations). 12 spec items / 4 families (CRUD × 4, ATTACHMENT × 3, HIERARCHY × 3, AUTHZ × 2) + iter1 TagViolationProofTest (8 deliberate-break checks). |
| `./gradlew testSessionManagement` | GREEN | 23/23 PASS (R33 + iter1 dogfood). R33 backend impl: SessionRecord + IpAddressMasker + UserAgentSummarizer + SessionRevocationCheck SPI (fail-closed default) + AdminSessionController (@PreAuthorize ROLE_ADMIN force-logout) + SessionService (idempotent register on (userId, jti), max-sessions auto-revoke). 14 spec items / 4 families (LIFECYCLE × 5, REVOCATION × 3, INTROSPECTION × 3, AUTHZ × 3). Raw IP / User-Agent stored on entity but @JsonIgnore — only masked/summarized forms reach DTOs. Iter1 dogfood closure: SESS-LIFECYCLE-004 (expiresAt past rejection) + SESS-LIFECYCLE-005 (max-active auto-revoke enforcement). |
| `./gradlew testFavorites`      | GREEN    | 17/17 PASS (R34 + iter1 violation proof). R34 backend impl: Favorite entity (polymorphic via entity_type/entity_id pair + UNIQUE(user_id, entity_type, entity_id)) + FavoriteService (idempotent add/delete, global count, quota enforcement). 12 spec items / 4 families (CRUD × 3, QUERY × 3, AUTHZ × 3, VALIDATION × 3) + FavoriteViolationProofTest (5 structural assertions). |
| `./gradlew testActivityFeed`   | GREEN    | 18/18 PASS (R35 + iter1 violation proof). R35 backend impl: ActivityEvent (actor/verb/object/audience@ElementCollection) + ActivityRead (per-(event,user) read state) + ActivityService (idempotent publish via (actor, idempotencyKey), fan-out-read feed, mark-read + mark-all-read) + ActivityController. 12 spec items / 4 families (PUBLISH × 3, READ × 3, MARK × 3, AUTHZ × 3) + ActivityViolationProofTest (6 structural assertions). ActivityStreams 2.0 vocabulary; visibility = actor OR audience contains caller. |
| `./gradlew testCommentThread`  | GREEN    | 18/18 PASS (R36 + iter1 violation proof). R36 backend impl: Comment entity (polymorphic via entity_type/entity_id + parentCommentId reply hierarchy + body NULLABLE for soft-delete) + CommentEdit (immutable edit history) + CommentService (author-only edit, author-or-admin delete, IDOR-safe 404 history visibility) + CommentController (6 endpoints). 12 spec items / 4 families (CRUD × 3, THREAD × 3, AUTHZ × 3, HISTORY × 3) + CommentViolationProofTest (6 structural assertions). Soft-delete masks body as '[deleted]'; admin CANNOT edit (only delete). Edit history preserved across soft-delete (audit posture). |
| `./gradlew testEmailOutbox`    | GREEN    | 24/24 PASS (R51 + R60 dogfood iter1 closure + R67 helper lift). R51 backend impl: EmailOutbox (PENDING/RETRY/SENT/DLQ enum, content columns @Column(updatable=false), MAX_RETRIES=3 terminal, exponential backoff (1L << retryCount) * 30L seconds) + EmailTemplate ({{var}} substitution) + EmailOutboxService (sole mutator + SLF4J audit.email-outbox structured logger) + EmailOutboxAdminController (@PreAuthorize ROLE_ADMIN + CacheControl.noStore + 409 EMAIL_OUTBOX_INVALID_TRANSITION on SENT row retry) + LoggingEmailSenderService (@ConditionalOnMissingBean default; refuses prod profile unless explicit opt-in). 8 spec items / 5 families (QUEUE × 2, SEND × 2, RETRY × 2, TEMPLATE × 1, ADMIN × 1) + 4 ViolationProof (immutable columns, no public setters, MAX_RETRIES constant, exponential formula) + 12 PiiHelper (R60). R60 dogfood F1/F2/F3/F6/F8/F9/F11 HIGH/MEDIUM closures: recipient hash on AUDIT, sanitizeReason on lastError storage path, processQueue batch summary, OutboxPage pagination metadata, distinct ADMIN_DELETE vs ADMIN_DELETE_ABSENT audit verbs. R67 helper lifted to common.AuditPiiHelper (cross-cutting). |
| `./gradlew testDsr`            | GREEN    | 15/15 PASS (IMW6 — IDW4 regulated-axis #6 closure). data-subject-rights backend lift composing specs/data-subject-rights-l0.yaml (GDPR Art 15-20): DsrRequest entity + DsrRequestStateMachine (sole mutator) + PersonalDataProvider SPI + DsrService (sole orchestrator: ACCESS/RECTIFY/ERASURE/PORTABILITY/RESTRICT/SLA) + DsrRestrictionGate (fail-closed 423) + DsrMetrics (3 bounded {tenant,type} Micrometer meters) + DsrSlaSweeper (@Scheduled 30-day SLA) + V030. 7 spec items / 10 compliance + 5 ViolationProof. Adversarial review closed 3 real bugs pre-merge: restriction gate now consulted on access+erase (not just rectify/portability), portability gate precedes format-validation, erasure idempotency persists the manifest (re-request returns it verbatim, no re-erase). full_trio spec realized backend-only (frontend trio deferred). |
| `./gradlew testI18n`           | GREEN    | 7/7 PASS (i18n-policy backend-only L4; 5 spec items / 4 families — LOCALE-NEG, MESSAGE-SOURCE, TIMEZONE, FORMATTING + 2 adversarial-closure). 새 @SpringBootTest 는 @DirtiesContext(BEFORE_CLASS) 적용 (R22 ContextCache lever) |
| `./gradlew testRealtime`       | GREEN    | 6/6 PASS (realtime-policy backend-only L4; SSE-first via MVC SseEmitter — RT-CHANNEL-AUTH/FANOUT/BACKPRESSURE/RECONNECT; RT-PROTOCOL-001 review-only) |
| `./gradlew testEcommerce`      | GREEN    | 11/11 PASS (R23 e-commerce capstone — EcommerceE2ETest composes crud + payment + notification + audit-log + search; ECOM-CART/CHECKOUT/INV/STATE/AUTHZ) |
| `./gradlew testCommonPrimitives` | GREEN | cross-cutting common/* primitives (BreakGlass/BulkResult/CallerScope/Consent/Idempotency/PageEnvelope/ParticipantScope) + MDC correlation-id IT. 2026-06-01 audit 가 @Tag→per-domain-task hard-gate escape 를 닫으며 신설 (guard [66]) |
| `./gradlew testThresholdTerminal` | GREEN | 12/12 PASS (BACKLOG P0-25 — IDW17 threshold-terminal-derivation: crossing accrual이 same-tx로 비가역 EXPIRED 유도, derived use fail-closed, @Check anchor<limit OR terminal) |
| `./gradlew testRecordLinkage`  | GREEN    | 10/10 PASS (P1-33~34 record-linkage: Fellegi-Sunter banded verdict 기록 + REVIEW 밴드 인간 결정 + per-field survivorship + loser tombstone-never-delete + chained resolve) |
| `./gradlew testTrueUp`         | GREEN    | 13/13 PASS (P1-14~17,19 — wave-8 remeasurement-trueup: 추정→실측 supersession(append-only, 강등 422) + 버전드 run(basis 기록, 멱등 재계산) + CLOSED 기간 NET delta forward 포스팅(보존 run_of_record+Σpostings==latest) + grid 결측 422 + OPEN→CLOSED→SEALED fail-closed) |
| `./gradlew testObligation`     | GREEN    | 10/10 PASS (P1-10~13 deadline-obligation: grounded 도출식 기한 + multi-axis min(candidates) + ladder exactly-once + closed-loop ack-only terminal, EXPIRED 없음) |
| `./gradlew testDecisionGov`    | GREEN    | 12/12 PASS (P1-1~4 decision-governance: basis 불변 스냅숏 + 재산정 reason 필수 append + override 4-eyes(@Check approved_by<>decided_by) + 8-thread 동시성 keystone) |
| `./gradlew testBundlePricing`  | GREEN    | 15/15 PASS (Broadleaf 재감사 #1 — bundle/kit 합성가격 보존 roll-up: ITEM_SUM Σ(child unit×qty)+fees, BUNDLE 고정가, taxability child 파생; stored-total 없음 derive-on-read로 위반 불가; adversarial opus ACCEPT. 앵커 IFRS 15/ASC 606) |
| `./gradlew testOfferEligibility` | GREEN | 14/14 PASS (Broadleaf 재감사 #3 — offer 적격성: fail-closed deny-by-default qualifier→target min-qty + customer/segment gate; 부적격은 discount 경로 도달 불가. 앵커 CWE-636/840/285) |
| `./gradlew testTaxApplication` | GREEN    | 14/14 PASS (Broadleaf 재감사 #4 — tax-exempt skip→0세 + idempotent recompute: UNIQUE(order_id) 엔티티-레벨로 정확히 1 tax row, now-exempt 시 prior 삭제; rate injected. 앵커 RFC 9110 §9.2.2 + CWE-840) |
| `./gradlew testCurrencyArithmetic` | GREEN | 14/14 PASS (Broadleaf 재감사 #5 — cross-currency fail-closed: CurrencyMoney plus/minus 통화 불일치 시 422 throw before any value/persist, convertedVia만 cross-ccy seam; common/Money.java 무수정. 앵커 ISO 4217 + Fowler + CWE-682) |
| `./gradlew testTokenizedSecurities` | GREEN | 29 PASS (compliance + ViolationProof + dogfood E2E) — STO 5-seam (chain-agnostic): **TRANSFER** — 증권토큰 이전이 적격성/lock-up/보유한도/잔고 게이트를 mutation 전 fail-closed로 통과해야만 append-only 계좌부 변경(보존 Σholdings==totalUnits) + 멱등(transferId) + issuer treasury 면제 + deny-by-default 적격성 SPI(InvestorEligibility, fork가 ERC-3643 ONCHAINID로 교체); **REGISTER** — 기초자산 단일증권화(자산레벨 이중기재0, 409) + 발행총량 finality; **HOLDER-AUTHZ** — OwnershipHolderAuthorization SPI(소유 기반 통제); **ANCHOR** — OnChainAnchor SPI + 재조정(off-chain↔on-chain); **ISSUE** — 발행 라이프사이클 state machine. PIT 변이테스트가 fail-closed SPI들을 kill-proof (guard[84][85]). 전자증권법 분산원장 계좌부 + EIP-3643 흡수. backend_only. caller↔holder 바인딩은 HolderAuthorization SPI(OwnershipHolderAuthorization, deny-by-default)로 이미 구현·검증됨(SecurityTokenRegisterService.transfer() 라인 110, TokenizedSecuritiesComplianceTest#transferFromUncontrolledHolder_isRejected_403_registerUnchanged @Tag HOLDER-AUTHZ-001, TokenizedSecuritiesViolationProofTest 라인 135); Phase 1 잔여 = ERC-3643 ON-CHAIN identity(ONCHAINID) 바인딩 — fork-receiver 관심사 |
| `./gradlew testPortability`    | advisory | 외부 fixture (spring-realworld-example-app) 에 cycle 있음. fork-receiver의 코드가 아니라 외부 reference 코드의 결함 |

전체 `./gradlew test` aggregate 도 **PortabilityCyclic advisory 1건을 제외하면
GREEN.** R20–R21 에서 표면화됐던 aggregate-only failure (FeatureFlagFlowIT
11건 spurious 401) 의 root cause 는 Spring TestContext `ContextCache` 의 기본
용량 32 한계였다 — 전체 71개 `@SpringBootTest` 클래스가 LRU 회전을 일으켜
`auth.signup.auto-verify=true` 라는 동일한 properties cache key 를 공유하던
BillingFlowIT context 가 후속 IT 들 사이에서 evict 되었고, FF 가 stale
cache miss 로 재진입할 때 `@LocalServerPort` 의 port 값이 죽은 Tomcat 의
포트를 가리키게 되어 모든 요청이 401 로 응답되었다. **R22 fix (당시)**:
BillingFlowIT + FeatureFlagFlowIT 두 클래스에 `@DirtiesContext(BEFORE_CLASS)`
를 적용해 매 클래스 시작 전 fresh context boot 을 강제.

**R22-blanket (2026-07-28) — per-class 완화책을 root-cause fix 로 대체.** 위
per-class 레버는 *증상이 어느 클래스에 떨어졌는지* 를 따라다니는 방식이라
근본이 아니었다. flake 가 그때그때 운 나쁜 클래스로 이동했을 뿐이다
(BillingFlowIT → FeatureFlagFlowIT → PageEnvelopeCatalogSweepTest →
GlobalProblemDetailAdviceTest).

**증명된 것**: eviction 이 실제로 일어나고 있었다. 증상은 이미 닫힌 Tomcat 의
`@LocalServerPort` 를 든 클래스가 전건 uniform 실패(격리 실행은 통과)이며,
eviction 은 live 고유 키 수가 상한을 넘어야만 발생하므로 **이 트리의 고유 키
수 > 32** 다. 상한을 올릴 근거는 이 부등식이지 특정 숫자가 아니다.

**측정되지 않은 것**: 정확한 고유 키 수. 아래 센서스는 *키를 forking 하는
어노테이션을 보유한 클래스 수*이지 키 수 자체가 아니다(여러 클래스가 한 키를
공유할 수도, 한 클래스가 중복 계수될 수도 있다) — `@SpringBootTest` **191개**
중 보유 **57개** (`properties=` 19 · `@TestPropertySource` 9 ·
`@AutoConfigureMockMvc` 14 · `@Import` 8 · `@TestConfiguration` 5 ·
`@MockitoBean` 2). 상한의 크기를 정하는 동기이지 측정치가 아니다.

fix 는 `backend/build.gradle.kts` 의 `tasks.withType<Test>` 에서
`spring.test.context.cache.maxSize = 128` 을 지정하는 것 (+ heap 2g → 4g).
`maxSize` 는 **eviction 상한이지 preallocation 이 아니다** — 128 로 올려도
컨텍스트는 run 이 실제로 필요로 하는 만큼만 생성되며(`DefaultContextCache` 의
내부 map 크기는 설정된 상한과 무관), 191개 클래스에
`@DirtiesContext` 를 다는 대안(=191 회 cold boot, 캐싱 무력화)보다 엄격히
싸다. 기존 4개 클래스의 `@DirtiesContext(BEFORE_CLASS)` 는 belt-and-braces 로
남겨둔다.

이 설정은 **ax-template 자신의 test harness 설정**이다. fork-receiver 의 CI
정책(merge gate·branch protection)에는 아무것도 강제하지 않는다 — 그 경계는
"우리 스위트가 결정론적인가" 와 무관하다.

```bash
# Backend
cd backend && ./gradlew build         # 빌드
cd backend && ./gradlew test          # 전체 — 위 매트릭스의 aggregate
cd backend && ./gradlew testAsvs      # auth ASVS 검증 (GREEN)
cd backend && ./gradlew testCrud      # CRUD spec 검증 (GREEN)
cd backend && ./gradlew testPractices # practices/ 233룰 검증 (GREEN)
cd backend && ./gradlew testPortability  # advisory: 외부 fixture에 룰 적용

# R22 ContextCache 감시 프로브 (BACKLOG P3-84) — 예전 상한으로 aggregate 재실행.
# 트리거: 대형 test-infra 변경 전(신규 @SpringBootTest 배치, Spring/Boot 업그레이드,
#         cache-size 변경) + 최소 분기 1회.
# 기대 exit 0. 평소 aggregate 대비 새로 깨지는 것이 있으면 그것은 flake가 아니라
# 진짜 테스트 간 상태 누수다 — 상한을 올려 덮지 말고 그 테스트를 고친다.
# -D가 아니라 -P: 데몬 -D는 같은 데몬을 재사용하는 이후 실행까지 남아
# 전체 스위트를 몰래 프로브 상한으로 돌린다.
(cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
   ./gradlew test -PcontextCacheMaxSize=32)

# Frontend
cd frontend && npm run build
cd frontend && npm run test
cd frontend && npm run lint   # ax/* React 카탈로그 룰 (--max-warnings 0) — R25 frontend-lint step (P2-4)
```

> **R25 frontend-lint (P2-4, 2026-06-24~)**: `verification-checklist.yaml`의 `frontend-lint`
> step이 `npm run lint`를 돌린다. 6개 reference 앱이 14 ax/* React 룰을 0-위반으로 통과해야 R25 PASS —
> backend 회귀와 동일하게 React 카탈로그 회귀도 게이트를 HARD-FAIL시킨다. `no-god-route` +
> `no-server-state-in-local-state`는 P2-2에서 warn→error 승격됨.

## Architecture

```
ax-template/
├── .claude-plugin/plugin.json # Claude Code plugin manifest
├── skills/
│   └── ax-transform/
│       └── SKILL.md           # /ax-transform skill 진입점 (frontmatter: name/description)
├── README.md                  # 외부 진입점 — repo의 첫 문서
├── CLAUDE.md                  # 본 문서 — project identity + methodology (AI agent 진입)
├── METHODOLOGY.md             # 5-step blueprint playbook
├── specs/                     # 검증 스펙 (핵심 — spec-first)
│   ├── auth-asvs-l1.yaml
│   ├── crud-security.yaml
│   ├── spring-practices-l0.yaml
│   └── portable-test-template/
├── contracts/                 # OpenAPI 계약 (핵심)
│   └── auth-openapi.yaml
├── blueprints/                # 정책 매니페스트 (핵심)
│   └── auth-manifest.yaml
├── practices/                 # AI-targeted catalog (skill 핵심 자산)
│   ├── rules/                 # 233룰, 22+ categories (R50/R58/R61 추가분 포함)
│   ├── upstream/              # 외부 사실 snapshot
│   ├── evals/                 # 4 hard gates + 108 hard guards
│   ├── AGENTS.md              # AI agent 진입점 (sha sentinel)
│   ├── SKILL.md               # practices 서브시스템 skill
│   ├── MAINTAINER.md
│   └── DECISIONS.md           # rule provenance trail
├── templates/
│   ├── L0/fork-receiver-kit/  # R53 — cross-cutting client primitives
│   │                          # (use-caller-id / parse-error / entity-key)
│   ├── L2/blocks/             # 30+ reusable widgets (rate-limit-banner R56,
│   │                          # confirm-dialog, offline-banner, toast 등)
│   └── L4/                    # 25 domain verticals (full-trio 20 + INFRA 1 + backend-only 4: multi-tenant, i18n-policy, ratelimit, realtime-policy)
├── backend/                   # Spring Boot reference workload (skill의 self-application)
│   └── src/main/java/com/ax/template/authblueprint/
│       ├── common/            # R67 — backend cross-cutting helpers (AuditPiiHelper)
│       └── <domain>/          # 20 domain modules + identity-verification (backend_only)
├── frontend/                  # React reference workload
├── docs/
│   ├── IMPLEMENTATION-STATUS.md  # 25 L4 status taxonomy (refreshed 2026-05-31)
│   └── dogfood-ledger/           # R71 — classified findings per dogfood iteration
├── verify/                    # 검증 스크립트 (선택적 — fork받은 팀이 채택 여부 결정)
└── docs/archive/              # 과거 거버넌스 문서 (참고용)
```

## Domains (reference workloads)

| 도메인 | Spec | 엔드포인트 | 항목 |
|---|---|---|---|
| Auth | `specs/auth-asvs-l1.yaml` | 14 (signup, login, OAuth Google/Naver/Kakao 등) | 26 ASVS items |
| CRUD | `specs/crud-security.yaml` | 5 (CRUD-001~005) | 7 security tests |
| Practices | `specs/spring-practices-l0.yaml` | — | 233 rules / 22 categories |

각 도메인은 동일한 패턴: spec YAML → `@Tag` test → `./gradlew test{Domain}` binary verification.

## Methodology

이 프로젝트의 방법론은 `METHODOLOGY.md`에 문서화. 핵심:

**Spec Trio** (Compliance Spec + API Contract + Policy Manifest) → TDD 구현 → 단일 명령 검증 (`./gradlew test{Domain}`).

새 도메인 추가 시 `METHODOLOGY.md` 5단계 + Dry-Run Checklist를 따른다.

**새 backend 도메인 scaffold (단일 진입점)**: `docs/NEW-DOMAIN-CHECKLIST.md` — 필수 산출물
(entity·repo·service·thin-controller·state-machine·domain-advice·V###.sql·ComplianceTest·**ViolationProofTest 필수**·per-domain gradle task),
재사용할 `common/` reference 구현(OptimisticLockingSupport / GlobalProblemDetailAdvice / IdempotencyKeyStore / AuditPiiHelper),
그리고 빌드를 막는 강제 가드(layering / entity-migration / unbounded-list / problemdetail / @Tag-uppercase / reachability)를
한 곳에 정리. IDW1 dogfood가 "필수 산출물을 역설계해야 했다"는 갭을 닫기 위해 추가.

## Verification Scripts (선택적 — fork받은 팀이 사용 여부 결정)

```bash
verify/run-all.sh           # build + 전체 test{Domain}
verify/run-checklist.sh     # YAML 기반 자동화 체크리스트
verify/ci-gate.sh           # CI 머지 게이트 (선택적)
verify/report-kpi.sh        # KPI 리포트
```

→ template이 제공하는 도구. fork받은 팀의 git/CI 정책에 어떻게 통합할지는 자율.

## Enforcement surfaces (what blocks where)

ax-template의 강제 표면은 **3개 계층**으로 분류된다. 전체 guard suite는 수동 실행 전용이며,
fork-receiver의 활성화는 opt-in이다.

| Surface | 파일 | 트리거 조건 | 차단 계층 | 활성화 |
|---------|------|------------|----------|--------|
| PreToolUse hook (Claude Code) | `.claude/settings.local.json` | Write/Edit이 `practices/rules/` 파일에 닿을 때 | session-bound advisory (commit 시 재검증 필요) | claude 세션 자동 |
| `.githooks/pre-commit` | `.githooks/pre-commit` | `practices/` 또는 `practices-react/` 변경 포함 커밋 — **spec_ref · substance · evidence · time_decay** 4개 binary gate 실행 | **commit-blocking** (exit 1이면 커밋 불가) | **opt-in per clone**: `bash practices/scripts/install-hooks.sh` |
| `.githooks/pre-push` (49th guard) | `.githooks/pre-push` | 커밋을 ship하는 모든 push 시 (delete-only push는 제외) — `completion_checklist_recency_guard.sh`가 HEAD에 대한 최신 R25 audit log 항목을 요구 | **push-blocking** (audit log 없으면 push 불가) | **opt-in per clone**: `bash practices/scripts/install-hooks.sh` |
| `run-all-guards.sh` (103 live guards) | `practices/evals/run-all-guards.sh` | R25 완료 선언 시 수동 호출 (verify-completion.sh 내부에서 실행) | **manual / R25 run** — 자동 트리거 없음 | 항상 사용 가능, 자동 실행 아님 |
| `per-domain ./gradlew test{Domain}` | `backend/build.gradle.kts` | 수동 또는 fork-receiver CI에서 호출 | **manual / CI** — 자동 트리거 없음 | 항상 사용 가능; CI 통합은 fork-receiver 자율 |
| `ax-case-sensitive-sweep.sh` (P2-72 로컬/수동 절반) | `practices/scripts/ax-case-sensitive-sweep.sh` | 사람이 주기적으로 호출 (릴리스 전 1회 권장) — 대소문자-**민감** APFS 볼륨을 만들어 HEAD를 클론하고 그 위에서 `run-all-guards.sh --include-fixtures`를 돌린다. **정규화 반쪽은 못 덮는다**(이 볼륨이 NFC/NFD를 접는 것이 측정됨) | **periodic / manual** — 자동 트리거 없음, merge gate 아님 | 항상 사용 가능(macOS `hdiutil` 필요; 없으면 **소리내어 실패**하고 skip하지 않는다) |
| `practices-case-normalization.yml` (P2-72+P3-138 **스케줄된** 짝) | `.github/workflows/practices-case-normalization.yml` | 매주 월 08:00 UTC 크론 + `workflow_dispatch` — `ubuntu-latest`에서 **먼저 파일시스템 능력 프로브**(`A`/`a` 두 inode, NFC `café`/NFD `cafe`+U+0301 두 inode)를 돌리고, 통과할 때만 `run-all-guards.sh --include-fixtures`를 돌려 pass/fail 수와 `FAIL [` 라인을 job summary에 싣는다 | **advisory** (`continue-on-error: true`) — 절대 머지를 막지 않음. 프로브 실패 시 스윕을 **돌리지 않고** "PREMISE NOT ESTABLISHED — no measurement was taken"이라고 적는다(무의미한 pass 금지) | GitHub Actions에 스케줄됨. ext4/overlayfs가 대소문자-민감 **+ 바이트 보존**이라 macOS 스크립트가 못 덮는 **정규화 반쪽까지** 덮는다 |

### 핵심 설명

- **가드 파일(`*_guard.sh`)은 109개다** (practices/evals 107 + practices-react/evals 2 — `doc_headline_count_guard`가 이 수를 헤드라인과 대조해 강제; 정확한 수는 항상 `ls practices/evals/*_guard.sh practices-react/evals/*_guard.sh | wc -l`의 disk truth를 따른다). `run-all-guards.sh`는 R25 완료 선언 시 수동 호출된다(커밋마다 자동 실행되지 않는다). 일부 가드는 추가 진입점을 갖는다 — pre-commit 4 hard gates(`spec_ref`·`substance`·`evidence`·`time_decay`)는 practices/ 변경 커밋에서, `completion_checklist_recency`는 pre-push에서 돈다.
  R25 완료 선언 전에 `verify-completion.sh`를 실행하면 이 guard들이 모두 돌아간다.
- **pre-commit / pre-push hook은 opt-in이다.** `install-hooks.sh`를 실행한 클론에서만 활성화된다.
  ax-template 자체 HEAD에서는 활성화되어 있다; fork-receiver가 활성화 여부를 결정한다.
- **fork-receiver CI 통합은 완전 자율이다.** ax-template은 catalog quality probe만 제공한다.
  merge gate, branch protection, PR 정책은 fork-receiver가 결정한다.
- **commit-blocking은 `practices/` 변경에만 적용된다.** 일반 소스 변경에는 pre-commit gate가 실행되지 않는다.
- **`ax-case-sensitive-sweep.sh`는 "게이트가 자기 파일시스템 위에서 참인가"를 묻는 주기 작업이다** (P2-72).
  이 맥의 기본 APFS는 대소문자를 **접기** 때문에, 커밋된 경로 문자열이 git이 기록한 스펠링과 달라도 실행이
  성공해 R25가 **false-green**을 낼 수 있다(2026-08-01 P1-B에서 실측). 임의 파일의 어느 substring이 경로인지는
  inspection으로 결정불가능하므로 정규식 스캐너로는 계열이 닫히지 않는다 — 유일한 완전 처방이 **비-aliasing
  파일시스템에서 스위트를 실제로 돌리는 것**이고, 이 스크립트가 그 실행을 재현 가능하게 만든다.
  ```bash
  bash practices/scripts/ax-case-sensitive-sweep.sh            # HEAD를 쓸어담는다 (~16분)
  bash practices/scripts/ax-case-sensitive-sweep.sh --rev <sha>
  ```
  **커버 안 하는 것을 스스로 출력한다**: gradle 스텝(JDK 미프로비저닝) · npm 스텝(node_modules 미프로비저닝) ·
  R25 전체 · 유니코드 정규화(이 볼륨은 NFC/NFD를 접는다 — 측정됨). 볼륨은 EXIT trap에서 detach되고
  `hdiutil info`로 잔류 없음을 **검증**한다(잔류 시 exit 5). 이것은 **로컬/수동** 절반이다 — 호출은 사람 몫이고,
  정규화 반쪽은 원리적으로 못 덮는다.
- **`practices-case-normalization.yml`이 그 스케줄된 짝이다** (P2-72 운영 잔여 + P3-138 능력 잔여).
  `.github/workflows/`에 이미 도는 크론들(`practices-drift` 주간 · `practices-portability` 주간 advisory ·
  `practices-chub-feedback` 월간)과 같은 모양으로, **주간 `ubuntu-latest` advisory 크론 + `workflow_dispatch`**다.
  자율성 경계는 **fork-receiver에게 게이트를 강제하지 않는다**는 뜻이지 우리 프로브를 스케줄하지 않는다는
  뜻이 아니고, advisory 잡은 구조적으로 누구에게도 아무것도 강제하지 않는다. Linux 러너를 고른 이유는
  ext4/overlayfs가 대소문자-민감 **+ 바이트 보존**이라 `hdiutil`이 한쪽만 주는 자리에서 **두 반쪽을 공짜로**
  주기 때문이다. 순서가 핵심이다 — **전제를 먼저 측정한다**: `A`/`a`와 NFC `café`/NFD `cafe`+U+0301을 만들어
  각각 **다른 inode 2개**임을 단언하고, 하나라도 실패하면 스윕을 **아예 돌리지 않고** "PREMISE NOT ESTABLISHED"를
  summary에 적는다(접는 파일시스템 위의 pass는 무측정보다 나쁘다). 프로브는 자기가 `git status --porcelain`을
  더럽히지 않았음도 단언한다(스위트 자신이 그것을 검사하므로).
  **커버 안 하는 것을 job summary에 스스로 출력한다**: gradle 스텝 · npm 스텝 · R25 전체(러너에 JDK·node_modules를
  일부러 프로비저닝하지 않는다 — 첫 실행의 신호를 흐리기 때문).
  **정직한 상태**: 이 트리의 가드가 Linux에서 청결한지는 **여전히 미측정**이다. 메커니즘은 출하·스케줄됐고,
  **측정치는 첫 스케줄/디스패치 실행이 만든다**. 주기 작업 전체 목록과 근거는 **`practices/MAINTAINER.md` §5d**.

### Surface별 binary 테스트 커버리지 (P2-3)

각 강제 표면은 "실제로 차단하는가"를 증명하는 binary 테스트로 backstop된다 — vacuous(항상-통과)
enforcement을 막기 위한 falsification 증명. (적대적 감사 thesis: gate는 **non-vacuously** 차단해야 한다.)

| Surface | binary 테스트 커버리지 |
|---------|----------------------|
| PreToolUse hook | **by-construction 예외** — Claude Code 세션 hook이라 shell에서 호출 불가. 단, 이 hook이 트리거하는 게이트(pre-commit의 4 guard)는 아래에서 falsification-proven. |
| pre-commit (4 gate) | 주력 게이트 evidence_guard에 falsification 증명 `practices/scripts/ax-prove-evidence-gate-blocks-agent.sh` (agent가 placeholder/빈 url evidence 작성→BLOCK→실제 출처 anchor→PASS, actor=agent 기록). `agent_block_proof_guard.sh`[76]가 존재·toggle·non-vacuity backstop. |
| pre-push (recency) | `completion_checklist_recency_guard.sh --fixtures` (pass_*/fail_* — HEAD 최신 audit면 통과, stale/없으면 차단). |
| run-all-guards | falsification 증명 `practices/scripts/ax-prove-gate-blocks-agent.sh` (agent가 Map-반환 @ExceptionHandler→BLOCK→ProblemDetail→PASS). [76]가 backstop. 추가로 모든 guard가 `--include-fixtures`로 pass/fail fixture 쌍 실행. shell guard fail fixture의 non-vacuity는 [87](`fixture_kill_proof_guard`)이 기계 검증 — guard의 특정 탐지 로직을 anchor neuter로 무력화했을 때 fixture exit이 1→0으로 flip됨을 mutation으로 확인. |
| per-domain test{Domain} | 각 task 자체가 binary pass/fail. 모든 도메인이 ViolationProofTest를 동봉 — 위반이 구조적으로 불가능함을 단언(by-construction falsification). |

→ shell-testable 차단 표면(pre-commit · pre-push · run-all-guards · per-domain)은 전부 binary/falsification
커버리지 보유. PreToolUse만 session-bound이라 by-construction 예외(gap 아님 — 트리거하는 게이트는 proven).

*(P2-3 — **closed 2026-06-24**: enforcement-surface 분류 문서화 + 위 surface별 binary 테스트 커버리지 map.
shell-testable 표면 전부 커버, 세션 hook은 구조적 예외로 honest 명시.)*

## RBAC (reference workload)

| 역할 | 접근 범위 |
|------|----------|
| ADMIN | `/api/admin/**` + 모든 인증 엔드포인트 |
| MANAGER | 모든 인증 엔드포인트 |
| MEMBER | 모든 인증 엔드포인트 |

JWT에 `role` claim. SecurityConfig에서 `hasAuthority("ROLE_ADMIN")` 검사.
