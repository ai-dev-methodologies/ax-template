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
| **practices/ catalog** (64 rules, 21 categories) | Evidence-anchored Java/Spring rules. AI cannot invent rules at random — every rule has an external URL / quote / RFC / JEP citation. |
| **`./gradlew test{Domain}`** | Single binary command per domain (testAsvs, testCrud, testPractices, testPortability). AI self-verifies in one shot. |
| **`AGENTS.md`** (auto-regenerated, sha256-sentinel) | AI agent's primary context file. Stays sync'd to `practices/rules/*.md` automatically. |
| **4 hard gates** (spec_ref / substance / time_decay / evidence) | Binary checks that block AI output if it diverges from external facts. |
| **Reference workloads** (`backend/` Spring Boot auth + CRUD + practices fixtures, `frontend/` React) | Worked examples — the skill applies itself to itself. |

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
| `practices/AGENTS.md` | The 64 rules in AI-consumable form, with sha256 sentinel |
| `practices/SKILL.md` | The practices subsystem entry point |
| `practices/MAINTAINER.md` | Maintainer guide for evolving the catalog |
| `practices/DECISIONS.md` | Rule provenance trail — every accepted / rejected rule with reasoning |
| `specs/auth-asvs-l1.yaml` | OWASP ASVS L1 spec (26 items) |
| `specs/spring-practices-l0.yaml` | Java/Spring rules spec (64 items) |
| `specs/crud-l0.yaml` | CRUD reference domain spec |

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
