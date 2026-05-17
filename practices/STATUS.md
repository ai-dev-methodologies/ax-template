# practices/ — STATUS: FROZEN v1.0

**Snapshot date:** 2026-05-17
**Last rule added:** 2026-05-16 (build-no-snapshot-dependencies)
**Rule count:** 64
**Categories:** 22

## Decision

`practices/` (Java/Spring catalog) is **frozen as v1.0 reference**. No active
maintenance, no new rules, no new domains.

## Why

Round 3 strategic review (2026-05-17) — 5 agents validating
`practices-react/eslint-plugin-ax` on 19 real Next.js repos — confirmed the
project's actual wedge is in the React side, not the Java side:

- ESLint plugin `react-async-parallel` rule found ~70% TP across 19 repos with
  no FP explosion at monorepo scale.
- The Java catalog has no equivalent enforcement vehicle (testPractices is
  Gradle-specific, not industry-portable; no AI agent shipping Spring code
  installs a "Spring rules" plugin the way they install ESLint).
- 132 rules with 90-day decay refresh × solo maintainer is unsustainable; per
  Codex Round 2 review the Java catalog was identified as the immediate
  "abandon" candidate.

## What this means

| Activity | Java `practices/` | React `practices-react/` |
|----------|-------------------|--------------------------|
| Add new rule | ❌ frozen | ✅ active |
| Add new domain | ❌ frozen | ✅ active |
| Refresh upstream snapshots | ❌ frozen (365d threshold) | ✅ 90d |
| Run `evals/run.sh` | ✅ as health check | ✅ as gate |
| Run `./gradlew testPractices` | ✅ regression-only (after backend move: `cd archive/backend-reference && ./gradlew testPractices`) | n/a |
| Time-decay guard merge gate | ❌ skip (frozen) | ✅ enforce |

## What stays useful

- 64 rules + evidence anchors remain readable as the methodology's worked example.
- `practices/AGENTS.md` (regenerated) still serves any AI agent reading Java/Spring code.
- `practices/_template.md` documents the rule frontmatter format for `practices-react/` to reuse.
- DECISIONS.md, MAINTAINER.md, generate_agents.sh remain as reference artifacts.

## Re-thaw criteria

Reopen Java active development if all three hold:
1. `practices-react/eslint-plugin-ax` reaches 1K+ weekly npm installs (proves wedge),
2. concrete demand surfaces for Java/Spring enforcement of similar nature (issue, PR, request),
3. maintenance capacity > 1 person.

Until then: frozen.
