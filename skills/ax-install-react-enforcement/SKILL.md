---
name: ax-install-react-enforcement
description: >
  Installs the practices-react ESLint plugin (`@ax/eslint-plugin-ax`) as a real,
  running gate in a downstream project — `file:` dependency, a `settings.ax`-wired
  `eslint.config.mjs` with a layout-parameterized `files` glob, and a mandatory
  probe→detect→delete non-vacuity check. Use when `ax-practices` reports a React
  rule as "not installed — see the install guide", or when a user asks to actually
  enforce (not just read) the ax React/Next.js catalog.
metadata:
  priority: 1
  tier: 1
  axis: consumption-channel
  docs:
    - "practices-react/README.md"
    - "practices-react/pilot/external-validation.md"
    - "practices-react/eslint-plugin-ax/schemas/ax.config.schema.json"
    - "practices-react/eslint-plugin-ax/lib/feature-layout.js"
    - "skills/ax-init-config/SKILL.md"
    - "skills/ax-practices/SKILL.md"
  pathPatterns:
    - 'skills/ax-install-react-enforcement/SKILL.md'
    - 'eslint.config.mjs'
    - 'eslint.config.js'
    - 'ax.config.json'
  bashPatterns:
    - 'npm install --save-dev file:*/eslint-plugin-ax'
    - 'npx eslint'
  importPatterns:
    - '@ax/eslint-plugin-ax'
retrieval:
  aliases:
    - ax-install-react-enforcement
    - install eslint plugin
    - wire eslint.config.mjs
    - enforce ax react rules
    - install ax-template lint
  intents:
    - "install the ax-template React/Next.js ESLint plugin in my project"
    - "actually enforce the practices-react catalog, not just review against it"
    - "why does my custom srcDir report 0 lint errors"
  entities:
    - eslint-plugin-ax
    - ax.config.json
    - settings.ax
    - no-upward-layer-import
    - srcDir
---

# ax-install-react-enforcement

Installs `practices-react`'s ESLint plugin as a **running gate**, not just a read
reference. `ax-practices` routes React catalog rules to "not installed" until this
skill (or an equivalent manual install) has actually been done — reading a rule is
not enforcing it.

**Principle.** Enforcement here is exactly what ESLint reports on the files it
actually lints. If the plugin loads but the `files` glob matches nothing, or
`settings.ax` was never injected, ESLint will report `0 problems` — which looks
identical to "the project is clean." Step 4 below exists specifically to rule that
out before this skill reports success.

## Procedure (fixed order)

### 1. Prerequisite: `ax.config.json`

This skill needs `ax.config.json` at the project root to know `react.srcDir` /
`react.alias` / `react.layers`. If it does not exist, **do not guess the layout**
— invoke `ax-init-config` (see `skills/ax-init-config/SKILL.md`) and stop. Resume
only after `ax.config.json` exists.

### 2. Install the plugin as a `file:` dependency

Resolve the plugin path depending on how this skill is being run — check both, in
this order, and use whichever exists (`ls` it before installing, do not assume):

- **Plugin (Claude Code plugin) environment**:
  `${CLAUDE_PLUGIN_ROOT}/practices-react/eslint-plugin-ax`
- **Clone / fork checkout environment**:
  `<ax-template-clone>/practices-react/eslint-plugin-ax` (a sibling or ancestor
  checkout of ax-template — resolve its actual path, do not assume `../ax-template`)

```bash
ls "<resolved-path>/package.json"   # confirm before installing — do not assume either path resolves
npm i -D "file:<resolved-path>"
```

`npm install file:<dir>` creates a symlink, so this works without publishing to
npm. It also means the plugin's own `package.json` `files` allowlist is bypassed
for local installs — this is expected in this channel, not a bug to fix here.

### 3. Wire `eslint.config.mjs`

**First, check whether the project has TypeScript**: a `tsconfig.json` at or above
`axConfig.react?.root`, or `.ts`/`.tsx` files under `axConfig.react?.srcDir`. If so, install
`typescript-eslint` and wire its parser **on the same config block** as the ax plugin — do not
treat this as optional or defer it:

```bash
npm i -D typescript-eslint
```

**Why this is mandatory, not a nicety**: with ESLint's default parser (`espree`), every `.ts`/
`.tsx` file that uses TypeScript-only syntax throws a parsing error and is skipped for *every*
rule — not just ax's. A run that reports `0 problems` because every `.ts` file failed to parse
looks identical, at a glance, to a run that reports `0 problems` because the project is clean.
Skipping the parser wiring means every ax rule silently never executes on `.ts`/`.tsx` files.

If the project has no TypeScript sources, skip the `typescript-eslint` install and omit the
`languageOptions` line below — the default parser is correct for plain JS/JSX.

```js
import fs from 'node:fs'
import axPlugin from '@ax/eslint-plugin-ax'
import tseslint from 'typescript-eslint'   // omit this import if the project has no TS sources

const axConfig = JSON.parse(fs.readFileSync('./ax.config.json', 'utf8'))

export default [
  {
    files: [`${axConfig.react?.srcDir ?? 'src'}/**/*.{ts,tsx,js,jsx}`],
    languageOptions: { parser: tseslint.parser },   // omit this line too if no TS sources
    plugins: { ax: axPlugin },
    settings: { ax: axConfig.react },
    rules: axPlugin.configs.recommended.rules,
  },
]
```

`axPlugin.configs.recommended.rules` turns the full recommended set on (see
`practices-react/eslint-plugin-ax/README.md`) — this is the starting point, not an
optional extra. A `rules: {}` block wires the plugin into the config without
enabling a single rule, so ESLint reports `0 problems` even on a file that should
fail — indistinguishable from "clean." Narrow individual severities from the
recommended set afterward (e.g. `'ax/no-god-route': 'warn'`) if the project needs
a lighter starting point; never replace the object with a hand-picked list that
starts empty.

> ⚠️ **Never hardcode the `files` glob to a fixed `"src"` top-level directory.** If `react.srcDir` in `ax.config.json`
> is anything other than `src`, a hardcoded glob silently matches zero files —
> ESLint reports `0 problems` and it looks identical to "clean." Always
> parameterize the glob from `axConfig.react?.srcDir` with a `'src'` fallback, as
> shown above — never write the literal string `src/**` into the config.
>
> If `react.root` in `ax.config.json` is not the repo root (e.g. a monorepo
> package), read `ax.config.json` and resolve `eslint.config.mjs` paths relative
> to `react.root`, and say so explicitly in what you report back.

### 4. Non-vacuous verification (mandatory — do not skip)

A plugin that "installed successfully" and an `eslint.config.mjs` that "looks
right" are not evidence it is actually catching anything. Prove it:

1. Create a probe file in a shared-layer directory (one of
   `axConfig.react?.layers?.shared` entries — `lib` or `components` by default)
   with an import that reaches into a higher layer. The import target must use
   the project's actual `axConfig.react?.layers?.app[0]` directory name (or
   `layers?.features[0]`) — a hardcoded `app` will silently fail to trigger the
   rule on a custom layout. **If the project has TypeScript sources (Step 3
   above), the probe must also contain a TypeScript-only construct** — a bare
   ESM import/export is also valid plain JavaScript, so a probe without one
   would still parse and "pass" even if the TypeScript parser wiring is
   completely missing, which is exactly the blind spot this probe exists to
   catch:

   ```ts
   // <srcDir>/lib/__ax_probe.ts  (or <srcDir>/components/__ax_probe.ts)
   import { probe } from '../<layers.app[0]>/__ax_probe_target'
   export const __axProbe: string = probe
   ```

   The `: string` annotation is deliberate, not incidental — it is what forces
   a real TypeScript parser to be involved. If the project has no TypeScript
   sources, drop the annotation and use plain `export const __axProbe = probe`.

   (The target module does not need to exist — the rule classifies the import
   path lexically, it does not resolve the module on disk.)

2. Run the linter against just the probe:

   ```bash
   npx eslint <path-to-probe>
   ```

3. **Confirm the rule id `ax/no-upward-layer-import` appears in the output.**
   Only then is the gate proven live. **If the output is a parsing error
   instead** (e.g. mentioning the `: string` annotation or "Unexpected
   token"), that is a *different* failure signature — the TypeScript parser
   wiring from Step 3 (the `typescript-eslint` install / `languageOptions:
   { parser: tseslint.parser }` line) is missing or broken, not the ax
   plugin. Fix that wiring first, then re-run the probe from step 1 — the
   4-step diagnostic below assumes the file parsed and is the wrong tool for
   a parsing error.

4. **Delete the probe file** — it must not remain in the project.

If step 3 does not show `ax/no-upward-layer-import` (and the output is not a
parsing error — see above), work through this diagnostic order — do not guess
which layer failed:

1. **Plugin load** — `npx eslint --print-config <probe>` — does `ax` appear
   under `plugins` at all? If not, the `file:` install or the `import axPlugin`
   line is broken.
2. **`settings` injection** — is `settings: { ax: axConfig.react }` actually on
   the config block that matches the probe file? A `files` glob mismatch (see
   next step) silently means this block never applies.
3. **`files` glob matching** — `npx eslint --debug <probe> 2>&1 | grep -i lint` /
   `npx eslint <path> --format json | python3 -c "import json,sys; print(len(json.load(sys.stdin)), 'files linted')"`
   — if the linted-file count is `0`, the glob does not match the probe's actual
   path; check `axConfig.react?.srcDir` against the probe's real directory.
4. **Layout path mismatch** — do `react.srcDir` / `react.layers.shared` /
   `react.layers.app` in `ax.config.json` actually match real directories on
   disk? A layout that names a directory that doesn't exist yields correct-looking
   config that classifies nothing.

## What this skill does NOT do

- It does not choose which catalog rules to enable beyond the starting
  `recommended` set — that curation is `ax-practices`'s job (tag/impact
  selection) or the user's explicit preference.
- It does not modify `ax.config.json` — that is `ax-init-config`'s job.
- It does not wire git hooks — see `skills/ax-install-hooks/SKILL.md`.

## Self-check before reporting "react enforcement installed"

- [ ] `ax.config.json` existed (or `ax-init-config` was invoked and the run stopped there)
- [ ] The plugin path was `ls`-verified before `npm i -D file:...`
- [ ] If the project has TypeScript sources, `typescript-eslint` was installed and `languageOptions: { parser: tseslint.parser }` is on the same config block as the ax plugin
- [ ] `eslint.config.mjs`'s `files` glob is parameterized from `axConfig.react?.srcDir` — the literal string `src/**` does not appear anywhere in it
- [ ] `settings: { ax: axConfig.react }` is present on the block that matches the project's real source files
- [ ] The probe→detect→delete check ran, `ax/no-upward-layer-import` was observed in `npx eslint` output (not a parsing error), and the probe file was deleted afterward
- [ ] If detection failed, the failure signature was checked first (parsing error → Step 3 parser wiring; missing rule id → the 4-step diagnostic order below, not guessed)
