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

---

## ⭐ Project Vision (READ FIRST — 절대 잊지 말 것)

**ax-template은 React (front) + Spring Boot (backend) full-stack 개발의
composition kit이다.** 각 component (Spec Trio · practices · practices-react ·
reference workloads · ESLint plugin · testPractices · AGENTS.md · 4 hard gates ·
/ax-transform skill) 를 **조합**해서 새 프로젝트를 시작하는 **fork-base
template**. 모든 layer에서 **규칙을 기계적으로 강제하는 선 순환 시스템**.

```
fork ax-template
    ↓ (25 L4 domains + 11 active recipes + 144 Java rules + 86 React rules + 7 ESLint rules + 70 hard guards + AGENTS.md sentinel)
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

→ 한 줄: **Korean enterprise standard stack (React + Spring Boot) 위에 AI agent가
규칙 안에서만 동작하는 코드를 짤 수 있게 하는 composition kit + 자체 강화
catalog 시스템.**

상세 내용은 README.md (composition-kit framing 정식 문서) 참조.

---

## Project Identity

**ax = AI transformation.** 이 repo는 Claude Code skill **`/ax-transform`** 의 source. AI agent가 새 프로젝트를 부트스트랩하거나 기존 프로젝트를 AI 친화적으로 전환할 때 활성화하는 skill 전체 패키지.

핵심 가치는 "코드"가 아니라 **AI agent가 빠르게 이해하고 안전하게 작업할 수 있는 인프라**. 인증/CRUD 구현은 skill이 자신을 자신에게 적용한 reference workload — skill의 동작 시연.

**Skill 진입점**: `skills/ax-transform/SKILL.md` (frontmatter `name: ax-transform`). Plugin manifest는 `.claude-plugin/plugin.json`. Claude Code plugin marketplace에 등록 시 `/plugin install ax-transform@<marketplace>` 로 설치 가능.

### 이 skill이 제공하는 것

1. **Spec Trio** — `specs/` + `contracts/` + `blueprints/`. AI가 코드보다 spec을 먼저 읽도록 강제하는 contract-first 구조. AI 환각 차단의 1차 방어선.
2. **practices/ catalog** — Java/Spring best-practices 144룰 + practices-react 86룰 + ESLint 7룰. evidence-anchored (외부 URL/quote 필수)라 AI가 임의로 룰을 발명하지 못함.
3. **Verification feedback loop** — `./gradlew test{Domain}` 단일 명령으로 binary pass/fail. AI가 자기 결과를 self-verify 가능.
4. **AGENTS.md sentinel** — AI agent가 진입 시 즉시 컨텍스트 받음. sha256 anchoring으로 catalog와 동기화 보장.
5. **4 hard gates** — spec_ref / substance / time_decay / evidence. AI 결과물이 외부 사실에 anchor 안 되면 통과 불가.

### 이 skill이 강제하지 않는 것 (fork받은 팀이 결정)

- **Git workflow** — branch protection, PR, force-push 정책. main 직접 commit, trunk-based, GitFlow 모두 가능
- **Deployment / release** — 어떻게 배포하든 catalog 품질과 무관
- **Code review** — 1인 maintainer, 팀 review, AI review 어떤 방식이든 OK
- **CI 정책** — sentinel CI는 catalog quality probe로만 제공. merge gate 여부는 fork받는 팀이 결정
- **언어/프레임워크 확장** — Java/Spring 카탈로그(144 rules) + React/Next.js 카탈로그(86 rules + 7 ESLint rules) 둘 다 active. 다른 stack(Kotlin/Go/Python 등) 추가는 동일 패턴 (spec → rule → evidence → test) 따라 확장.

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
- `./gradlew testPractices` — 144 practices rules
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

### "단일 명령 binary pass/fail" — 정확한 의미

catalog claim **"`./gradlew test{Domain}` — 단일 명령 binary pass/fail"** 은
**per-domain task** 에 적용된다. **R22 baseline (2026-05-21)**: per-domain
task 전 GREEN + aggregate `./gradlew test` 는 advisory PortabilityCyclic
한 건을 제외하면 GREEN. 도메인별 상태:

| Per-domain task                | 상태       | 비고 |
|--------------------------------|----------|---|
| `./gradlew testCrud`           | GREEN    | 7/7 PASS |
| `./gradlew testAsvs`           | GREEN    | 26 ASVS items PASS |
| `./gradlew testPractices`      | GREEN    | 144 rules PASS |
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
| `./gradlew testPortability`    | advisory | 외부 fixture (spring-realworld-example-app) 에 cycle 있음. fork-receiver의 코드가 아니라 외부 reference 코드의 결함 |

전체 `./gradlew test` aggregate 도 **PortabilityCyclic advisory 1건을 제외하면
GREEN.** R20–R21 에서 표면화됐던 aggregate-only failure (FeatureFlagFlowIT
11건 spurious 401) 의 root cause 는 Spring TestContext `ContextCache` 의 기본
용량 32 한계였다 — 전체 71개 `@SpringBootTest` 클래스가 LRU 회전을 일으켜
`auth.signup.auto-verify=true` 라는 동일한 properties cache key 를 공유하던
BillingFlowIT context 가 후속 IT 들 사이에서 evict 되었고, FF 가 stale
cache miss 로 재진입할 때 `@LocalServerPort` 의 port 값이 죽은 Tomcat 의
포트를 가리키게 되어 모든 요청이 401 로 응답되었다. **R22 fix**:
BillingFlowIT + FeatureFlagFlowIT 두 클래스에 `@DirtiesContext(BEFORE_CLASS)`
를 적용해 매 클래스 시작 전 fresh context boot 을 강제 — 가장 surgical 한
선택. catalog scope 안 의 root-cause closure 이고 fork-receiver 가 자신의 CI
에서 강제할 의무가 없는 ApplicationContext 캐시 정책에는 손대지 않는다.

```bash
# Backend
cd backend && ./gradlew build         # 빌드
cd backend && ./gradlew test          # 전체 — 위 매트릭스의 aggregate
cd backend && ./gradlew testAsvs      # auth ASVS 검증 (GREEN)
cd backend && ./gradlew testCrud      # CRUD spec 검증 (GREEN)
cd backend && ./gradlew testPractices # practices/ 144룰 검증 (GREEN)
cd backend && ./gradlew testPortability  # advisory: 외부 fixture에 룰 적용

# Frontend
cd frontend && npm run build
cd frontend && npm run test
```

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
│   ├── crud-l0.yaml
│   ├── spring-practices-l0.yaml
│   └── portable-test-template/
├── contracts/                 # OpenAPI 계약 (핵심)
│   └── auth-openapi.yaml
├── blueprints/                # 정책 매니페스트 (핵심)
│   └── auth-manifest.yaml
├── practices/                 # AI-targeted catalog (skill 핵심 자산)
│   ├── rules/                 # 144룰, 22+ categories (R50/R58/R61 추가분 포함)
│   ├── upstream/              # 외부 사실 snapshot
│   ├── evals/                 # 4 hard gates + 70 hard guards
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
| CRUD | `specs/crud-l0.yaml` | 5 (CRUD-001~005) | 7 security tests |
| Practices | `specs/spring-practices-l0.yaml` | — | 144 rules / 22 categories |

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

## RBAC (reference workload)

| 역할 | 접근 범위 |
|------|----------|
| ADMIN | `/api/admin/**` + 모든 인증 엔드포인트 |
| MANAGER | 모든 인증 엔드포인트 |
| MEMBER | 모든 인증 엔드포인트 |

JWT에 `role` claim. SecurityConfig에서 `hasAuthority("ROLE_ADMIN")` 검사.
