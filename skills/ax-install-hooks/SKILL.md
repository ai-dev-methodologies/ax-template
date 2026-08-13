---
name: ax-install-hooks
description: >
  Wires a pre-commit hook in a downstream project to run THAT project's own
  lint/test, via core.hooksPath (worktree-aware), husky, or lefthook. Does NOT
  copy ax-template's `.githooks/` directory and does NOT wire pre-push --
  ax-template's pre-push hook needs its own R25 audit log, which a downstream
  project lacks and would permanently block every push. Use when a user asks
  to add a commit hook running ax-install-react-enforcement /
  ax-install-java-enforcement gates automatically, including in a worktree.
metadata:
  priority: 3
  tier: 1
  axis: consumption-channel
  docs:
    - "skills/ax-install-react-enforcement/SKILL.md"
    - "skills/ax-install-java-enforcement/SKILL.md"
  pathPatterns:
    - 'skills/ax-install-hooks/SKILL.md'
    - '.githooks/pre-commit'
    - '.husky/pre-commit'
    - 'lefthook.yml'
  bashPatterns:
    - 'git config core.hooksPath'
  importPatterns: []
retrieval:
  aliases:
    - ax-install-hooks
    - wire pre-commit hook
    - install git hooks
  intents:
    - "run lint/test automatically before every commit"
    - "install hooks in a linked git worktree without breaking sibling worktrees"
  entities:
    - core.hooksPath
    - extensions.worktreeConfig
---

# ax-install-hooks

Wires a **pre-commit** hook for a downstream project. It does not touch pre-push.

> ⚠️ **Do not copy ax-template's `.githooks/` directory into a downstream
> project.** Its `pre-push` hook requires an R25 audit-log entry from
> ax-template's own `verify-completion.sh` — a downstream project has neither,
> so `git push` there would be **permanently blocked**. If a copy already
> happened, remove the pasted `pre-push` hook instead of trying to satisfy it.

## What this skill wires: pre-commit only

Pick **one** of the three branches below (check for `.husky/` or `lefthook.yml`
first; default to `core.hooksPath` if neither present). Before writing
anything, check `ax.config.json`'s `stacks` array — delete the react block
below if `"react"` is absent, the java block if `"java"` is absent. The hook
body itself also reads `react.root`/`java.root`/`java.rootPackage` from
`ax.config.json` at commit time, instead of hardcoding a path.

### Hook body — shared verbatim by all three branches (D-9 path-scope, D-10 config-driven)

Write this exact script into whichever file your branch names (A:
`.githooks/pre-commit`; B: `.husky/pre-commit`; C: a helper script referenced
from `lefthook.yml`), always as a **top-level heredoc** — never nested inside
`$(...)`, which is what trips the stock-bash-3.2 apostrophe-parity parser bug
(P2-78); a top-level heredoc is immune to it regardless of comment wording.

```bash
#!/usr/bin/env bash
set -euo pipefail
# ax-installed pre-commit gate (config-driven, path-scoped) -- see
# skills/ax-install-hooks/SKILL.md.
CONFIG="ax.config.json"
[ -f "$CONFIG" ] || { echo "ax-hook: $CONFIG missing -- cannot resolve react.root/java.root" >&2; exit 1; }

# python3 preferred; sed fallback is bash-3.2 compatible (no heredoc -- P2-78).
if command -v python3 >/dev/null 2>&1; then
  REACT_ROOT="$(python3 -c "import json
d = json.load(open('$CONFIG'))
print(d.get('react', {}).get('root', '') or '')" 2>/dev/null)"
  JAVA_ROOT="$(python3 -c "import json
d = json.load(open('$CONFIG'))
print(d.get('java', {}).get('root', '') or '')" 2>/dev/null)"
  JAVA_ROOT_PACKAGE="$(python3 -c "import json
d = json.load(open('$CONFIG'))
print(d.get('java', {}).get('rootPackage', '') or '')" 2>/dev/null)"
else
  REACT_ROOT="$(sed -n 's/.*"react"[[:space:]]*:[[:space:]]*{[^}]*"root"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$CONFIG" | head -1)"
  # java.root/java.rootPackage: isolate the "java" key's own line first, then
  # extract each field directly off that isolated line (no [^}]* bridging) --
  # the old single-sed bridge pattern silently failed to match whenever a
  # nested object sat between "{" and the target field on the same line.
  # This still assumes the "java" object is written on ONE line (true for
  # ax.config.sample.json's convention); a hand-formatted multi-line "java"
  # block will not match here -- python3 above has no such limit, so this
  # branch is only a fallback for python3-less environments.
  JAVA_LINE="$(grep '"java"[[:space:]]*:' "$CONFIG" | head -1 || true)"
  JAVA_ROOT="$(printf '%s' "$JAVA_LINE" | sed -n 's/.*"root"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
  JAVA_ROOT_PACKAGE="$(printf '%s' "$JAVA_LINE" | sed -n 's/.*"rootPackage"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
fi
# react -- delete this block if "react" is not in ax.config.json's stacks[].
# Unresolved root is a config defect -- fail loud, never silently default.
[ -n "$REACT_ROOT" ] || { echo "ax-hook: react.root not resolved from $CONFIG" >&2; exit 1; }
REACT_TOUCHED=0
if [ "$REACT_ROOT" = "." ]; then
  git diff --cached --name-only | grep -qE '\.(tsx?|jsx?|css|scss)$' && REACT_TOUCHED=1
else
  git diff --cached --name-only | grep -q "^${REACT_ROOT}/" && REACT_TOUCHED=1
fi
[ "$REACT_TOUCHED" = 1 ] && ( cd "$REACT_ROOT" && npm run lint --if-present && npm test --if-present )  # cd subshell keeps cwd clean for the java block below; && (not ;) propagates a lint failure instead of letting the last command's status hide it (#79)
# java -- delete this block if "java" is not in ax.config.json's stacks[].
# "testPractices" is the ax-template default task; swap for the project's real one.
[ -n "$JAVA_ROOT" ] || { echo "ax-hook: java.root not resolved from $CONFIG" >&2; exit 1; }
[ -n "$JAVA_ROOT_PACKAGE" ] || { echo "ax-hook: java.rootPackage not resolved from $CONFIG" >&2; exit 1; }
JAVA_TOUCHED=0
if [ "$JAVA_ROOT" = "." ]; then
  git diff --cached --name-only | grep -qE '\.java$' && JAVA_TOUCHED=1
else
  git diff --cached --name-only | grep -q "^${JAVA_ROOT}/" && JAVA_TOUCHED=1
fi
[ "$JAVA_TOUCHED" = 1 ] && ( cd "$JAVA_ROOT" && ./gradlew testPractices -PaxRootPackage="$JAVA_ROOT_PACKAGE" )  # -P is mandatory: without it ArchUnit falls back to build.gradle.kts's generic default package, scans 0 real classes, and PASSes silently even with real violations present (F-024 / #86)
exit 0   # a legitimate skip must not leak the last test's own nonzero status
```

No staged files under either root (e.g. a Python-only commit) → both blocks
skip, hook exits 0 — intended PLUGIN-CHANNEL rule-6 behavior ("root 밖 파일에
카탈로그 미적용"), not a bug. The `"."`-root case uses a file-extension proxy
instead of a path prefix, since a prefix match against `.` matches everything.

### Branch A — `core.hooksPath` (no dependency, default recommendation)

#### A0. Detect the repository shape first (D-8 / F-2)

```bash
[ -f .git ] && echo "linked worktree" || echo "normal checkout / main worktree"
```

**Directory (normal checkout):** `git config core.hooksPath .githooks` — no preflight needed.

**File (linked worktree):** that plain form writes into the repo's SHARED
config and silently re-points hooksPath for **every sibling worktree**. Use
`--worktree` instead — but first run this preflight (F-2: skipping it makes
every worktree's `git status`/`add`/`commit` die the instant `worktreeConfig`
is enabled, `fatal: this operation must be run in a work tree` — confirmed
live against a bare-main + worktree-farm layout):

```bash
git config --get core.bare; git config --get core.worktree
# If EITHER prints a value, core.bare still lives in the SHARED config --
# migrate it into config.worktree BEFORE enabling worktreeConfig (git's own
# documented procedure; --git-common-dir resolves correctly whether main is
# bare or normal):
git config --unset core.bare
git config -f "$(git rev-parse --git-common-dir)/config.worktree" core.bare true
git rev-parse --is-bare-repository   # must print "true" -- confirm before continuing

# Only now (or if neither core.bare nor core.worktree was set to begin with):
git config extensions.worktreeConfig true
git config --worktree core.hooksPath .githooks
```

#### A1. Write the hook and make it executable

```bash
mkdir -p .githooks
cat > .githooks/pre-commit <<'EOF'
# — paste the shared hook body above, minus whichever stack block
#   ax.config.json's stacks array excludes —
EOF
chmod +x .githooks/pre-commit
# then wire core.hooksPath per whichever A0 branch applied, above
```

#### A2. Sibling non-interference (mandatory whenever A0 took the worktree branch — D-8 / F-3)

"The sibling's `core.hooksPath` reads empty" does NOT hold when the shared
config already had a value before this skill ran (confirmed live) — the
sibling still resolves that pre-existing value. Use both instead: (1) **value
+ origin unchanged** — `git config --show-origin --get core.hooksPath` from a
*different* worktree, before and after wiring, diffed byte-for-byte (collapses
to "no output either time" when the sibling had none — same rule, not a
separate one); (2) **exactly one new `config.worktree`** — `ls
"$(git rev-parse --git-common-dir)/worktrees/"*/config.worktree` shows only
the target worktree's own new file, no sibling gained one.

### Branch B — husky

Same hook body as Branch A, pasted unedited into `.husky/pre-commit` (shebang
included, react/java blocks trimmed per stacks). husky never touches the
shared `core.hooksPath`, so the D-8/F-2 worktree preflight does not apply here.

```bash
npx husky init
cat > .husky/pre-commit <<'EOF'
# — paste the shared hook body above —
EOF
```

### Branch C — lefthook

Same hook body as Branch A, written to a helper script `lefthook.yml` invokes
(`run:` takes one command, not an inline multi-line script):

```bash
mkdir -p .githooks
cat > .githooks/ax-pre-commit-checks.sh <<'EOF'
# — paste the shared hook body above —
EOF
chmod +x .githooks/ax-pre-commit-checks.sh
npx lefthook install
```

```yaml
# lefthook.yml
pre-commit:
  commands:
    ax-gates:
      run: bash .githooks/ax-pre-commit-checks.sh
```

**What each branch actually runs:** the target project's own `lint`/`test`/
`gradlew` commands — including, if installed, `ax-install-react-enforcement`'s
ESLint gate and `ax-install-java-enforcement`'s `./gradlew testPractices`. No
`ax-template` script is invoked directly — `practices/scripts/install-hooks.sh`
is only ax-template's own internal wiring (no path-scoping or worktree
awareness there; both are this skill's additions for downstream use).

## Non-vacuous verification (mandatory — do not skip)

A hook that "was written" and a config that "looks right" are not evidence git
invokes it, blocks a bad commit, or skips an out-of-scope one. Prove all
three, as `ax-install-react-enforcement`/`ax-install-java-enforcement` prove
their own gates.

**Probe choice:** stack-specific gate already installed → reuse that skill's own
probe file verbatim, staged under `react.root`/`java.root` so the path-scope check
also fires (don't invent a second, disconnected check). No stack-specific gate yet →
prove the *wiring*: append `exit 1` as the hook's last line, temporarily.

**Procedure:** (1) stage a throwaway change under the scoped root, attempt
`git commit -m "ax-install-hooks probe — expect BLOCK"`, assert nonzero exit
**and** the gate's own signal in the output — `ax/*` rule id for react, probe
class/test name for java (exit code alone is insufficient: a wiring defect
also exits nonzero, #85); (2) remove the probe, confirm a normal `git commit`
(same scoped path) succeeds; (3) **scope-skip, both directions (D-9)** —
stage a throwaway change OUTSIDE both roots (or with neither extension, when
a root is `.`) and confirm `git commit` succeeds WITHOUT `npm`/`gradlew`
output, i.e. a scoped-out commit passes cleanly and not "by accident because
nothing was staged"; (4) `git status` clean afterward — no probe residue,
hook restored exactly. Report the BLOCK, PASS, AND scope-skip evidence — "I
wrote the hook" ≠ "I confirmed it blocks the right things and skips the rest."

**If the commit succeeds instead of blocking, diagnose in order:** (1)
`git config --get core.hooksPath` (`--worktree` for a worktree install) points
at the hook's directory; (2) hook not executable — `ls -l`, git silently
skips one; (3) `ax.config.json` unresolved — did the hook print its own "not
resolved" error and exit 1 (config defect, not a probe failure)? (4)
wrong stack/path-scope — does the probe path fall under the root checked,
per the real `stacks`/root values? (5) stack-specific probe only — run
`npm run lint`/`./gradlew testPractices` directly against the probe, outside
git; if that also passes, the defect is in that stack's install, not the hook.

## pre-push: intentionally not wired

This skill does not configure a `pre-push` hook — that is a decision for the
project's own maintainers against their own CI/test suite; ax-template's own
example is coupled to its R25 audit log and cannot be lifted as-is.

## Self-check before reporting "hooks installed"

- [ ] `.githooks/` was NOT copied wholesale from ax-template; no `pre-push` hook was added by this skill
- [ ] Exactly one of core.hooksPath / husky / lefthook was wired, matching the project; `.git` was checked (file vs directory) first, and if a file, `--worktree` + the F-2 preflight + the A2 sibling non-interference check all ran and passed
- [ ] The hook reads `react.root`/`java.root`/`java.rootPackage` from `ax.config.json` at commit time — no hardcoded `cd backend`/`cd frontend` placeholder remains, and the react/java blocks present match exactly the `stacks` array (the other block deleted, not commented out)
- [ ] The java block's `./gradlew testPractices` call passes `-PaxRootPackage="$JAVA_ROOT_PACKAGE"` — a `-P`-less invocation lets ArchUnit fall back to the build file's generic default package and PASS silently on real violations (F-024 / #86); `JAVA_ROOT_PACKAGE` unresolved fails loud (`exit 1`), same as `JAVA_ROOT`
- [ ] The probe→BLOCK→clean→scope-skip check ran (both in-scope and out-of-scope `git commit`), all evidence captured, the probe removed, and `git status` shows no residue
- [ ] If the commit succeeded instead of blocking, the 5-step diagnostic order was followed, not guessed
- [ ] The user was told, explicitly, why `.githooks/pre-push` cannot be copied (R25 recency guard requires ax-template's own audit log)
