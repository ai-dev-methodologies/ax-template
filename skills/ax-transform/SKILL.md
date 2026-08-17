---
name: ax-transform
description: AI transformation starter skill. Bootstraps a new project (or transforms an existing one) into an AI-agent-friendly codebase using contract-first Spec Trio, evidence-anchored Java/Spring practices catalog, and single-command verification feedback loops.
---

# ax-transform

A starter / transformation skill for projects that want AI agents (Claude Code etc.) to write code safely — by reading spec-first, applying evidence-anchored rules, and self-verifying with binary pass/fail commands.

**Use this skill when:**
- Bootstrapping a new Java/Spring project that AI agents will work on
- Retrofitting an existing project to be AI-agent-friendly (adding spec contracts + practices catalog)
- Onboarding a team to contract-first + evidence-anchored development with AI assistance

**This skill ships:**

| Asset | Purpose for AI agent |
|---|---|
| **Spec Trio** (`specs/` + `contracts/` + `blueprints/`) | Spec is read before code. AI hallucination's 1st line of defense. |
| **practices/ catalog** (233 rules, 22+ categories) | Evidence-anchored Java/Spring rules. AI cannot invent rules at random — every rule has an external URL / quote / RFC / JEP citation. |
| **`./gradlew test{Domain}`** | Single binary command per domain (testAsvs, testCrud, testPractices, testPortability). AI self-verifies in one shot. |
| **`AGENTS.md`** (auto-regenerated, sha256-sentinel) | AI agent's primary context file. Stays sync'd to `practices/rules/*.md` automatically. |
| **4 hard gates** (spec_ref / substance / time_decay / evidence) | Binary checks that block AI output if it diverges from external facts. |
| **Reference workloads** (`backend/` Spring Boot auth + CRUD + practices fixtures, `frontend/` React) | Worked examples — the skill applies itself to itself. |
| **Frontend Spec Trio** (`specs/<domain>-frontend-l0.yaml` + `contracts/<domain>-ui.yaml` + `blueprints/<domain>-ui-manifest.yaml`) | Mirrors backend Spec Trio for UI routes. Anchors frontend AI output to external specs. |
| **`practices-react/` catalog** (108 rules, 9 families) | Evidence-anchored rules for React 19 / Next.js 16. Mirrors Java catalog discipline. |
| **`templates/` layer library** (L1 primitives, L2 feature blocks, L3 page templates, L4 domain workloads) | Composition kit layers. Each artifact carries `evidence:` frontmatter. |
| **3-tier skill topology** | 3 Tier-1 commands, 8 Tier-2 path-triggered skills, 6 Tier-3 core evidence gates (within the 101-guard suite). Agents navigate via skill invocation graph. |

**This skill does NOT impose:**
- Git workflow (branch protection, PR policy, merge strategy) — fork받은 팀이 정함
- Deployment / release / staging policy
- Team code-review policy or sign-off requirements
- CI merge-gate beyond what the catalog already provides (sentinel CI is offered as an advisory probe; whether the fork-받은 팀 promotes it to a merge gate is their call)

→ Catalog quality is enforced. Human collaboration policy is the user's choice.

## Quickstart

```bash
# 1. Use this repo as a starting point
git clone https://github.com/ai-dev-methodologies/ax-template my-project
cd my-project

# 2. Run the full verification suite
cd backend && ./gradlew test          # testAsvs + testCrud + testPractices

# 3. Check practices catalog hard gates
bash practices/evals/spec_ref_guard.sh
bash practices/evals/substance_guard.sh
bash practices/evals/time_decay_guard.sh
bash practices/evals/evidence_guard.sh

# 4. (Optional) install local hooks for catalog quality
bash practices/scripts/install-hooks.sh
```

## Where to look (AI agent entry points)

| File | Purpose |
|---|---|
| `CLAUDE.md` | Top-level project identity + methodology summary. **Read this first.** |
| `METHODOLOGY.md` | 5-step blueprint playbook for adding a new domain |
| `practices/AGENTS.md` | The 233 rules in AI-consumable form, with sha256 sentinel |
| `practices/SKILL.md` | The practices subsystem entry point |
| `practices/MAINTAINER.md` | Maintainer guide for evolving the catalog |
| `practices/DECISIONS.md` | Rule provenance trail — every accepted / rejected rule with reasoning |
| `specs/auth-asvs-l1.yaml` | OWASP ASVS L1 spec (26 items) |
| `specs/spring-practices-l0.yaml` | Java/Spring rules spec (64 items) |
| `specs/crud-security.yaml` | CRUD reference domain spec |
| `practices-react/AGENTS.md` | 108 React rules in AI-consumable form |
| `practices-react/SKILL.md` | React practices subsystem entry point |
| `templates/AGENTS.md` | Layer library AI-consumable index (sha256 sentinel) |
| `templates/DECISIONS.md` | ADR registry TD-2026-05-17-001..010 — provenance trail |
| `specs/<domain>-frontend-l0.yaml` | Frontend page-compliance spec (per domain) |

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Read `CLAUDE.md` and `METHODOLOGY.md` to understand the 5-step playbook
- [ ] Step 2: Read `practices/AGENTS.md` (Java rules) + `practices-react/AGENTS.md` (React rules)
- [ ] Step 3: Identify domain mode — `full_trio`, `backend_only`, or `frontend_only`
- [ ] Step 4: Create Spec Trio for the new domain (backend spec + contract + blueprint; frontend if applicable)
- [ ] Step 5: Write TDD anchor test (RED) before any implementation
- [ ] Step 6: Implement to GREEN — `./gradlew test{Domain}` exits 0
- [ ] Step 7: Run the guard suite (117 hard guards — 6 core evidence gates + domain/meta guards) — `bash practices/evals/run-all-guards.sh` exits 0
- [ ] Step 8: Invoke `/ax-verify` — Tier-1 recursive check exits 0
- [ ] **Step 9 (MANDATORY — R25)**: `bash practices/scripts/verify-completion.sh` exits 0

> Step 9 is the catalog's binary completion contract. It reads
> `practices/verification-checklist.yaml` and chains: backend-build →
> per-domain-tests (30) → hard-guards → catalog-meta-guards → aggregate-regression.
> No agent may declare "task done" until Step 9 exits 0. The 49th hard guard
> (`completion_checklist_recency_guard.sh`) audits the resulting log and BLOCKS
> `git push` if no entry matches HEAD.

## Steps detail

### Step 3: Identify domain mode
Read `practices/evals/trio_integrity_allowlist.yaml`. If domain is absent, add it
under the correct mode (`full_trio` / `backend_only` / `frontend_only`) before step 4.

### Step 5: TDD anchor
The anchor test must exist and fail (RED) before any implementation file is touched
in the current SP. See `METHODOLOGY.md` §5 for the pattern.

### Step 7: Guard suite
Script: `practices/evals/run-all-guards.sh` — chains all 117 hard guards. The 6
evidence-anchoring core gates are:
`spec_ref_guard.sh`, `substance_guard.sh`, `time_decay_guard.sh`,
`evidence_guard.sh`, `trio_integrity_guard.sh`, `cross_trio_guard.sh`.

## Feedback loop
When step fails: read stderr from the failing guard. Each guard emits a named error
code (e.g. `MISSING_FRONTEND_SPEC`, `COVERAGE_SHORTFALL`, `ZERO_SCAN`). Fix the
artifact that owns that error code, then re-run the guard in isolation before
re-running the full suite.
Halt threshold: if 3 consecutive guard runs fail on the same error code, escalate
to `docs/superpowers/escape/` — do NOT attempt further variations without human review.

### Mechanical feedback loop (R25 enforcement)
For autonomous agents, the loop is bundled in `verify-and-fix-loop.sh`:
```bash
bash practices/scripts/verify-and-fix-loop.sh
```
This runs `verify-completion.sh`, prints the `fix_playbook` for the failing
step, pauses for a fix, and retries — up to 3 attempts. After 3 failed
attempts it exits 1 and demands human review. Headless / CI variant:
```bash
bash practices/scripts/verify-and-fix-loop.sh --non-interactive
```

## Invocation graph
- Calls (Tier-2): delegates to `/ax-verify` for end-to-end verification
- Called by (Tier-1): user directly; no parent skill

## Why ax-transform exists

AI agents writing production code carry specific risks:
1. **Hallucination** — inventing rules / APIs / patterns that don't exist
2. **No self-verification** — shipping code without confirming behavior
3. **Drift from external facts** — out-of-date docs / RFCs

This skill makes all three risks **mechanically detectable**:
1. Spec Trio forces the AI to read the contract before writing the implementation
2. `./gradlew test{Domain}` gives binary pass/fail in seconds
3. Evidence guard refuses any rule that doesn't cite an external URL + quote

The output: a codebase where AI agents are **constrained by external sources**, not by their training-time memories.

## Related skills (within this repo)

- `practices/SKILL.md` — the practices subsystem (catalog quality gates)

## License & origin

ax-template lives at the methodology HQ: https://github.com/ai-dev-methodologies/ax-template
