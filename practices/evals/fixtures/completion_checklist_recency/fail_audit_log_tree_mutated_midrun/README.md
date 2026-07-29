# fail_audit_log_tree_mutated_midrun

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape (cross-family review P1, 2026-07-29 — *a run is not an instant*):

- The latest `.ax-verify/runs.jsonl` line satisfies every endpoint check:
  `head_sha` matches `expected_head.txt`, `exit` 0, `hard_fail` 0,
  `full_run` true, a usable `tree_fingerprint`, and **both** `tree_clean` and
  `tree_clean_end` are `true` with `head_sha_end` / `tree_fingerprint_end`
  identical to the opening values.
- BUT `tree_stable` is `false`: the samples verify-completion.sh took at the
  step boundaries in between did **not** all agree with the endpoints.

The reproduction it stands for (measured window on a real full run: 2,225s):

1. start clean at commit `H`, whose committed state fails a later step,
2. while an early long step runs, make the uncommitted fix that step needs,
3. the later step passes — on an edit no commit contains,
4. revert it after the run finishes; both endpoints are pristine again,
5. `git push H`.

Endpoint evidence alone cannot see this: the opening and closing snapshots are
identical and perfectly clean. Only the samples taken *across* the run witness it.

HONEST LIMIT: sampling is at step boundaries, so a change made **and undone**
inside a single step is still unobserved — the exposure is one step wide, not zero.

Expected guard exit: 1 (`AUDIT_TREE_MUTATED_MIDRUN`).
