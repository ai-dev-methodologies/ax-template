---
name: ax-init-config
description: >
  Detects a project's stack, layout, and build tool, then proposes an ax.config.json
  (version/stacks/react/java/design/rules per
  practices-react/eslint-plugin-ax/schemas/ax.config.schema.json) for user approval.
  Never writes the file without an explicit approval step. Use when `ax-practices` (or
  any D-3 install skill) reports that ax.config.json is missing.
metadata:
  priority: 1
  tier: 1
  axis: consumption-channel
  docs:
    - "practices-react/eslint-plugin-ax/schemas/ax.config.schema.json"
    - "ax.config.sample.json"
    - "skills/ax-practices/SKILL.md"
  pathPatterns:
    - 'skills/ax-init-config/SKILL.md'
  bashPatterns: []
  importPatterns: []
retrieval:
  aliases:
    - ax-init-config
    - generate ax.config.json
    - detect project layout
    - init ax config
  intents:
    - "create an ax.config.json for my project"
    - "detect my project's stack and layout"
  entities:
    - ax.config.json
    - ax.config.schema.json
    - stack detection
    - fork-receiver
---

# ax-init-config

Detects a project's stack(s), directory layout, and build tooling, then proposes a
complete `ax.config.json` for the user to approve. **Never generates the file
silently** — detection is guessing dressed as certainty until a human confirms it,
and a wrong `srcDir`/`root` makes every downstream rule/lint silently apply to the
wrong files or nothing at all (the exact failure class `ax-practices` and the D-3
install guides exist to avoid).

## Output shape

The proposed config must validate against `ax.config.schema.json` — a plain object
with `version`, `stacks`, `react`, `java`, `design`, `rules`. The schema and a
filled-in example (`ax.config.sample.json`) each live at two possible locations
depending on how this skill was installed — check both, in this order, and use
whichever exists:
- Plugin install: `${CLAUDE_PLUGIN_ROOT}/practices-react/eslint-plugin-ax/schemas/ax.config.schema.json`
  / `${CLAUDE_PLUGIN_ROOT}/ax.config.sample.json`
- Clone / fork checkout: `practices-react/eslint-plugin-ax/schemas/ax.config.schema.json`
  / `ax.config.sample.json` (repo-relative to wherever the catalog lives)

`ls` the candidate path before reading it — do not assume either location exists.
If **neither** location resolves, report that explicitly ("could not locate
`ax.config.schema.json` — checked `${CLAUDE_PLUGIN_ROOT}/...` and `<repo>/...`")
rather than skipping validation silently, and fall back to validating only against
the field list above (`version`/`stacks`/`react`/`java`/`design`/`rules`).

## Procedure (fixed order)

1. **Detect stacks present.**
   - React/Next.js: a `package.json` exists (check `workspaces` too — the React app
     may live in a workspace package rather than the repo root) with a `react` or
     `next` dependency.
   - Java/Spring: `build.gradle`, `build.gradle.kts`, or `pom.xml` exists anywhere
     in the tree (not just the root — monorepos put it under a subdirectory).
   - Populate `stacks` with whichever of `["react", "java"]` were actually found.
     A project can be either, both, or (report explicitly) neither.

2. **For a detected React stack, derive `react.*`:**
   - `root`: the directory containing the React `package.json` (repo-relative;
     `.` if it's the repo root itself, not `frontend` — do not assume `frontend`).
   - `srcDir`: look for `src/` under `root` first; if absent, use whatever
     top-level directory actually holds application code (report the choice, do
     not silently invent one).
   - `alias`: read `tsconfig.json`'s (or `jsconfig.json`'s) `compilerOptions.paths`
     and reverse-map each entry into `{ prefix: replacement }` form (e.g.
     `"@/*": ["./src/*"]` → `{ "@/": "src/" }`). If no `paths` config exists, do
     **not** fall back to the schema default's literal `src/` — that default
     assumes the conventional layout and silently mismatches any project whose
     detected `srcDir` (step above) is something else. Derive the alias target
     from the `srcDir` already detected instead: `{ "@/": "<srcDir>/" }`. Only
     if `srcDir` itself could not be detected either, fall back to the schema
     default and flag it as needing manual confirmation.
   - `layers`: look for `app`, `features`, `components`, `lib` (or clear analogs)
     as top-level directories under `srcDir`. Map found directories into
     `layers.app` / `layers.features` / `layers.shared` by name; directories that
     don't match any known layer name are left out and reported, not guessed into
     a layer.

3. **For a detected Java stack, derive `java.*`:**
   - `root`: the directory containing the `build.gradle(.kts)`/`pom.xml`.
   - `buildTool`: `"gradle"` if a Gradle file was found, `"maven"` if only `pom.xml`.
   - `rootPackage`: walk the Java source tree (`src/main/java` under `root`) and
     take the **longest common package prefix** across all `.java` files found
     (e.g. if every file lives under `com/example/app/...`, the root package is
     `com.example.app`). If the source tree is empty or has no common prefix
     deeper than one segment, report that and leave `rootPackage` for the user to
     fill in rather than guessing a one-segment package.

4. **Leave `design.useCatalog` and `rules.excludeTags`/`rules.disabled` at schema
   defaults** (`useCatalog: true`, empty arrays) unless the user has already stated
   a preference — this skill's job is layout detection, not rule curation.

5. **Present the complete proposed `ax.config.json` to the user and ask for
   approval before writing anything.** Show:
   - every field that was detected with confidence, and from what evidence
     (e.g. "`java.root: backend` — found `backend/build.gradle.kts`")
   - every field that could not be detected and was left at a schema default or a
     placeholder, flagged as needing manual confirmation

6. **Only after explicit approval, write `ax.config.json` to the project root.**
   If the user requests changes, apply them and re-present before writing — do not
   write an intermediate, unapproved version.

## What this skill does NOT do

- It does not install the ESLint plugin, hooks, or any gradle wiring.
- It does not apply any practices-catalog rule — that's `ax-practices`, invoked
  after `ax.config.json` exists.
- It does not overwrite an existing `ax.config.json` without being asked to.

## Self-check before reporting "config generated"

- [ ] Every populated field traces to a specific piece of detected evidence (a file, a directory, a `tsconfig.json` entry) — none were invented
- [ ] Every field that could not be detected is flagged, not silently defaulted without mention
- [ ] The user explicitly approved the shown config before the file was written
- [ ] The written file validates against `ax.config.schema.json`'s required/typed fields
