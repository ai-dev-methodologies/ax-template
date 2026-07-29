# fail_audit_log_dirty_tree

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape (cross-family review P1, 2026-07-29 — needs no `--resume`):

- `.ax-verify/runs.jsonl` latest line matches `expected_head.txt`, `exit` is 0,
  `hard_fail` is 0, `full_run` is `true`, and it carries a `tree_fingerprint` —
  i.e. it satisfies every pre-existing check.
- BUT `tree_clean` is `false`: the run that produced it was performed on a DIRTY
  working tree, so it certifies a tree that differs from the commit being pushed.

The reproduction it stands for:

1. committed HEAD `H` fails a step,
2. an uncommitted fix makes a full R25 run pass → this audit line,
3. the fix is stashed — nothing re-runs, the line is untouched,
4. `git push H` — the pushed tree was never verified.

A push must be backed by evidence from the CLEAN tree of the pushed sha.
Local iteration and `--resume` are unaffected; only push eligibility tightens.

Expected guard exit: 1 (`AUDIT_DIRTY_TREE_EVIDENCE`).
