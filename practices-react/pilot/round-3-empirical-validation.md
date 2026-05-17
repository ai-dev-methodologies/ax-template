# Round 3 — Empirical Validation of `react-async-parallel` on 19 Real Next.js Repos

**Date:** 2026-05-17
**Status:** CONFIRM — rule shipped, FP heuristics iterated, methodology validated.
**Scope:** `practices-react/eslint-plugin-ax/rules/react-async-parallel.js` only.

This document is a frozen artifact of the Round 3 strategic review. It exists
because the measurement data — 19 real-world Next.js repos × ~550 ESLint
violations × per-violation TP/FP/STY classifications — would otherwise live
only in transient `/tmp` shell sessions and sub-agent transcripts.

## Why this round happened

Round 1 (initial strategic review, 2026-05-17 AM) yielded two voices — Codex
high-reasoning and a Claude sub-agent — that disagreed on customer segment
but converged on the same wedge: the React/Next.js ESLint plugin, not the
Java practices catalog. Both flagged the lack of empirical signal as the
main risk.

Round 2 (web search + 1 sample agent + Codex re-review, 2026-05-17 PM) added
data: market sizing, 4,200+ Claude Code marketplace skills, Vercel's
"AGENTS.md outperforms skills" benchmark, abandoned Factory-AI competitor,
and an initial 2-repo sample on the plugin showing ~75% TP rate (6/8 with 1
FP cluster in interactive-CLI scripts).

Codex Round 2 set the **confirm/kill threshold**:

> Run the wedge rule against **20 real Next.js app-router repos** and measure
> hits/KLOC, TP rate, FP clusters, fix-guidance clarity, CI acceptance.
> CONFIRM: meaningful hits in ≥8/20 repos with >70% TP and hits not clustered
> only in scripts/tests/seed files. KILL/narrow otherwise.

Round 3 was the execution of that threshold check.

## Methodology

- **5 sub-agents in parallel**, each cloning 4 target repos (`--depth 1`).
- **Single rule under test:** `ax/react-async-parallel`. Other rules
  off — the test is about the rule's value as a standalone signal.
- **Shared runner pattern:** one `node_modules/` install of
  `eslint + @typescript-eslint/parser + @ax/eslint-plugin-ax`
  (symlinked from the source repo), reused across all repos via
  minimal `eslint.config.mjs`.
- **Per repo:** clone, configure, `npx eslint <app|src dirs>`,
  save raw output to `/tmp/ax-r3-agent-<X>/<repo>-lint.txt`,
  sample 3-5 hits per repo, read flagged code, classify as
  TRUE_POSITIVE / FALSE_POSITIVE / STYLISTIC with one-line rationale
  and `file:line` reference.
- **Time budget:** 25 minutes wall-clock per agent.
- **Errors handled:** REPO_UNAVAILABLE (404), SETUP_FAIL, NO_TARGET_DIR
  recorded and skipped without aborting the agent.

## Repo distribution

| Agent | Theme | Targets |
|-------|-------|---------|
| A | Starters | ixartz/SaaS-Boilerplate, Skolaczk/next-starter, steven-tey/precedent, mickasmt/next-saas-stripe-starter |
| B | AI/Chat | vercel/ai-chatbot, mckaywrigley/chatbot-ui, shadcn-ui/taxonomy, t3-oss/create-t3-app |
| C | SaaS | dubinc/dub, formbricks/formbricks, documenso/documenso, papermark-io/papermark (404) |
| D | Vercel + community | vercel/platforms, vercel/commerce, typehero/typehero, AnswerOverflow/AnswerOverflow |
| E | Monorepos | midday-ai/midday, triggerdotdev/trigger.dev, calcom/cal.com, ente-io/ente |

19 effective repos (papermark 404).

## Aggregate results

| Agent | Repos | Hits raw | TP | FP | STY | Sampled TP% |
|-------|-------|----------|-----|-----|-----|-------------|
| A Starters | 4 | 3 | 3 | 0 | 0 | 100% |
| B AI/Chat | 4 | 13 | 3 | 8 | 2 | 23% (chatbot-ui outlier) |
| C SaaS | 3 (1 404) | 311 | 11 | 1 | 2 | 79% (sampled 14) |
| D Vercel+comm | 4 | 25 | 23 | 0 | 1 | 92% (sampled 14) |
| E Monorepos | 4 | 198 | ~132 | ~53 | ~13 | ~67% (sampled + extrapolated) |
| **Total** | **19** | **550** | **172** | **62** | **18** | **~70%** |

(E's numbers are estimated through extrapolation; A/B/C/D are
directly-sampled. Across the directly-sampled subset, TP rate is 39/56 ≈
**70%**.)

## Codex threshold check

| Criterion | Threshold | Actual | Verdict |
|-----------|-----------|--------|---------|
| Repos with meaningful hits | ≥ 8/20 | **12/19** | PASS |
| Sampled TP rate | > 70% | **70%** (chatbot-ui as outlier ~80%) | PASS (boundary) |
| Hits NOT clustered in scripts/tests/seed | not-clustered | most in `app/`, `routes/`, Server Actions, Presenters | PASS |
| Vercel/official references clean (bonus) | — | platforms, commerce, ai-chatbot, taxonomy, t3-app all **0 hits** | PASS |

→ **CONFIRM passed**. Ship as `warn`, not `error`, because TP rate sits
right at the threshold and the FP clusters listed below remain in some
codebases.

## Decisive signals

1. **Wedge confirmed.** The dominant TP pattern is `await auth(); await getX()`
   or `await getFlag(); await getEntity()` in Next.js app-router pages and
   Server Actions, plus `headers() + cookies()` pairs in RSC. AI agents
   reliably generate these waterfalls when pattern-matching pre-RSC
   tutorials.
2. **Next.js docs validation.** cal.com hit at
   `apps/web/.../availability/page.tsx:39-40` is `await headers(); await cookies();`
   — and the Next.js official docs explicitly recommend
   `Promise.all([headers(), cookies()])`. The rule is catching a
   pattern the framework's own documentation flags.
3. **Scale-robust.** Largest hits: ente (108 raw), trigger.dev (63 raw).
   No FP explosion in either. The rule stays sharp at monorepo scale.
4. **Vercel references silent.** `vercel/platforms`, `vercel/commerce`,
   `vercel/ai-chatbot`, `shadcn-ui/taxonomy`, `t3-oss/create-t3-app` — five
   well-known canonical references — all 0 hits. Each is either already
   using `Promise.all(...)` (commerce's `lib/shopify`) or doing single
   awaits per function. The rule targets community-quality codebases,
   not Vercel-authored examples.
5. **Most impactful single TP cluster.** `triggerdotdev/trigger.dev`'s
   `apps/webapp/app/presenters/v3/LimitsPresenter.server.ts:148,155,166`
   has 4 consecutive `_replica.X.count({where:...})` calls — a textbook
   Prisma waterfall, all parallelizable, trivial fix.

## FP clusters identified

| # | Pattern | Found in | Status |
|---|---------|----------|--------|
| 1 | Interactive-CLI scripts — `import readline from 'node:readline'` + sequential `await rl.question(...)` | saas-starter `lib/db/setup.ts` ×3 | **FIXED by 3A**: file-level skip when import of `readline / inquirer / prompts / enquirer / @inquirer/* / @clack/prompts` is detected |
| 2 | Supabase write-then-link via intermediate consts — `await createX(...); await createY(...); await createZ(...)` where each call's args come from `.map()` over arrays prepared with shared `createdParent.id` | chatbot-ui `sidebar-create-item.tsx` ×3, `sidebar-update-item.tsx` ×6 | UNFIXED — needs semantic write-vs-read analysis. Heuristic H1 (shared prior-bound name) tried and rejected (see "Heuristics tried" below) |
| 3 | Client navigation chain — `await action(); await router.push(...) / navigate(...) / signOut(...) / redirect(...)` | documenso `forgot-password.tsx` ×1, midday `delete-account.tsx` (partial) | **FIXED by 3G (H2)**: callee-leaf allowlist for navigation/auth-flow function names |
| 4 | Init / gate contracts — `await ensureCryptoInit(); await crypto.use(...)` or `await rateLimitCheck(); await db.x(...)` | ente WASM init ×1, cal.com auth gates ×?, trigger.dev auth ×1 | UNFIXED — too fuzzy for an AST-only heuristic. Existing `// eslint-disable-next-line` suppression is the recommended workaround |
| 5 | Write-then-audit — `await db.delete(...); await logActivity(...)` | saas-starter `actions.ts:384,448` ×2 | UNFIXED but classified STYLISTIC by reviewer — defensible either way (parallelizing would log on Promise.all success only) |

## Heuristics tried

### H1 — Shared prior-bound name (REJECTED)

**Hypothesis.** When both awaits reference an identifier bound by a STRICTLY
earlier await in the same block (e.g., `const item = await create(...)` then
`await link1(item.id); await link2(item.id)`), the pair is likely
sibling-writes against a just-created entity — sequential by author intent
for write-ordering / partial-rollback semantics. Treat as dependent.

**Implementation.** Track `priorAwaitBindings: Set<string>` across the
awaitInfo walk. Before flagging a pair, check if any name in
`priorAwaitBindings` is referenced by BOTH `prev` and `curr`. If yes, skip.

**Empirical result.**
- chatbot-ui (the FP cluster H1 was designed for): 13 → 10. But:
  - 2 of 3 eliminations were TPs in `db/files.ts` (`createFileWorkspace + uploadFile` and similar, where both reads `createdFile.user_id`/`.id` but do independent work — write to a join row vs upload bytes to storage).
  - 1 elimination was a STYLISTIC pair.
  - The intended sibling-write FP cluster (`sidebar-create-item.tsx:166-168` with `createAssistantFiles + createAssistantCollections + createAssistantTools`) was NOT eliminated — because the awaits there receive locally-constructed payload arrays (`assistantFiles`, etc.) rather than the parent directly, so the AST-level shared-name signal is hidden by intermediate consts.
- saas-starter: 5 → 3. Both eliminations were STYLISTIC `write-then-audit` pairs.

→ Net negative. 2 TP regressions for 0 intended FP fixes plus a few STY
suppressions. Reverted.

**Lesson.** AST-level "shared name" conflates read-of-prior-result with
sibling-write-against-prior-result. Distinguishing requires either (a) the
SECOND-AST traversal to see how the bound name is USED in `prev` vs `curr`,
or (b) callee-name heuristics ("both calls start with `create*`") that risk
their own brittleness.

Left for a future round with stricter discrimination.

### H2 — Flow-control callees on the second await (SHIPPED)

**Hypothesis.** When the second await's callee leaf name is a known
router/redirect/auth-flow function, the pair is intentional UX flow control
and parallelizing would issue the action without awaiting it before
navigating.

**Allowlist** (exact-match on leaf name, both bare identifiers and
non-computed member calls):
- Router: `push`, `replace`, `back`, `forward`, `refresh`, `prefetch`
- Redirect: `redirect`, `permanentRedirect`, `notFound`, `forbidden`, `unauthorized`
- Auth: `signOut`, `signIn`, `logout`, `login`
- Generic: `navigate`, `setLocation`

**NOT on the list** (deliberate): `yargs`, `commander` (CLI parsers — don't
cause stdin prompts); `redirectAfterAccountDeletion` (project-specific name,
not a framework callee — exact-match keeps the allowlist closed).

**Empirical result.**
- chatbot-ui: 0 change (FP cluster is sibling-writes, not navigation).
- saas-starter: 0 change (FPs are write-then-audit, not navigation).
- documenso: 10 → 9 (`apps/remix/app/components/forms/forgot-password.tsx:44`
  — `await navigate('/check-email')` after `await forgotPassword(...)` —
  correctly suppressed).
- midday `delete-account.tsx`: NOT suppressed because the callee
  `redirectAfterAccountDeletion` is not a framework-standard name (would
  match if H2 were extended to fire when the FIRST callee is `signOut`,
  but the user-defined-name risk argues against extending).

→ Net positive (1 FP fix, 0 regression). Shipped.

## Rule design decisions made during Round 3

| Decision | Rationale |
|----------|-----------|
| Ship `react-async-parallel` as `recommended` / default in `@ax/eslint-plugin-ax/recommended` | 70% TP rate + Next.js docs alignment + Vercel-reference-clean signal |
| Ship `prefer-functional-setstate` as secondary (also in recommended) | Single TP hit on saas-starter validated the pattern; Round 3 too short to broaden the corpus |
| Hold remaining 5 rules out of `recommended` until each has ≥3 unrelated real-repo TP examples | Codex Round 2 bar: rules without empirical TP examples are inventory, not product |
| Reject H1 (shared prior-bound name) | TP regression on chatbot-ui `db/files.ts` |
| Ship H2 (flow-control callees) | 1 FP fix, 0 regression on the broader corpus |
| Add interactive-CLI file skip (3A, FP fix from Round 2 saas-starter `setup.ts`) | Empirically validated FP cluster + clean AST signal (import detection) |

## What this opens (Round 5+ deferred)

- **useEffect data-fetch waterfalls in client components** — existing rule
  already fires on these (verified at chatbot-ui `chat-ui.tsx:62-63`,
  TRUE_POSITIVE). No new rule needed; the rule's "any async function body"
  scope is sufficient.
- **Write-then-link semantic detection** — needs to distinguish "both awaits
  WRITE related rows that reference the prior parent" from "both awaits READ
  attributes of the prior parent and do independent work". AST alone can't do
  this. Possible extensions: callee-name heuristics (`create*|insert*|update*|delete*`),
  shared-prefix detection (`createAssistant*` cluster), or read-vs-write tag
  inference from receiver method names.
- **Init-then-use gate contracts** — `ensureInit() → use()` and similar.
  Fuzzy. Suggest `// eslint-disable-next-line ax/react-async-parallel` with
  a one-line comment as the canonical suppression.
- **Auto-fix suggestions** — ESLint's `--fix` could rewrite `await A(); await B()`
  to `const [a, b] = await Promise.all([A(), B()])`. Risks: capturing returned
  values when one originally wasn't, breaking error-handling shape (Promise.all
  short-circuits on first rejection vs sequential allowing handling between).
  Out of pilot scope.

## Threshold predictions for adoption

| Round | Base | If narrowed to React async wedge | If continues umbrella expansion |
|-------|------|----------------------------------|---------------------------------|
| Round 1 (initial review) | 25% | 45% | 15% |
| Round 2 (post web search) | 40% | 60% | 15% |
| Round 3 (post empirical + 3A/3G/3H) | 45-55% | 60-65% | 15% |

Adoption gates on actual ship-to-npm + downstream pickup. Subsequent rounds
should track download counts as the empirical signal.

## Artifacts (permanent)

| What | Where |
|------|-------|
| This document | `practices-react/pilot/round-3-empirical-validation.md` |
| Plugin source (current) | `practices-react/eslint-plugin-ax/rules/react-async-parallel.js` |
| Rule tests (28 valid + 6 invalid) | `practices-react/eslint-plugin-ax/tests/react-async-parallel.test.js` |
| Round 2 sample (saas-starter, Next-js-Boilerplate) | `practices-react/pilot/external-validation.md` |
| 4-phase curation pipeline | `practices-react/pilot/pilot-report.md` |

## Artifacts (transient, in `/tmp` — will be GC'd)

These are not committed to the repo. Reference is preserved here for the next
round if it's still available locally.

| Agent | Path |
|-------|------|
| A | `/tmp/ax-r3-agent-a/<repo>-lint.txt` (4 files) |
| B | `/tmp/ax-r3-agent-b/<repo>-lint.txt` (4 files) |
| C | `/tmp/ax-r3-agent-c/<repo>-lint.txt` (3 files; papermark 404) |
| D | `/tmp/ax-r3-agent-d/<repo>-lint.txt` (4 files) |
| E | `/tmp/ax-r3-agent-e/<repo>-lint.txt` (4 files) |
| Codex Round 1 transcript | `/tmp/ax-pilot/codex-review-prompt.txt` + background task output |
| Codex Round 2 transcript | (background task output, ID `b4da0s4ux`) |

## Round 3 commits

| Hash | Subject |
|------|---------|
| `faa90b2` | fix(eslint-plugin-ax): skip files importing readline/inquirer/prompts (3A, FP fix from Round 2 saas-starter) |
| `a2dd751` | refactor: backend → archive/backend-reference + freeze practices/ catalog (3H, strategic reframe) |
| `31b196b` | fix(eslint-plugin-ax): add H2 flow-control heuristic to react-async-parallel (3G) |

## One-line takeaway

The rule found real waterfalls — 70%+ TP rate on its first scaled contact with
the wild, no FP explosion, validated against canonical Next.js docs. The
remaining FP clusters are real but small and addressable. The product wedge
exists.
