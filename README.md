# ax-template

> **`/ax-transform` skill source.** Starter / transformation infrastructure for AI-agent-friendly Java/Spring projects.

## What this is

ax = **AI transformation**. This repo is the source of the Claude Code plugin **`ax-transform`** — a skill that bootstraps (or transforms) a project into a codebase where AI agents can write code safely:

- **Spec Trio** (`specs/` + `contracts/` + `blueprints/`) — contract-first; AI reads spec before code
- **practices/ catalog** — 64 evidence-anchored Java/Spring rules across 21 categories
- **Single-command verification** — `./gradlew test{Domain}` returns binary pass/fail
- **AGENTS.md sentinel** — sha256-anchored AI agent context, auto-regenerated on rule change
- **4 hard gates** — `spec_ref`, `substance`, `time_decay`, `evidence` block AI output that can't anchor to external sources

The `backend/` (Spring Boot) and `frontend/` (React) directories are **reference workloads** — the skill applied to itself.

## What this skill does NOT impose

Catalog quality is enforced. Human-collaboration policy is the fork-받은 팀의 자율:

- Git workflow (branch protection, PR policy, merge strategy, force-push rules)
- Deployment / release / staging policy
- Team code-review or sign-off requirements
- Any CI gate beyond the catalog quality probes

## Installation as a Claude Code plugin

Once published to a Claude Code plugin marketplace:

```
/plugin install ax-transform@<marketplace-name>
```

Local install (during development):

```
/plugin marketplace add /path/to/ax-template
/plugin install ax-transform@<your-marketplace>
```

Plugin manifest: [`.claude-plugin/plugin.json`](./.claude-plugin/plugin.json). Skill entry point: [`skills/ax-transform/SKILL.md`](./skills/ax-transform/SKILL.md).

## Use the repo as a project starter

Clone, then run the verification suite:

```bash
git clone https://github.com/ai-dev-methodologies/ax-template my-project
cd my-project
git submodule update --init  # fetches portability fixtures (petclinic, realworld, modulith)

cd backend && ./gradlew test          # full regression: testAsvs + testCrud + testPractices
```

## Repo layout

```
ax-template/
├── .claude-plugin/plugin.json    # Claude Code plugin manifest
├── skills/
│   └── ax-transform/
│       └── SKILL.md              # /ax-transform skill entry point (frontmatter)
├── CLAUDE.md                     # Project identity + methodology (top-level AI guidance)
├── METHODOLOGY.md                # 5-step blueprint playbook for new domains
├── specs/                        # Compliance specs (auth-asvs-l1, crud-l0, spring-practices-l0)
├── contracts/                    # OpenAPI contracts
├── blueprints/                   # Policy manifests (JWT / session / rate limit / CORS)
├── practices/                    # Catalog: 64 rules / 21 categories / 4 hard gates / evidence trail
│   ├── rules/                    # The 64 rule.md files
│   ├── upstream/                 # Fetched external snapshots (gitignored, regen via fetch.sh)
│   ├── evals/                    # spec_ref / substance / time_decay / evidence guards + advisory probes
│   ├── AGENTS.md                 # AI agent entry point (sha sentinel)
│   ├── SKILL.md                  # practices subsystem skill
│   ├── MAINTAINER.md             # Catalog maintainer guide
│   └── DECISIONS.md              # Rule provenance trail
├── backend/                      # Spring Boot reference workload
├── frontend/                     # React reference workload
├── verify/                       # Optional verification scripts (fork-받은 팀 자율)
└── docs/archive/                 # Historical governance documents
```

## Verification commands

```bash
cd backend
./gradlew testAsvs                # OWASP ASVS L1 (26 items, auth domain)
./gradlew testCrud                # CRUD reference domain (7 security tests)
./gradlew testPractices           # 64 practices rules (binary pass/fail per rule)
./gradlew testPortability         # advisory: rules applied to spring-petclinic / realworld / modulith
```

```bash
# Catalog hard gates (run independently of Gradle)
bash practices/evals/spec_ref_guard.sh        # every rule must declare spec_ref
bash practices/evals/substance_guard.sh       # rule body has Incorrect/Correct ≥2 lines, Reference URL
bash practices/evals/time_decay_guard.sh      # cited snapshots ≤ 90 days old
bash practices/evals/evidence_guard.sh        # evidence block points to a real snapshot or external citation
```

## Optional: install local catalog-quality hooks

```bash
bash practices/scripts/install-hooks.sh       # opt-in per clone
```

Wires `.githooks/pre-commit` (4 hard gates + AGENTS.md auto-regen + testPractices when backend touched) and `.githooks/pre-push` (full regression). The hooks only protect catalog quality; they do not impose any git-workflow policy.

## License

MIT — see plugin.json.

## Related documents

- [`CLAUDE.md`](./CLAUDE.md) — top-level project identity for AI agents
- [`METHODOLOGY.md`](./METHODOLOGY.md) — 5-step blueprint for adding new domains
- [`skills/ax-transform/SKILL.md`](./skills/ax-transform/SKILL.md) — skill entry point
- [`practices/MAINTAINER.md`](./practices/MAINTAINER.md) — catalog maintainer guide
- [`practices/DECISIONS.md`](./practices/DECISIONS.md) — rule provenance trail
