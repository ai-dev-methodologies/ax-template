# practices/ Maintainer Guide

> This guide covers the **maintainer-side** PR cycle for the `practices/` directory.
> Developer-side enforcement (IDE integration, Claude Code hooks, PR build gates) is a
> separate phase and is explicitly out of scope here.

---

## 1. Role

The practices maintainer owns the `practices/rules/` catalog as a **view layer of Spec Trio**,
not a 4th canonical artifact tier.

Responsibilities:

- Review and merge rule PRs according to the 5-step PR cycle below.
- Ensure every merged rule is anchored to a `spec_ref` in `specs/*.yaml`.
- Run `./gradlew evalPractices` to produce the advisory dashboard report.
- Monitor the advisory balance metric and act on WARN signals (never block on them).
- Decide when a sub-category accumulates enough items to warrant advisory spec domain promotion.

The maintainer does **not** enforce style, opinion, or architecture stance beyond what is
explicitly captured in the spec items and hard gates.

---

## 2. PR Cycle (5 Steps)

Every rule PR must follow this sequence before merge:

### Step 1 — Add or reuse a spec item

Add a new item to an existing `specs/*.yaml` domain (or create a new one if justified),
or reuse an existing item. The item must have:
- a unique `id` in `{DOMAIN}-{ITEM-ID}` format
- a concrete `requirement` and `test_method`
- a `notes` field describing the expected test behavior

The spec item is the source of truth. The rule `.md` is a human-readable projection of it.

### Step 2 — Add a `@Tag("PRACTICES")` + `@Tag("PRACTICES-<ID>")` test

Write a RestAssured black-box test in the reference implementation:

```java
@Test
@Tag("PRACTICES")
@Tag("PRACTICES-PERS-001")
void persistence_nPlusOneQueryDetected() {
    // black-box HTTP assertion — no MockMvc, no @WithMockUser
}
```

Run `./gradlew testPractices` to confirm the test passes (GREEN).

### Step 3 — Add the rule `.md` with `spec_ref` in frontmatter

Create `practices/rules/{category-slug}.md`. The frontmatter **must** include:

```yaml
---
spec_ref: "#PERS-001"          # required — links to specs/*.yaml item id
upstream_version: "spring-boot: 3.x"
category: "persistence-"
---
```

`evals/spec_ref_guard.sh` will reject any rule missing `spec_ref` — this is a hard gate.

### Step 4 — Run `./gradlew testPractices` and confirm PASS

This is the **only hard gate** for merge. All tests tagged `PRACTICES` must exit 0.
No rule may be merged if this command fails.

### Step 5 — Review the advisory report (informational only)

Run `./gradlew evalPractices` and read `evals/reports/$(date +%F).md`. The 5-axis advisory
scores (Detection / Outcome / Reference / Portability / Drift) are advisory only — a low
score is a signal to improve the rule, not a merge blocker. Check the balance WARN if present.

---

## 3. Advisory vs Hard Gate Distinction

| Type | What it is | Examples | Merge-blocking? |
|------|------------|----------|-----------------|
| **Hard gate** | Binary, falsifiable, external fact | `spec_ref_present`, `gradle_test_pass` | **Yes** |
| **Advisory only** | Weighted score, human judgment, trend signal | 5-axis rubric scores, balance metric | **No** |

### Hard gates (2 total)

1. `spec_ref_present` — every rule `.md` must declare a `spec_ref` in frontmatter pointing to a
   real item in `specs/*.yaml`. Enforced by `evals/spec_ref_guard.sh` (exits ≠ 0 if missing).
2. `gradle_test_pass` — `./gradlew test{Domain}` must exit 0. This is the single binary truth.

### Advisory only metrics (never block)

- The 5-axis rubric in `rubric.yaml` (Detection, Outcome, Reference, Portability, Drift) produces
  a composite score that is printed in `report.md` for maintainer awareness only.
- The 22-category balance guard (`_balance_guard.sh`) emits WARN when one category exceeds 25%
  of total rules. It always exits 0 — it is advisory only.
- Rubric axis scores are advisory only because weighted composites are not falsifiable: a rule
  can score well on Detection while being stale on Drift. Only the binary Gradle test result
  is an externally verifiable fact anchored to Spec Trio.

---

## 4. The 21-Category Catalog (Advisory)

The `advisory_metrics.balance.categories` list in `rubric.yaml` defines 21 Spring Boot concern
prefixes. This catalog is **advisory** — it guides rule naming and distribution visibility,
but it is **not a normative axiom**.

Purpose:
- Rule authors use category prefixes to name their files (e.g., `persistence-no-n-plus-1.md`).
- The balance guard watches for category concentration and emits WARN (never blocks) when one
  prefix exceeds 25% of total rules, guarding against security-bias regression.
- The catalog will be re-evaluated after 100 rules to check whether usage matches the categories.

The catalog may evolve (add, rename, or retire categories) without touching spec files, tests,
or build configuration. It is a human navigation aid, not a spec tier.

### Catalog history

| Date | Change | Reason |
|------|--------|--------|
| 2026-05-15 | Initial 22-category catalog (ralplan ADR) | Cover the Spring Boot surface broadly enough to detect security-only bias |
| 2026-05-16 | `native-` removed (22 → **21**) | P2-D D3 REJECTED — GraalVM Native Image is a niche deployment target; mandating it forces GraalVM SDK into the maintainer build (build time 30s → 5–10 min, destroying the fast-feedback property). See `DECISIONS.md`. |

### Closed-at-current-size categories

Some categories are explicitly **closed** — no new rules accepted without re-opening:

| Category | Current size | Reason for closure |
|----------|-------------|---------------------|
| `arch-` | 3 rules (cyclic / layer / jpa) | P2-D D2 REJECTED new style-specific rules (Hexagonal, Modulith). The 3 shipped rules are universal under any architecture; further style-specific rules would impose taste. See `DECISIONS.md`. |

Closed status is recorded in `DECISIONS.md` with a re-evaluation trigger. Submitting a new
rule into a closed category requires citing the re-evaluation trigger first.

---

## 5. Spec Domain Promotion Criterion

A sub-category may be promoted from the seed domain (`specs/spring-practices-l0.yaml`) to its
own dedicated spec domain (e.g., `specs/spring-persistence-l0.yaml`) when:

- **≥ 3 spec items** in the same sub-category have been merged and all tests pass, **and**
- The maintainer judges that the sub-category has stable, coherent semantics worth isolating.

### Rules for spec promotion

- Spec promotion is **advisory only** — no automation forces it.
- Forced promotion is explicitly **forbidden**: a sub-category with exactly 3 items is eligible
  but not required to be promoted.
- A new spec domain requires a new Gradle task (`testPractices{SubDomain}`) that includes the
  promoted tags.
- After promotion, the original `spring-practices-l0.yaml` items may be deprecated (marked
  `applicable: false`) but must not be deleted until all references are migrated.

### Why ≥ 3 items?

A single-item shim spec domain adds maintenance cost with no benefit. Two items may be
coincidental. Three items with coherent semantics indicate a real, stable sub-domain.
This threshold prevents the 1-item shim proliferation risk identified in the plan ADR.

---

## 5b. Rule Provenance Policy (added 2026-05-15)

Every rule in `practices/rules/*.md` must be **traceable to an external source** — not
Claude's prior training data, not maintainer intuition. The provenance trail is what lets
future maintainers confidently improve, change, or retire a rule.

### What counts as evidence

Each rule's frontmatter MUST have an `evidence:` field with at least one entry. An entry
is one of two shapes:

```yaml
# Shape 1 — anchored to a fetched upstream snapshot
evidence:
  - upstream_id: spring-boot-3.5         # id from practices/upstream/_MANIFEST.yaml
    section: "Constructor Injection"
    quote: "Constructor injection is the recommended approach…"

# Shape 2 — external citation (RFC / JEP / vendor docs / peer-reviewed paper)
evidence:
  - source_type: external
    citation: "Spring Framework Reference §1.4.1 — Constructor-based Dependency Injection"
    url: "https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-constructor-injection"
```

Both shapes may coexist on the same rule when multiple sources reinforce the rule.

### What does NOT count

- "Claude said so", "I remember reading", "everyone does it" — opinion, not evidence.
- Random blog posts and stack-overflow answers — not authoritative unless they are the
  cited source of a downstream standard.
- Tribal knowledge — record in `DECISIONS.md` as rationale, but it cannot be the
  *evidence* anchor.

### Hard gate

`practices/evals/evidence_guard.sh` is binary:

- empty `evidence` → BLOCK
- `upstream_id` not in `_MANIFEST.yaml` → BLOCK
- `source_type: external` missing `citation` or `url` → BLOCK
- evidence entry still contains the `_template.md` placeholder string → BLOCK

The guard runs in `.github/workflows/practices-sentinel.yml` on every PR / push.

### Advisory pre-flight (R85b)

`practices/scripts/verify-rule-evidence-quotes.sh` is a **maintainer-run advisory tool**
that goes one layer deeper than `evidence_guard`: it fetches each cited URL and verifies
that the verbatim `'...'` quote inside the citation actually appears at the URL.

Motivation: R85's iter1 commit fabricated two evidence quotes (Fowler + NIST). The codex
critic caught them, but only because a separate AI reviewer happened to run. The
catalog's `evidence_guard` checks shape (URL + citation present), not content. R85b
closes the content gap when invoked.

This is NOT a hard guard — running WebFetch in CI is fragile (rate limits, transient 5xx,
network isolation, JS-rendered pages, paywalled archives). Maintainers run it locally as
a pre-flight before committing a new rule or approving a quote-edit.

```sh
# Single rule
bash practices/scripts/verify-rule-evidence-quotes.sh \
  practices/rules/dogfood-finding-must-have-expiry-trigger.md

# Whole catalog
bash practices/scripts/verify-rule-evidence-quotes.sh --catalog practices

# Both catalogs
bash practices/scripts/verify-rule-evidence-quotes.sh --all
```

Output status:
- `VERIFIED` — every `source_type: external` evidence URL was fetched and the longest
  `'...'` quote inside the citation was found in the page text.
- `UNVERIFIED` — fetch succeeded but the quote was not found. The page may be JS-rendered,
  paywalled, or behind a redirect chain; manual inspection of the URL is required before
  treating the citation as auditable.
- `WARN` — fetch failed (network, 404, no quote candidate of sufficient length in the
  citation prose).

`upstream_id` evidence entries are skipped (already covered by `evidence_guard`'s
snapshot check). The tool's exit code is 1 on any `UNVERIFIED`, 0 otherwise — but the
exit is advisory only and MUST NOT be wired into pre-commit / pre-push.

### Trail (DECISIONS.md)

Every accepted rule gets one entry in `practices/DECISIONS.md` with:

1. **Date / maintainer** — who decided when
2. **Rationale** — *why this passes provenance* (not just "it works")
3. **Alternatives considered** — at least one rejected, with reason
4. **Re-evaluation trigger** — what would force a revisit

Rejected rule candidates (P2-D class) also get a DECISIONS.md entry under "REJECTED /
DEFERRED", so the same debate is not relitigated in six months.

### Catalog-quality enforcement is mechanical (no human-process gating)

The 4 binary hard gates run unconditionally when their inputs change — they require no
"signed enforcement decision" because they have no human-process surface. Local hooks
are opt-in per clone via `bash practices/scripts/install-hooks.sh`; once installed,
they fail-close on catalog quality without further configuration.

Git-workflow / branch protection / PR-required policy is **out of skill scope** and is
fork-받은 팀이 정함. See `DECISIONS-P3.md` for the scope-corrected enforcement table.

### ~~Snapshot limitation~~ — RESOLVED 2026-05-15 (see `DECISIONS.md`)

Historical note: an early concern held that the `practices/upstream/` snapshots were
fetched from `htmlsingle/index.html` navigation landing pages whose topic keywords
(`JOIN FETCH`, `constructor injection`, …) did not match, forcing rules onto the
`source_type: external` shape instead of `upstream_id`. **That was resolved 2026-05-15.**
`practices/upstream/` now holds 61 deep-reference `*.snapshot.md` files, and of the 112
rules, 70 carry the `upstream_id` evidence shape (resolving against a real snapshot +
section + quoted substring) — enforced binary by `evidence_guard.sh`. The two filenames
referenced in the original note (`spring-boot-3.5.snapshot.md`,
`spring-security-6.x.snapshot.md`) never existed on disk. See `DECISIONS.md` §"Snapshot
bodies are navigation indexes — RESOLVED" for the recorded resolution.

---

## 5c. Enforcement Layers (catalog quality only — scope-corrected 2026-05-16)

ax-template is a `/ax-transform` skill package. The skill enforces **catalog quality**, not human collaboration policy. The four binary gates (`spec_ref` / `substance` / `time_decay` / `evidence`) plus AGENTS.md sentinel + testPractices fail-close on the following catalog-touching surfaces:

| Surface | Trigger | File |
|---------|---------|------|
| 1. Claude Code PreToolUse | Write/Edit/MultiEdit on `practices/rules/*.md`, the seed spec, or `backend/src/.../practices/*` | `.claude/settings.local.json` |
| 2a. Git pre-commit, stage 0 | commit touches `practices/rules/*.md` or `_template.md` | `.githooks/pre-commit` regenerates AGENTS.md + auto-stages it |
| 2b. Git pre-commit, stage 1 | every commit touching practices/ or the seed spec | 4 binary guards in `.githooks/pre-commit` |
| 2c. Git pre-commit, stage 2 | commit touches `backend/src/{main,test}/java/.../practices/` | `./gradlew testPractices` runs inside `.githooks/pre-commit` |
| 3. Git pre-push | local commits ahead of remote touch backend/, practices/, or seed spec | full regression `./gradlew testPractices testAsvs testCrud` in `.githooks/pre-push` |
| 4. GitHub Actions (PR + main push) | `.github/workflows/practices-sentinel.yml` runs the same gates | advisory probe at the source repo; fork-받은 팀이 본인 CI에 채택할지 결정 |

### Not in this table (removed 2026-05-16)

- **Branch protection on main** — fork-받은 팀의 repo settings 영역. ax-template은 강제 안 함.
- **PR-required workflow / `[break-glass]:` title convention** — fork-받은 팀의 정책. 어떤 git workflow를 채택하든 catalog quality gates는 동일하게 작동.
- **`enforce_admins=true`, `required_linear_history`, force-push 금지** — fork-받은 팀이 본인 정책으로 설정.

skill을 채택한 팀이 자신의 정책으로 위 항목을 적용하든 안 하든 catalog 신뢰도는 영향받지 않음. catalog 게이트는 mechanical / binary / external-fact-anchored.

### Why hooks are opt-in (`install-hooks.sh`)

`.githooks/`는 클론마다 사용자가 명시적으로 `bash practices/scripts/install-hooks.sh`로 활성화해야 적용됨. 강제 설치 안 함 — fork-받은 팀이 본인 workflow에 통합하든, sentinel CI만 의존하든, hooks 없이 가든 자율. 다만 hooks를 활성화하면 commit/push 시 catalog quality gates가 fail-close — 이 동작은 binary.

### Rule request channel

새 룰 요청은 `.github/ISSUE_TEMPLATE/practices-rule-request.yml` issue template로. evidence URL 필수, maintainer review 후 PR 진행. 이건 fork-받은 팀의 PR 정책과는 별개 — 이 repo (ax-template skill source)에 룰 추가 시 사용.

### Rule request channel for non-maintainers

Developers who need a new rule but cannot draft one without help should open the
**Practices — New Rule Request** GitHub issue
(`.github/ISSUE_TEMPLATE/practices-rule-request.yml`). The template enforces the same
substance bar as the rule files themselves: external evidence URL is required,
incorrect/correct examples must be ≥ 2 substantive Java lines, alternatives must be
listed. The maintainer reviews, runs `practices/upstream/fetch.sh` against the cited
URL, and either schedules a PR or files a DECISIONS.md DEFER entry — same provenance
contract as any other rule.

---

## 5d. Periodic jobs — nobody schedules these, you do (added 2026-08-01)

Two checks in this repo are **invocable but unscheduled**. No hook, no CI workflow and no R25
step calls them, so they run exactly as often as a maintainer runs them. The natural moment is
**once before a release**, and that is the only cadence this section prescribes.

| Job | Command | Cost | What it answers |
|---|---|---|---|
| upstream URL spot audit | `bash practices/scripts/external_url_spot_audit.sh` | network, minutes | do `source_type: external` citation URLs still resolve, and does the page still carry the id the citation claims? Three buckets: OK / SUSPICIOUS / UNREACHABLE. |
| non-aliasing filesystem sweep | `bash practices/scripts/ax-case-sensitive-sweep.sh` | macOS only (`hdiutil`), ~16 min | does the guard suite still pass when the filesystem **does not fold case**? A committed path string can name a file by a spelling git never recorded, and the default case-insensitive APFS every macOS checkout lives on serves it anyway — the command runs, the tree is clean, R25 reports GREEN on evidence it never produced. |

**What the sweep does NOT cover, and it prints this itself at the end of every run:** R25's
gradle steps (no JDK is provisioned on the volume), R25's npm step (no `node_modules`), R25 as a
whole, and **unicode normalization** — the volume `hdiutil` creates folds NFC/NFD, measured, so
only the case half of the aliasing family is swept. Uncommitted work is also out of scope: the
sweep clones a **committed revision**.

Both jobs fail loudly rather than skipping: a missing `hdiutil`, a volume that turns out to
alias, or a leaked attachment is a distinct non-zero exit with its reason printed.

**Why this is a section and not a cron.** It is tempting to read ax-template's autonomy
boundary — *"Fork받은 팀의 정책을 skill이 강제 ❌ … catalog 품질을 넘는 CI gate"* — as saying
this project does not schedule things. It does not say that, and the repo does not behave that
way: `.github/workflows/` carries three scheduled jobs today (`practices-drift` weekly,
`practices-portability` weekly and advisory, `practices-chub-feedback` monthly) plus a
push/PR-triggered `practices-sentinel`. The boundary is about **not imposing gates on
fork-receivers**, not about refusing to schedule our own probes. So "nothing schedules the
sweep" is a genuine remainder (BACKLOG P2-72), not a design decision — and the cheapest closure
is not a macOS cron but a **Linux runner**, whose filesystem is case-sensitive *and*
byte-preserving by default, i.e. gets both halves for free where `hdiutil` gets only one. That
job has not been written, and the claim that the guard suite is clean on Linux has **not been
measured** — every guard in this tree has only ever run on macOS.

---

## 6. Anti-Pattern: Governance Loop

> **Citation from `CLAUDE.md` — Anti-Patterns (거버넌스 무한루프 금지):**
>
> *"과거 실패: 30+ 문서와 18 세션을 소비했지만 코드 0줄.*
> *원인: draft→curated→stable 승격 게이트가 데드락을 만듦.*
>
> *금지 사항:*
> *- `TEMPLATE-GOVERNANCE.md` 같은 승격 절차 문서 생성 금지*
> *- 'curated promotion check' 같은 게이트 프로세스 금지*
> *- 'evidence bundle' 같은 검증-위한-검증 문서 금지*
> *- 구현 없이 문서만 생산하는 계획 수립 금지*
>
> *대신: 코드를 먼저 쓰고, `./gradlew test{Domain}`으로 검증하고, 부족하면 spec을 보강한다."*

### How `practices/` avoids this anti-pattern

| Governance-loop symptom | How this system prevents it |
|---|---|
| Promotion gates requiring evidence bundles | Spec promotion is advisory only — no evidence bundle required |
| Draft → curated → stable tiers | No tiers; a rule is either merged (hard gate passes) or not |
| Verification-for-verification documents | Advisory rubric reports are outputs, never inputs to a hard gate |
| Planning documents without code | Every rule PR requires a `@Tag` test and a `spec_ref` before touching `rules/` |

The Spec Trio binary — `./gradlew testPractices` exits 0 — is the only hard gate.
Everything else is advisory only.

## Shared utility promotion (R67 lesson)

R67 (2026-05-26) lifted `EmailPiiHelper` from the `emailoutbox` package
to a new `common.AuditPiiHelper` after seven backend modules had
adopted it. R80 codified the rule that drove the lift:
[`practices/rules/promote-on-third-use.md`](./rules/promote-on-third-use.md).

### The 3-use threshold

When a utility helper is duplicated across 3+ modules, the third commit
MUST either:

1. **Lift to a shared package in the same commit** — create the new
   location, move all existing inline copies, delete the duplicates. No
   transition window where divergence is possible.
2. **Explicit deferral with expiry** — commit message records: "Helper X
   now in three modules; deferring lift to package common/Y because
   <reason>. Lift trigger: <fourth adoption | dated quarter | named
   owner>."

Silent deferral is permanent duplication. The rule of three exists to
catch helpers BEFORE they accumulate per-module drift.

### Canonical examples in the ax-template catalog

| Helper | Inline life | Lifted at | Now at |
|---|---|---|---|
| `useCallerId` (TS) | 7 L4 directories (R39-R51) | R53 | `templates/L0/fork-receiver-kit/use-caller-id.ts` |
| `parseError` (TS) | 7 L4 directories (R39-R51) | R53 | `templates/L0/fork-receiver-kit/parse-error.ts` |
| `assertSafeEntityRef` (TS) | favorites-bookmarks only | R53 (pre-emptive) | `templates/L0/fork-receiver-kit/entity-key.ts` |
| `EmailPiiHelper` (JVM) | emailoutbox-private R60 → 7 modules R62/R63/R65/R72 | R67 | `backend/.../common/AuditPiiHelper.java` |

The R53 lift was triggered by 7 simultaneous adopters (R51 email-outbox
forced the issue at module count 6 + 7). The R67 lift was deferred
TWICE (R62 + R63) before finally happening at module count 7. **Both
are above the rule-of-three threshold**, both took longer than they
should have. Future lifts should fire at module 3, not module 7.

### How to detect a missed lift

Run periodically:

```bash
# Frontend helpers (TS)
for helper in useCallerId parseError assertSafeEntityRef; do
  count=$(grep -rl "$helper" templates/L4 --include="*.tsx" --include="*.ts" 2>/dev/null | wc -l)
  if [ "$count" -ge 3 ] && ! grep -q "fork-receiver-kit" <(grep -r "$helper" templates/L4 --include="*.tsx" 2>/dev/null | head -1); then
    echo "LIFT CANDIDATE: $helper used in $count files but no fork-receiver-kit import found"
  fi
done

# Backend helpers (JVM)
for helper in AuditPiiHelper; do
  count=$(grep -rl "$helper" backend/src/main/java --include="*.java" 2>/dev/null | wc -l)
  if [ "$count" -ge 3 ] && ! grep -q "common\." <(grep -r "$helper" backend/src/main/java --include="*.java" 2>/dev/null | head -1); then
    echo "LIFT CANDIDATE: $helper used in $count files but not in common package"
  fi
done
```

The catalog does not (yet) ship a mechanical guard for this; the
discipline is enforced by maintainer review + the rule prose in R80.
A future guard could automate the scan.

### When to defer the lift legitimately

- Two of the three adopters use the helper with subtly different
  semantics. The lift would require choosing the canonical semantics;
  better to wait for the third adopter to inform the choice.
- The shared package location is contested (e.g. `common` vs
  `lib/utils` vs `domain-shared`). Lift after the architecture decision
  is recorded in `DECISIONS.md`.
- The fork-receiver impact is non-trivial (e.g. published library with
  semver) — N/A here because ax-template is source-of-truth catalog,
  not a published library.

In all deferral cases, **record the deferral in the commit message and
set a concrete expiry** (next-adopter trigger or dated quarter).
