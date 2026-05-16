# P3 — Catalog Quality Enforcement (skill-internal only)

> **Scope correction 2026-05-16**: ax-template is a `/ax-transform` skill package source.
> The skill must NOT enforce a fork-받은 팀's git workflow, branch protection, PR policy,
> or human collaboration policy. P3 enforcement is therefore reduced to **catalog quality**:
> the 4 binary hard gates (spec_ref / substance / time_decay / evidence) + AGENTS.md
> sentinel + testPractices. Git workflow strength (branch protection on main, push-level
> guards, [break-glass] PR title conventions) was REMOVED — that's fork-받은 팀의 결정.
>
> The original 7-axis sheet below is preserved for historical context. Axes 1–3 and 6–7
> are explicitly **out of skill scope** going forward. Only the catalog-quality axes
> (Stage 2 testPractices + 4 binary guards) remain skill-enforced.

## How to use

For each section: pick one option (or write "Other:" with the alternative). Sign and date
the file. Then open a follow-up ralplan for the chosen P3 plan. Defaults marked **bold**
are the lowest-friction starting point; pick differently only with a reason.

---

## 1. Enforcement target — *which surface enforces?*

- [ ] Claude Code PreToolUse / PostToolUse hooks (per-developer Claude session)
- [ ] IDE plugin (IntelliJ / VSCode)
- [ ] Local pre-commit hook (developers install)
- [ ] Server-side CI (GitHub Action on PR — already exists for sentinel)
- [ ] **CI only** ← default: zero per-developer setup, single point of truth
- [ ] All of the above

Tradeoff: more surfaces = earlier feedback but higher install/maintenance burden.

## 2. Enforcement strength

- [ ] Hard-block (CI red → no merge)
- [ ] Soft-warn (CI green with annotation; humans decide)
- [ ] **Hard-block on guards, soft-warn on advisory metrics** ← default; matches existing sentinel
- [ ] Advisory-only (information without consequence)

## 3. Escape hatch

- [ ] No bypass — guards are absolute
- [ ] `--no-verify` / `[skip practices]` commit tag (logged in audit)
- [ ] Maintainer override (approved PR comment unlocks)
- [ ] **Maintainer override only** ← default; logged for auditability

Tradeoff: no bypass strains emergency hotfixes; full bypass invites silent decay.

## 4. Scope — *which guards are enforced?*

- [ ] Hard gates only (spec_ref + substance + time_decay + evidence)
- [ ] **Hard gates + balance WARN posted as PR comment (no block)** ← default
- [ ] Everything including advisory rubric score
- [ ] Tiered: hard gates blocking; balance soft; outcome informational

## 5. Failure UX — *what does a developer see when blocked?*

- [ ] Generic CI failure ("evidence_guard exited 1")
- [ ] Detailed violation list with file paths and line refs
- [ ] **Detailed violations + link to the relevant rule.md + suggested fix** ← default
- [ ] Same plus interactive `practices fix --rule X` CLI

## 6. Per-developer opt-out

- [ ] **No opt-out** ← default; rules apply to all PRs uniformly
- [ ] Opt-out by branch label (e.g. `wip/*` skips enforcement)
- [ ] Opt-out by team / repo subpath
- [ ] Opt-out by developer (not recommended — defeats the purpose)

## 7. Adoption rollout

- [ ] Big-bang (one PR turns everything on)
- [ ] **Per-rule rollout** (each hard gate goes through soft → hard over 2 weeks) ← default
- [ ] Per-team / per-repo rollout
- [ ] Opt-in only (developers volunteer)

---

## Sign-off

| Field | Value |
|-------|-------|
| Date | 2026-05-15 |
| Maintainer | jay |
| Chosen profile | **Multi-layer hard enforcement** (overrides earlier "CI only" default) |
| Notes | User instruction: "CI단으로 넘어가며 안되" — local commit-time + edit-time feedback required, no `--no-verify` blanket bypass. |

### Effective axis values — REDUCED to catalog-quality only (2026-05-16)

The skill only enforces what makes the **catalog itself** trustworthy. Git / branch
/ PR / human-process axes are removed; fork받은 팀 decides those.

| Axis | In-skill behavior | Out-of-skill (fork-받은 팀 자율) |
|------|-------------------|--------------------------------|
| 1. enforcement target | Claude Code PreToolUse hook + git pre-commit hook (Stage 0 AGENTS.md regen + 4 binary guards + Stage 2 testPractices) + git pre-push hook (full regression) | Branch protection, PR-required policy, merge strategy, force-push allowance |
| 2. enforcement strength | Hard-block, unconditional, for the 4 binary catalog-quality gates | Whether catalog gates also block merges in fork-받은 팀의 CI is their call |
| 3. escape hatch | None at the catalog-quality layer — guards always run when their inputs change. (Hooks themselves are opt-in per clone via `install-hooks.sh`.) | Any human-process break-glass procedure is fork-받은 팀이 정함 |
| 4. scope | 4 hard gates (spec_ref + substance + time_decay + evidence) + AGENTS.md sentinel + testPractices when backend/practices/ touched | balance / outcome / quote_match / portability remain advisory regardless of context |
| 5. failure UX | Detailed violation list + rule.md link + actionable fix command | — |
| 6. per-developer opt-out | None within catalog-quality (the hooks are mechanically uniform per clone). Skip is achieved by not installing hooks, which sentinel CI catches downstream when adopted | — |
| 7. adoption rollout | Catalog-quality gates were live since P0 — no rollout window needed | Whether fork-받은 팀 promotes the same gates into their own CI is their staged decision |

### Activation steps for a fresh clone (catalog-quality only)

1. `bash practices/scripts/install-hooks.sh` — wires `.githooks/pre-commit` AND `.githooks/pre-push` for catalog-quality regression
2. Restart Claude Code in the repo to pick up `.claude/settings.local.json` (edit-time gate)

That's it. **No branch protection, no [break-glass] PR title, no `enforce_admins=true`** — those were removed when the skill scope was corrected. If a fork-받은 팀 wants any of that, they configure it in their own repo, in their own GitHub settings, with their own rules.

### Local gate stages (added 2026-05-15 after P2-B6 build-fail incident)

| Stage | When | What runs | Blocks |
|-------|------|-----------|--------|
| **pre-commit / stage 1** | every commit touching practices/ or the seed spec or backend practices fixtures | 4 binary guards | commit |
| **pre-commit / stage 2** | commit touches `backend/src/{main,test}/java/.../practices/` | `./gradlew testPractices` | commit |
| **pre-push / stage 3** | local commits ahead of remote touch backend/, practices/, or seed spec | `./gradlew testPractices testAsvs testCrud` | push |
| **PreToolUse (Claude Code)** | Write/Edit/MultiEdit on rule.md / seed spec / backend practices | spec_ref + substance + evidence | the tool call (Claude is forced to re-plan) |

The incident that motivated stage 2 was P2-B6: a commit landed in which `Edit` silently failed on two source files (Read prerequisite not met), so the `MethodArgumentNotValidException` handler and the `/practices/demo/users` endpoint never existed in the committed code, yet the 4 binary guards passed and the commit was accepted. The build-time test was the only thing that would have caught it, and only sentinel CI (PR-time) was watching. Pre-commit stage 2 closes the loop locally.

### ~~Break-glass procedure~~ — REMOVED (2026-05-16)

The PR-title `[break-glass]:` convention assumed a fork-받은 팀이 PR-based workflow를 채택했다는 전제였음. ax-template은 git workflow를 강제하지 않으므로 이 procedure 자체가 skill scope 밖. Fork-받은 팀이 본인 workflow에 맞는 escape hatch을 정의함.

Catalog-quality 자체에는 escape hatch가 없음 — 4 hard gates는 통과하거나 통과 못 하거나, binary. 통과 못 하면 commit/push 안 됨. 그게 catalog 신뢰의 기반.

---

## Non-decisions (already settled by ralplan ADR)

- The 22-category catalog stays advisory; P3 cannot promote it to a hard gate without a
  new ralplan.
- Per the ADR, P3 developer-side work is a **separate phase** from maintainer-side
  practices/. P3 must never modify the hard-gate semantics (binary `./gradlew testPractices`
  + evidence + spec_ref) — those are owned by the maintainer-side spec.
- "Force-merge bypass" of the binary hard gate is a Methodology violation and is not on
  this sheet — `./gradlew testPractices` must pass before any merge regardless of P3 surface.
