# Claude Code hook integration for R25 completion contract

This guide is **advisory**. ax-template does not require any specific Claude
Code hook configuration — but if your team uses Claude Code, wiring the
R25 completion contract into hooks gives you mechanical enforcement at the
agent-turn boundary instead of only at git-commit / git-push time.

The R25 enforcement chain:

1. `practices/verification-checklist.yaml` — the contract
2. `practices/scripts/verify-completion.sh` — the executor
3. `practices/evals/completion_checklist_recency_guard.sh` — the 49th audit guard
4. `.githooks/pre-push` — already wired to call the 49th guard

What this guide adds: making Claude Code itself run the loop at agent turn
boundaries, so the AI cannot "declare done" without the contract being green.

---

## Option A — repo-local settings (recommended)

Add to `.claude/settings.json` at the repo root (gitignored OR shared, your
choice):

```json
{
  "hooks": {
    "Stop": [
      {
        "command": "bash practices/scripts/verify-completion.sh --json",
        "description": "R25: enforce completion contract on every agent stop"
      }
    ]
  }
}
```

The Stop hook fires when the agent declares its turn complete. If
`verify-completion.sh` exits non-zero, Claude Code surfaces the failure
back into the conversation, and the agent must keep working.

## Option B — auto-run on file edits (heavier, faster signal)

Use this when you want sub-second feedback inside the agent loop:

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "command": "bash practices/scripts/verify-completion.sh --step hard-guards --json",
        "description": "R25: re-run guards (no gradle) after every edit"
      }
    ]
  }
}
```

`--step hard-guards` runs only the guard suite (fast, seconds) rather than
the full gradle matrix (minutes). The agent gets immediate signal on
catalog-rule violations without paying the per-edit gradle cost.

## Option C — user-global enforcement

If you want every Claude Code session in this repo to honor the contract,
put the hook in `~/.claude/settings.json` (not just the repo). Same shape
as Option A.

---

## Why this is advisory, not part of the catalog hard gates

The catalog enforces the contract at **git-push time** via the 49th hard
guard. That is the load-bearing mechanism — it works for every fork-receiver
regardless of editor, agent, or workflow. The Claude Code hook above is a
**faster feedback loop** for the AI agent during work, not an additional
gate. A fork-받은 팀 that doesn't use Claude Code loses nothing; their
git-push still trips the 49th guard.

## Verifying your hook installation

```bash
# 1. Generate one passing run to populate the audit log
bash practices/scripts/verify-completion.sh

# 2. Confirm the 49th guard reads it as PASS
bash practices/evals/completion_checklist_recency_guard.sh
# → expect: {"signal":"completion_checklist.recency_pass",...}

# 3. Make a no-op commit to advance HEAD
git commit --allow-empty -m "test: R25 hook smoke"

# 4. Confirm the 49th guard now reports STALE
bash practices/evals/completion_checklist_recency_guard.sh
# → expect exit 1, VIOLATION code AUDIT_STALE_HEAD

# 5. Re-run verify-completion.sh and confirm PASS again
bash practices/scripts/verify-completion.sh
bash practices/evals/completion_checklist_recency_guard.sh
# → expect exit 0
```
