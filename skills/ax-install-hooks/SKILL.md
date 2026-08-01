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

### Branch A — `core.hooksPath` (no dependency, default recommendation)

```bash
mkdir -p .githooks
cat > .githooks/pre-commit <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
# Run THIS project's own checks — not ax-template's guards.
npm run lint --if-present
npm test --if-present
EOF
chmod +x .githooks/pre-commit
git config core.hooksPath .githooks
```

### Branch B — husky

```bash
npx husky init
cat > .husky/pre-commit <<'EOF'
npm run lint --if-present
npm test --if-present
EOF
```

### Branch C — lefthook

```yaml
# lefthook.yml
pre-commit:
  commands:
    lint:
      run: npm run lint --if-present
    test:
      run: npm test --if-present
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
- [ ] No `pre-push` hook was added by this skill
- [ ] The user was told, explicitly, why `.githooks/pre-push` cannot be copied (R25 recency guard requires ax-template's own audit log)
