# P3 — Developer-Side Enforcement Decision Sheet

> This file enumerates the open questions that **must** be answered before P3 work can
> begin. The maintainer-side practices/ infrastructure (P0+P1+P2-A+P2-C) is in place and
> can run without P3. But applying it to developer flows requires explicit choices on
> seven axes below. Until these are decided, P3 stays out of scope.

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

### Effective axis values (overrides the §1–§7 defaults above)

| Axis | Chosen | Rationale |
|------|--------|-----------|
| 1. enforcement target | **CI + git pre-commit hook + git pre-push hook + Claude Code PreToolUse hook** (4 surfaces) | CI-only feedback is PR-time = too late. Local loops shorten to seconds. Pre-push adds full regression before any remote interaction. |
| 2. enforcement strength | **Hard-block, unconditional** for the 4 binary gates | "템플릿을 지키지 않으면 무조건 실패." |
| 3. escape hatch | **Break-glass only** — PR title `[break-glass]:` + audit entry in `practices/break-glass-log.md`, same commit | Auditable rather than free. |
| 4. scope | All 4 hard gates (spec_ref + substance + time_decay + evidence). balance / outcome / quote_match remain advisory. | Matches rubric.yaml.gating.binary_only contract. |
| 5. failure UX | Detailed violation list + rule.md link + suggested fix command | Lowest-friction recovery. |
| 6. per-developer opt-out | **None.** | Uniform application. |
| 7. adoption rollout | **Immediate hard** for the 13 currently-green rules. | All rules pass locally + in CI today; no rollout window needed. |
| 8. rule request (new axis) | `.github/ISSUE_TEMPLATE/practices-rule-request.yml` — evidence required, maintainer review, requester drafts the PR | Open channel for new-rule proposals without bypassing provenance policy. |

### Activation steps for a fresh clone

1. `bash practices/scripts/install-hooks.sh` — wires `.githooks/pre-commit` AND `.githooks/pre-push`
2. Restart Claude Code in the repo to pick up `.claude/settings.local.json` (edit-time gate)
3. Apply main branch protection — codified, not click-ops:
   - Source-of-truth JSON: `.github/rulesets/main-protection.json`
   - Apply: `bash practices/scripts/setup-branch-protection.sh` (requires `gh auth login` + repo admin)
   - Verify: `bash practices/scripts/setup-branch-protection.sh --check`
   - Dry-run: `bash practices/scripts/setup-branch-protection.sh --dry-run`
   - Policy locks in: linear history, PR-required, `practices-sentinel / guards` status check, `enforce_admins=true` (no admin bypass), no force-push, no deletions, required conversation resolution.

### Local gate stages (added 2026-05-15 after P2-B6 build-fail incident)

| Stage | When | What runs | Blocks |
|-------|------|-----------|--------|
| **pre-commit / stage 1** | every commit touching practices/ or the seed spec or backend practices fixtures | 4 binary guards | commit |
| **pre-commit / stage 2** | commit touches `backend/src/{main,test}/java/.../practices/` | `./gradlew testPractices` | commit |
| **pre-push / stage 3** | local commits ahead of remote touch backend/, practices/, or seed spec | `./gradlew testPractices testAsvs testCrud` | push |
| **PreToolUse (Claude Code)** | Write/Edit/MultiEdit on rule.md / seed spec / backend practices | spec_ref + substance + evidence | the tool call (Claude is forced to re-plan) |

The incident that motivated stage 2 was P2-B6: a commit landed in which `Edit` silently failed on two source files (Read prerequisite not met), so the `MethodArgumentNotValidException` handler and the `/practices/demo/users` endpoint never existed in the committed code, yet the 4 binary guards passed and the commit was accepted. The build-time test was the only thing that would have caught it, and only sentinel CI (PR-time) was watching. Pre-commit stage 2 closes the loop locally.

### Break-glass procedure (Axis 3 detail)

Used only when a critical hotfix must merge before the relevant guard can pass:

1. PR title prefixed `[break-glass]: <one-line reason>`
2. Same commit must add an entry to `practices/break-glass-log.md` (date / maintainer / guard bypassed / reason / planned remediation / re-evaluation date)
3. The follow-up PR that re-imposes the guard must land within **14 days**

Override without these three artifacts is a Methodology violation.

---

## Non-decisions (already settled by ralplan ADR)

- The 22-category catalog stays advisory; P3 cannot promote it to a hard gate without a
  new ralplan.
- Per the ADR, P3 developer-side work is a **separate phase** from maintainer-side
  practices/. P3 must never modify the hard-gate semantics (binary `./gradlew testPractices`
  + evidence + spec_ref) — those are owned by the maintainer-side spec.
- "Force-merge bypass" of the binary hard gate is a Methodology violation and is not on
  this sheet — `./gradlew testPractices` must pass before any merge regardless of P3 surface.
