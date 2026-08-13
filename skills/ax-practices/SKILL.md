---
name: ax-practices
description: >
  Entry/routing skill for applying the ax-template practices catalog (Java + React
  rules) to a project WITHOUT forking the whole repo — reads the target project's
  ax.config.json + the catalog's INDEX.md, selects relevant rules by tag and impact,
  and routes each rule to "apply directly" (review-type) or "not installed — see
  install guide" (any machine-checked type). Use when a project wants catalog
  guidance applied to its own code but has not adopted ax-template's Spec Trio /
  reference workloads.
metadata:
  priority: 1
  tier: 1
  axis: consumption-channel
  docs:
    - "practices/INDEX.md"
    - "practices-react/INDEX.md"
    - "skills/ax-init-config/SKILL.md"
  pathPatterns:
    - 'skills/ax-practices/SKILL.md'
    - 'ax.config.json'
  bashPatterns:
    - 'bash practices/generate_index.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-practices
    - apply practices catalog
    - apply best practices
    - review against ax rules
    - what rules apply here
  intents:
    - "apply the ax-template practices catalog to my project"
    - "review this file against the java/react rule catalog"
    - "which ax rules apply to this code"
  entities:
    - practices catalog
    - INDEX.md
    - ax.config.json
    - verification_kind
    - fork-receiver
---

# ax-practices

Entry/routing skill for the practices catalog (`practices/` = 233 Java/Spring rules,
`practices-react/` = 102 React/Next.js rules). It routes an AI agent or reviewer to the
right rules for a project; it does not itself enforce anything.

**Principle (non-negotiable — read before using this skill).** `practices/evals/*_guard.sh`
resolve `REPO_ROOT` from their own on-disk location. Running them from ax-template's
own checkout checks ax-template itself, not the project you are consuming the catalog
from. **The skill routes knowledge. Enforcement is whatever lint/hook/test the target
project has actually installed** (see `skills/ax-install-*` guides, D-3). Never say or
imply that applying this skill "enforces" the catalog on a project — say what was
actually applied (a human/AI judgment call) versus what would need an installed gate.

## Procedure (fixed order — do not reorder or skip a step)

1. **Read `ax.config.json` at the project root.** If it does not exist, do not guess
   the project's layout — invoke `ax-init-config` (see `skills/ax-init-config/SKILL.md`)
   and stop. Resume only after `ax.config.json` exists.

2. **Read only the INDEX(es) for the stacks the config declares** (`ax.config.json`'s
   `stacks` array — e.g. `["react"]`, `["java"]`, or both). Each catalog's INDEX lives
   at two possible locations depending on how this skill was installed — check both,
   in this order, and use whichever exists:
   - Plugin install: `${CLAUDE_PLUGIN_ROOT}/practices/INDEX.md` /
     `${CLAUDE_PLUGIN_ROOT}/practices-react/INDEX.md`
   - Clone / fork checkout: `practices/INDEX.md` / `practices-react/INDEX.md`
     (repo-relative to wherever the catalog lives)

   `ls` the candidate path before reading it — do not assume either location exists.
   If **neither** location resolves for a stack the config declares, report that
   explicitly ("could not locate the `<stack>` catalog INDEX — checked
   `${CLAUDE_PLUGIN_ROOT}/...` and `<repo>/...`") and stop for that stack. Do not
   silently fall back to guessing rule content.

3. **Select candidate rules by tag match** against the task at hand, using the
   INDEX's `## By tag` section. Drop any rule id present in `ax.config.json`'s
   `rules.disabled`, and drop any rule whose tags are entirely covered by
   `rules.excludeTags` (the config schema carries these fields precisely so a
   project can opt a rule or tag family out — honor that before spending budget
   reading the rule body).

   Read **at most 8** of the remaining candidates individually
   (`<catalog>/rules/<id>.md`) — do not expand the whole INDEX into rule bodies;
   that defeats the point of having an index. When more than 8 candidates remain,
   rank by this **total order** and take the top 8:

   ```
   CRITICAL > HIGH > MEDIUM-HIGH > MEDIUM > LOW-MEDIUM > LOW
   ```

   This is a strict total order, not "prefer HIGH" — HIGH-impact rules vastly
   outnumber CRITICAL ones in this catalog, so a same-tier tie-break would silently
   drop CRITICAL rules once the 8-rule cap is reached. A rule whose `impact` value
   falls outside these six labels ranks **last**, and that fact is reported to the
   user rather than absorbed quietly.

4. **Route by `verification_kind`** (the normalized value `practices/generate_index.sh`
   writes into the INDEX's `verification` column). This routing is **deny-by-default**
   — `review` is the **only** allowlisted kind:
   - `review` → apply directly: read the rule, judge the code against it, report
     findings inline.
   - **Any other value** → **"not installed — see the install guide"** only. Never
     claim this skill enforced or verified a machine-checked rule; it did not run
     the check. The two catalogs use different token shapes for these values
     (`practices/INDEX.md`: colon-qualified like `gradle:*`/`eslint:*`/`guard:*`;
     `practices-react/INDEX.md`: bare tokens like `lint`/`script`/`regex_scan`/
     `eslint`/`guard`) — treat both shapes, and any value not yet seen in either
     catalog, the same way. Any example list of kinds is illustrative only, never
     exhaustive; an unrecognized token is never pass-through.

   A new `verification_kind` value that doesn't match `review` must still fall
   into the "not installed" branch, never into "apply directly" — the failure mode
   this guards against is a rule quietly being treated as enforced when nothing
   ran.

5. **Cite the rule id in every finding** — `ax/<id>` (ESLint rule) or
   `practices/rules/<id>.md` / `practices-react/rules/<id>.md` (catalog rule). A
   finding without a citable rule id is not from this catalog; say so.

6. **Never apply the catalog outside the config's declared roots.** Java rules
   apply only under `ax.config.json`'s `java.root`; React rules apply only under
   `react.root`. A file outside both roots gets no catalog-derived findings from
   this skill, regardless of how relevant a rule might look.

   Some rule bodies additionally carry a narrower-looking scope hint — a
   `protects_template_id` pointing at an ax-template artifact (e.g.
   `templates/L2/blocks/status-badge.tsx`), or an L4-structure premise (tags like
   `no-l4-cross-import`, `prefer-recipe-over-l4-page-cross-import`). Read these as
   **provenance** (what ax-template artifact the rule was extracted from), not as
   a scope restriction narrower than `react.root`/`java.root`. A target project
   need not have that literal path or an L4 layout at all — apply the rule to
   whatever structurally analogous code exists under the declared root.

## What this skill does NOT do

- It does not install the ESLint plugin, git hooks, or any gradle task — see the
  D-3 install-guide skills for that.
- It does not run `gradle`/`eslint`/any guard script on the target project's code.
- It does not modify `ax.config.json` — that is `ax-init-config`'s job, and only
  after explicit user approval of the generated config.

## Self-check before reporting "applied ax-practices"

- [ ] `ax.config.json` was read (or `ax-init-config` was invoked and the run stopped there)
- [ ] Only the INDEX(es) for the declared `stacks` were read, from a location that was `ls`-verified to exist
- [ ] At most 8 rule bodies were read, selected by the CRITICAL→LOW total order
- [ ] Every reported finding cites a rule id
- [ ] No finding falls outside `react.root` / `java.root`
- [ ] No machine-checked (`verification_kind != review`) rule was reported as "enforced" or "checked" — only "not installed"
