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
identical to "the project is clean." Step 5 below exists specifically to rule that
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

<!-- ax:artifact id=react-plugin-dep path=- kind=command base=repo substs=env.axPluginPath -->
```bash
# The plugin path is supplied by the install environment, not by ax.config.json: which of the two
# locations above exists depends on how this skill is being run, not on the consuming project.
# ax:subst env.axPluginPath
AX_PLUGIN_PATH="@@env.axPluginPath@@"
ls "$AX_PLUGIN_PATH/package.json"   # confirm before installing -- do not assume the path resolves
npm i -D "file:$AX_PLUGIN_PATH"
```

`npm install file:<dir>` creates a symlink, so this works without publishing to
npm. It also means the plugin's own `package.json` `files` allowlist is bypassed
for local installs — this is expected in this channel, not a bug to fix here.

### 3. Prescribe the `lint` (and `test`) npm scripts

The pre-commit hook from `ax-install-hooks` runs the project's **own** `npm run lint`. Until this
step, no ax skill ever created that script — and `npm run lint --if-present` against a project with
no `lint` script exits **0 with zero bytes of output**, so the react gate looked green while it had
never executed a single rule (F-031). The hook therefore no longer uses `--if-present`: it names a
missing script and fails. Merge these into `<react.root>/package.json`:

<!-- ax:artifact id=react-lint-script path=package.json kind=file-fragment base=react.root merge=json-deep -->
```json5
{
  "scripts": {
    "lint": "eslint . --max-warnings 0",
    "test": "echo 'no frontend test suite wired yet -- see ax-install-react-enforcement' >&2"
  }
}
```

`--max-warnings 0` is deliberate: the ax recommended set ships some rules at `warn`, and a warning
that never fails a build is not a gate. The `test` entry is a **placeholder to be replaced** with
the project's real runner (`vitest run`, `jest`, …). If the project genuinely has no frontend test
suite, leave the placeholder: it exits 0 and prints one visible line, so "this project chose not to
run frontend tests" is recorded in `package.json` where a reviewer can see it — which is exactly
what `--if-present`'s invisible zero-output skip could not express.

### 4. Wire `eslint.config.mjs`

**First, check whether the project has TypeScript**: a `tsconfig.json` at or above
`axConfig.react?.root`, or `.ts`/`.tsx` files under `axConfig.react?.srcDir`. Record the answer in
`ax.config.json` as `react.typescript` (boolean) — the two TypeScript-only lines in the config
below are gated on exactly that key, so the decision is made once, in config, rather than being
re-derived by prose at every install (F-033). When it is true, install `typescript-eslint` and wire
its parser **on the same config block** as the ax plugin — this is not optional and must not be
deferred:

<!-- ax:artifact id=react-ts-eslint-dep path=- kind=command base=react.root when=config.react.typescript -->
```bash
npm i -D typescript-eslint
```

**Why this is mandatory, not a nicety**: with ESLint's default parser (`espree`), every `.ts`/
`.tsx` file that uses TypeScript-only syntax throws a parsing error and is skipped for *every*
rule — not just ax's. A run that reports `0 problems` because every `.ts` file failed to parse
looks identical, at a glance, to a run that reports `0 problems` because the project is clean.
Skipping the parser wiring means every ax rule silently never executes on `.ts`/`.tsx` files.

**The non-TypeScript path is not "install nothing" — it needs its own `languageOptions`** (F-035).
When `react.typescript` is false or absent, the `typescript-eslint` install above and the
`import tseslint` line drop out, and ESLint falls back to its default parser, `espree`. **espree
does not enable JSX unless it is told to.** A config that merely omits the TypeScript lines omits
`languageOptions` entirely, and then every `.jsx` file in the project dies at parse time:

```text
{"ruleId":null,"fatal":true,"severity":2,"message":"Parsing error: Unexpected token <","line":2}
```

That is not a rule reporting nothing — it is the file never being read, so not one of the 15
recommended `ax/*` rules executes on any component in the project. The `ax:else` branch in the
config below therefore carries real content, `parserOptions: { ecmaFeatures: { jsx: true } }`,
rather than being empty. Measured on ESLint 9.39 + espree: with it, `.jsx` parses and both
import-path rules (`ax/no-upward-layer-import`) and JSX-AST rules (`ax/no-falsy-numeric-render`)
fire; without it, every `.jsx` is a fatal parse error.

Setting `ecmaFeatures.jsx` on the **TypeScript** branch instead of branching would be harmless but
wrong-shaped: measured on typescript-eslint, a `.ts` file containing the angle-bracket assertion
`<string>raw` parses identically with and without it, because that parser takes JSX from the file
extension (`.tsx` yes, `.ts` no) and not from `ecmaFeatures`. The two branches are separate because
they need *different parsers*, not because one of them is dangerous.

<!-- ax:artifact id=react-eslint-config path=eslint.config.mjs kind=file base=react.root merge=replace -->
```js
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import axPlugin from '@ax/eslint-plugin-ax'
// ax:if config.react.typescript   (the typescript-eslint parser is wired only for a TS project)
import tseslint from 'typescript-eslint'
// ax:endif

// F-030: this file is NOT always loaded from the repo root. The pre-commit hook runs
// `npm run lint` with cwd = react.root, so a hardcoded './ax.config.json' resolves to
// <react.root>/ax.config.json and dies with ENOENT. A fixed '../ax.config.json' is equally wrong:
// it breaks whenever react.root is '.', where ax.config.json is a sibling and not a parent.
// Search UPWARD from this file's own directory instead, and throw when nothing is found -- a
// silent fallback here would leave the whole gate unconfigured while still reporting 0 problems.
function findAxConfig(startDir) {
  let dir = startDir
  for (;;) {
    const candidate = path.join(dir, 'ax.config.json')
    if (fs.existsSync(candidate)) return candidate
    const parent = path.dirname(dir)
    if (parent === dir) {
      throw new Error(
        `ax.config.json not found in ${startDir} or any ancestor directory -- eslint.config.mjs ` +
          'cannot build its files glob without react.srcDir. Run ax-init-config ' +
          '(see skills/ax-init-config/SKILL.md).'
      )
    }
    dir = parent
  }
}

const axConfig = JSON.parse(
  fs.readFileSync(findAxConfig(path.dirname(fileURLToPath(import.meta.url))), 'utf8')
)
const srcDir = axConfig.react?.srcDir
if (!srcDir) {
  throw new Error(
    "ax.config.json is missing react.srcDir -- the ESLint 'files' glob cannot be built " +
      "without it (a silent 'src' default would match zero files on any layout that " +
      'differs, and report 0 problems indistinguishably from "clean"). Run ax-init-config ' +
      '(see skills/ax-init-config/SKILL.md) or add react.srcDir to ax.config.json manually.'
  )
}

export default [
  {
    files: [`${srcDir}/**/*.{ts,tsx,js,jsx}`],
    // ax:if config.react.typescript   (typescript-eslint reads JSX from the .tsx extension itself)
    languageOptions: { parser: tseslint.parser },
    // ax:else   (espree parses no JSX at all unless ecmaFeatures.jsx is set -- see F-035)
    languageOptions: { parserOptions: { ecmaFeatures: { jsx: true } } },
    // ax:endif
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

> ⚠️ **Never hardcode the `files` glob to a fixed `"src"` top-level directory, and never
> silently default `react.srcDir` to `'src'` either.** A `?? 'src'` fallback is the same
> class of defect as F-024/#86's `-P`-less java hook invocation — a config value that
> failed to resolve gets papered over with a generic default instead of failing loud, so
> the gate goes silently vacuous (glob matches zero files on any layout that isn't
> literally `src`; ESLint reports `0 problems`, indistinguishable from "clean"). Always
> resolve `axConfig.react?.srcDir` explicitly and `throw` when it is missing, as shown
> above — never write the literal string `src/**` into the config, and never fall back to
> a default value for an unresolved `react.srcDir`.
>
> If `react.root` in `ax.config.json` is not the repo root (e.g. a monorepo
> package), read `ax.config.json` and resolve `eslint.config.mjs` paths relative
> to `react.root`, and say so explicitly in what you report back.

### 5. Non-vacuous verification (mandatory — do not skip)

A plugin that "installed successfully" and an `eslint.config.mjs` that "looks
right" are not evidence it is actually catching anything. Prove it:

1. Create a probe file in a shared-layer directory (one of
   `axConfig.react?.layers?.shared` entries — `lib` or `components` by default)
   with an import that reaches into a higher layer. The import target must use
   the project's actual `axConfig.react?.layers?.app[0]` directory name (or
   `layers?.features[0]`) — a hardcoded `app` will silently fail to trigger the
   rule on a custom layout.

   **The probe must also exercise the parser this project actually configured**
   — a bare ESM import/export is valid plain JavaScript under *every* parser, so
   a probe built only out of one proves the plugin loaded and proves nothing at
   all about parsing. That blind spot is not hypothetical: it is exactly how
   F-035 shipped. The non-TypeScript probe used to be a JSX-free
   `import`/`export const`, which parses fine under a bare `espree` with no
   `languageOptions` — so this step reported the gate live while every real
   `.jsx` component in the project was a fatal parse error. **A React probe that
   contains no JSX does not test a React project.** Use the file matching this
   project's `react.typescript`:

   **TypeScript project** — `.tsx`, carrying both a type annotation and JSX:

   ```tsx
   // <srcDir>/lib/__ax_probe.tsx  (or <srcDir>/components/__ax_probe.tsx)
   import { probe } from '../<layers.app[0]>/__ax_probe_target'
   const label: string = probe
   export const __axProbe = <span>{label}</span>
   ```

   **Non-TypeScript project** — `.jsx`, carrying JSX (no annotation):

   ```jsx
   // <srcDir>/lib/__ax_probe.jsx  (or <srcDir>/components/__ax_probe.jsx)
   import { probe } from '../<layers.app[0]>/__ax_probe_target'
   export const __axProbe = <span>{probe}</span>
   ```

   Neither construct is incidental. The `: string` annotation is what forces a
   real TypeScript parser to be involved; the `<span>` is what forces JSX to be
   enabled. Drop either one and the probe passes on a config that cannot read
   the project's own source files.

   (The target module does not need to exist — the rule classifies the import
   path lexically, it does not resolve the module on disk.)

2. Run the linter against just the probe:

   ```bash
   npx eslint <path-to-probe>
   ```

3. **Confirm the rule id `ax/no-upward-layer-import` appears in the output.**
   Only then is the gate proven live. **If the output is a parsing error
   instead**, that is a *different* failure signature — a `languageOptions`
   defect in Step 4, not the ax plugin — and the two spellings tell you which
   half:

   | Parsing error mentions | Missing wiring |
   |---|---|
   | `Unexpected token <` (the `<span>`) | JSX. On a TS project, `typescript-eslint` / `languageOptions: { parser: tseslint.parser }`; on a non-TS project, `languageOptions: { parserOptions: { ecmaFeatures: { jsx: true } } }` (F-035) |
   | `Unexpected token :` (the `: string`) | The TypeScript parser — `typescript-eslint` is not installed or `parser: tseslint.parser` is not on this config block |

   Fix that wiring first, then re-run the probe from step 1 — the 4-step
   diagnostic below assumes the file parsed and is the wrong tool for a
   parsing error.

4. **Delete the probe file** — it must not remain in the project.

**If `npx eslint` throws immediately** mentioning `react.srcDir` before it even
attempts to lint the probe, that is Step 4's fail-loud guard firing — fix
`ax.config.json`'s `react.srcDir`, this is not a glob/settings defect and the
4-step diagnostic below is the wrong tool for it.

If step 3 does not show `ax/no-upward-layer-import` (and the output is not a
parsing error or the srcDir throw above), work through this diagnostic order
— do not guess which layer failed:

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
- [ ] `<react.root>/package.json` now has a real `lint` script (`eslint . --max-warnings 0`) and a `test` script — without them the pre-commit hook has nothing to run and, before F-031, `--if-present` made that absence indistinguishable from a pass
- [ ] `eslint.config.mjs` locates `ax.config.json` by searching **upward from its own directory** (`import.meta.url`), not via `'./ax.config.json'` or a fixed `'../ax.config.json'` — the hook lints with cwd = `react.root` (F-030), and a not-found config throws rather than defaulting
- [ ] `languageOptions` is present on the ax config block **either way** — `{ parser: tseslint.parser }` (with `typescript-eslint` installed) when `react.typescript` is true, `{ parserOptions: { ecmaFeatures: { jsx: true } } }` when it is false/absent. A block with no `languageOptions` at all is the F-035 shape: espree reads no JSX, every `.jsx` is a fatal parse error, and zero `ax/*` rules ever run
- [ ] The probe file contained JSX (`<span>…</span>`), and on a TypeScript project a `: string` annotation as well — a JSX-free probe passes against a config that cannot parse a single real component, which is how F-035 shipped
- [ ] `eslint.config.mjs`'s `files` glob is parameterized from `axConfig.react?.srcDir` — the literal string `src/**` does not appear anywhere in it, and an unresolved `react.srcDir` throws instead of silently defaulting to `'src'`
- [ ] `settings: { ax: axConfig.react }` is present on the block that matches the project's real source files
- [ ] The probe→detect→delete check ran on a **`.jsx`/`.tsx`** probe, `ax/no-upward-layer-import` was observed in `npx eslint` output (not a parsing error), and the probe file was deleted afterward
- [ ] If detection failed, the failure signature was checked first (parsing error → Step 4 parser wiring; missing rule id → the 4-step diagnostic order below, not guessed)
