---
name: ax-install-hooks
description: >
  Wires a pre-commit hook in a downstream project to run THAT project's own
  lint/test, via core.hooksPath, husky, or lefthook. Does NOT copy ax-template's
  `.githooks/` directory and does NOT wire pre-push — ax-template's pre-push hook
  requires ax-template's own R25 completion-checklist audit log, which a
  downstream project does not have, and would permanently block every push. Use
  when a user asks to add a commit hook that runs the ax-install-react-enforcement
  / ax-install-java-enforcement gates automatically.
metadata:
  priority: 3
  tier: 1
  axis: consumption-channel
  docs:
    - "practices/scripts/install-hooks.sh"
    - "practices/scripts/HOOKS-GUIDE.md"
    - "skills/ax-install-react-enforcement/SKILL.md"
    - "skills/ax-install-java-enforcement/SKILL.md"
  pathPatterns:
    - 'skills/ax-install-hooks/SKILL.md'
    - '.githooks/pre-commit'
    - '.husky/pre-commit'
    - 'lefthook.yml'
  bashPatterns:
    - 'git config core.hooksPath'
    - 'npx husky init'
  importPatterns: []
retrieval:
  aliases:
    - ax-install-hooks
    - wire pre-commit hook
    - install git hooks
    - core.hooksPath
  intents:
    - "run lint/test automatically before every commit"
    - "wire a pre-commit hook for the ax react/java enforcement I just installed"
  entities:
    - core.hooksPath
    - husky
    - lefthook
    - pre-commit
    - R25
---

# ax-install-hooks

Wires a **pre-commit** hook for a downstream project. It does not touch pre-push.

> ⚠️ **Do not copy ax-template's `.githooks/` directory into a downstream
> project.** Its `pre-push` hook calls
> `practices/evals/completion_checklist_recency_guard.sh`, which requires **an R25
> completion-checklist audit log entry produced by ax-template's own
> `practices/scripts/verify-completion.sh`** for the current commit. A downstream
> project has neither `verify-completion.sh` nor a `verification-checklist.yaml`
> — that requirement can never be satisfied, so every `git push` in that project
> would be **permanently blocked**. If a copy has already happened, the fix is to
> remove the pasted `pre-push` hook (or unset `core.hooksPath`), not to try to
> satisfy it.

## What this skill wires: pre-commit only

Pick **one** of the three branches below based on what the project already uses
(check for `.husky/` or `lefthook.yml` first; default to `core.hooksPath` if
neither is present).

Before writing the commands, check `ax.config.json`'s `stacks` field to decide
which lines belong in the hook — `stacks` is an array (e.g. `["react", "java"]`),
not a boolean map: `"react"` in the array → the npm `lint`/`test` lines;
`"java"` in the array → a Gradle verification task line (`./gradlew
testPractices` below is the ax-template default — adjust the module path and
task name to whatever the target project actually registers); both present →
include both. Omit whichever block the project's `stacks` array doesn't contain.

### Branch A — `core.hooksPath` (no dependency, default recommendation)

```bash
mkdir -p .githooks
cat > .githooks/pre-commit <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
# Run THIS project's own checks — not ax-template's guards.

# react stack — omit this pair of lines entirely if "react" is not in
# ax.config.json's stacks array.
npm run lint --if-present
npm test --if-present

# java stack — omit this line entirely if "java" is not in ax.config.json's
# stacks array. "backend" and "testPractices" are the ax-template
# reference-workload defaults — before writing this file, replace both with
# the target project's actual Gradle module path (java.root, if the module
# isn't the repo root) and the verification task it registers (whatever
# /ax-install-java-enforcement wired there, or the project's own task name).
(cd backend && ./gradlew testPractices)
EOF
chmod +x .githooks/pre-commit
git config core.hooksPath .githooks
```

### Branch B — husky

```bash
npx husky init
cat > .husky/pre-commit <<'EOF'
# react stack — omit this pair of lines entirely if "react" is not in
# ax.config.json's stacks array.
npm run lint --if-present
npm test --if-present

# java stack — omit this line entirely if "java" is not in ax.config.json's
# stacks array. Same substitution rule as Branch A: swap "backend" /
# "testPractices" for the project's real module path and task name before
# writing this file.
(cd backend && ./gradlew testPractices)
EOF
```

### Branch C — lefthook

```yaml
# lefthook.yml
pre-commit:
  commands:
    lint: # react stack only — omit for java-only projects
      run: npm run lint --if-present
    test: # react stack only — omit for java-only projects
      run: npm test --if-present
    java-practices: # java stack only — omit this command entirely for react-only projects.
      # Same substitution rule as Branch A: "backend" / "testPractices" are
      # ax-template defaults — swap in the project's real module path and task.
      run: cd backend && ./gradlew testPractices
```

```bash
npx lefthook install
```

## What each branch actually runs

In every branch, the commands are **the target project's own** `lint`/`test`
scripts — including, if the user has installed them, the gates from
`skills/ax-install-react-enforcement/SKILL.md` (ESLint) and
`skills/ax-install-java-enforcement/SKILL.md` (`./gradlew testPractices`). This
skill does not invoke any `ax-template` script, guard, or checklist directly —
`practices/scripts/install-hooks.sh` is cited above as a reference for what
ax-template's own repo wires internally, but it is **not** meant to be run inside
a downstream project (it assumes ax-template's own `.githooks/` and
`verify-completion.sh` exist).

## Non-vacuous verification (mandatory — do not skip)

A hook file that "was written" and a `core.hooksPath`/husky/lefthook config
that "looks right" are not evidence git will actually invoke it, or that the
invocation blocks a bad commit. Prove it, the same way
`ax-install-react-enforcement` and `ax-install-java-enforcement` prove their
own gates: plant a real violation, attempt a real `git commit`, and require a
BLOCK before reporting success.

**Which probe to plant depends on what is actually wired in this project:**

1. **A stack-specific gate is already installed** (`ax-install-react-enforcement`
   and/or `ax-install-java-enforcement` ran in this project — check for
   `@ax/eslint-plugin-ax` in `package.json` / the `testPractices` task in the
   Gradle build). Reuse **that skill's own probe file, verbatim** — the
   upward-layer-import probe from `ax-install-react-enforcement` step 4, or the
   ArchUnit-violating probe class from `ax-install-java-enforcement` step 5. Do
   not invent a new violation shape here; the point is to prove the *whole*
   chain (`git commit` → hook → `npm run lint`/`./gradlew testPractices` →
   catalog rule), not a second, disconnected check.
2. **No stack-specific gate is installed yet** (the hook only wraps
   `npm run lint --if-present` / `npm test --if-present` with no scripts
   present, or the java line was omitted because `testPractices` doesn't
   exist) — there is nothing downstream for a probe to catch, so prove the
   *wiring itself* instead: temporarily append `exit 1` as the last line of
   the hook file that was just written (`.githooks/pre-commit`,
   `.husky/pre-commit`, or the `lefthook.yml` command block, matching whichever
   branch was wired), leaving everything above it untouched.

**Common procedure after the probe is in place:**

1. Stage a trivial, throwaway change (`git add` a scratch file, or
   `--allow-empty` if the probe is the appended `exit 1` and nothing else
   changed) and attempt `git commit -m "ax-install-hooks probe — expect BLOCK"`.
2. **Assert the commit was blocked**: nonzero exit code from `git commit`, and
   a recognizable message on stderr/stdout — the project's own lint/test
   failure output (case 1), or `husky - pre-commit hook exited with code 1` /
   the equivalent lefthook failure line / the plain shell error for
   `core.hooksPath` (case 2). `git log -1` must still show the prior commit,
   not the probe's.
3. **Remove the probe** — delete the stack-specific probe file (case 1) or the
   appended `exit 1` line (case 2) — and confirm a normal `git commit` now
   succeeds (GREEN).
4. Leave the repo clean: `git status` shows no probe residue, no stray staged
   files, and (case 2) the hook file's content is back to exactly what Branch
   A/B/C wrote.

**Report the BLOCK evidence** (the nonzero exit + the recognizable message)
when telling the user hooks are installed — "I wrote the hook" is not the same
claim as "I confirmed it blocks."

**If the commit SUCCEEDS instead of blocking, the wiring is vacuous — stop and
diagnose, in this order, before touching the probe again:**

1. **`core.hooksPath` not set / wrong path** — `git config --get core.hooksPath`
   must point at the directory actually containing the written hook (`.githooks`
   for Branch A; husky sets this to `.husky` itself during `npx husky init`,
   confirm it wasn't later overwritten; lefthook does not use
   `core.hooksPath` at all — confirm `npx lefthook install` actually ran and
   populated `.git/hooks/pre-commit` with a lefthook-generated file instead).
2. **Hook not executable** — `ls -l` the hook file; git silently skips a
   non-executable hook with no warning. Branch A's `chmod +x` step is not
   optional; husky/lefthook set this themselves on install, but confirm it
   after any manual edit to the file.
3. **Wrong stack detection** — does the hook file actually contain the line
   for the stack that was probed? A hook written before `ax.config.json`'s
   `stacks` array was checked (or written from the wrong branch) may be
   missing the exact command the probe was meant to trigger — re-check
   against the `stacks` array, not against what was assumed.
4. **(case 1 only) the underlying script is broken independent of the hook**
   — run the same `npm run lint` / `./gradlew testPractices` command directly,
   outside git, against the same probe. If it also passes there, the defect is
   in the stack-specific install (refer to that skill's own diagnostic order),
   not in this hook's wiring.

## pre-push: intentionally not wired

This skill does not configure a `pre-push` hook. If a downstream project wants
push-time enforcement, that is a decision for the project's own maintainers to
design against their own CI/test suite — not something this skill proposes,
since the only worked example on hand (ax-template's own) is coupled to
ax-template's R25 audit log and cannot be lifted as-is.

## Self-check before reporting "hooks installed"

- [ ] `.githooks/` was NOT copied wholesale from ax-template
- [ ] Exactly one of core.hooksPath / husky / lefthook was wired, matching what the project already had (or core.hooksPath if none)
- [ ] The wired hook runs the target project's own lint/test commands, not any ax-template script
- [ ] The java line (if `"java"` is in `stacks`) is a real, uncommented command with the module path/task name substituted for the project's actual values — not left as the ax-template placeholder or a bash comment
- [ ] The probe→BLOCK→clean check ran (stack-specific probe reused from `ax-install-react-enforcement`/`ax-install-java-enforcement` if one is installed, otherwise the appended-`exit 1` wiring probe), a real `git commit` was attempted, and the BLOCK evidence (nonzero exit + message) was captured
- [ ] The probe was removed afterward and a clean `git commit` was confirmed to succeed; `git status` shows no residue
- [ ] If the probe commit succeeded instead of blocking, the 4-step diagnostic order (hooksPath → executable bit → stack detection → underlying script) was followed, not guessed
- [ ] No `pre-push` hook was added by this skill
- [ ] The user was told, explicitly, why `.githooks/pre-push` cannot be copied (R25 recency guard requires ax-template's own audit log)
