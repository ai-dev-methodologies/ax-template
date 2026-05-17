---
name: ax-guard-time-decay
description: >
  Tier-3 time-decay guard wrapper. Thin skill wrapper around practices/evals/time_decay_guard.sh.
  Checks fetched_at timestamps on all upstream snapshots; fails if any snapshot exceeds 90 days.
  Prevents catalog drift from stale external references. Invoked by Tier-2 only — NOT pathPattern-triggered.
metadata:
  priority: 4
  tier: 3
  axis: concern
  docs: []
  pathPatterns: []
  bashPatterns:
    - 'bash practices/evals/time_decay_guard.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-guard-time-decay
    - time decay guard
    - snapshot freshness
    - upstream snapshot age
    - stale snapshot
  intents:
    - check snapshot freshness
    - verify upstream snapshots are not stale
    - guard against time-decayed references
  entities:
    - time_decay_guard.sh
    - fetched_at
    - upstream snapshot
    - 90 days threshold
---

# ax-guard-time-decay

Tier-3 time-decay guard wrapper. Wraps `practices/evals/time_decay_guard.sh`.
Every upstream snapshot in `practices/upstream/` and `practices-react/upstream/`
carries a `fetched_at: YYYY-MM-DD` field. If any snapshot is older than 90 days
from today, this guard fails — the rule anchored to that snapshot may be
referencing outdated information.

NOT pathPattern-triggered. Invoked exclusively by Tier-2 skills.

## Workflow checklist (copyable per Anthropic best-practices)

- [ ] Step 1: Run `bash practices/evals/time_decay_guard.sh`
- [ ] Step 2: Read output — violations are `STALE_SNAPSHOT: <file> (age: <N> days)`
- [ ] Step 3: Re-fetch the stale snapshot using the source URL in the snapshot file
- [ ] Step 4: Update `fetched_at` to today's date
- [ ] Step 5: Re-run guard — confirm exit 0

## Steps detail

### Step 1: time_decay_guard.sh
Script: `practices/evals/time_decay_guard.sh`.
No args required for full scan. Optional `<scope-path>` to scope to a sub-directory
(e.g., `practices-react/upstream/` only).
Reads `fetched_at:` from each `*.snapshot.md` file.
Computes age in days from today's date (system clock).
Threshold: 90 days. Configurable via `TIME_DECAY_THRESHOLD_DAYS` env var.
Exit 0 = all snapshots fresh; non-zero = first stale snapshot path + age on stderr.

### Step 3: Re-fetch
Each snapshot file contains a `source_url:` field. Fetch the current content from
that URL, update the snapshot file content, and update `fetched_at` to today.
The snapshot format (frontmatter + extracted content) is documented in
`practices/upstream/_MANIFEST.yaml`.

## Bundled scripts
- `skills/ax-guard-time-decay/scripts/run.sh` — thin wrapper; passes args to `time_decay_guard.sh`; exits with guard's exit code

## Feedback loop
When guard fails: the stale snapshot's `source_url` points to the external source.
Re-fetch, update the file, re-run the guard on that single file:
`bash practices/evals/time_decay_guard.sh <file>`.
If the source URL is no longer valid: update the snapshot with the new URL and
file an ADR entry noting the URL change.

## Invocation graph
- Calls: `practices/evals/time_decay_guard.sh`
- Called by (Tier-2): `ax-verify-react`, `ax-verify-shared`, `ax-verify-L1`

## Acceptance (binary)
```bash
bash practices/evals/time_decay_guard.sh
# Expected: exit 0
```
