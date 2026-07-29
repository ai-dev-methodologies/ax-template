# fail_audit_log_partial_run

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape (dogfood-confirmed P2, 2026-07-10):
- `.ax-verify/runs.jsonl` latest line matches `expected_head.txt`, `exit` is 0
  and `hard_fail` is 0 — i.e. it would have PASSED the pre-full_run guard —
- but `full_run` is `false`: the line was written by a `--step <id>` partial
  run (a single trivial step, e.g. backend-build), not the full checklist.

A partial run must NOT satisfy the completion contract, otherwise `--step`
functions as an undocumented skip flag.

- Every OTHER axis is deliberately green — including the across-the-run tree
  sampling fields (`tree_clean_end`, `tree_stable`, `tree_samples`, matching
  endpoints) — so this fixture isolates exactly the one defect it is named for.
  fixture_kill_proof [87] depends on that: neuter the targeted check and this
  fixture must PASS, which it cannot do if a second check also fires.

Expected guard exit: 1 (`AUDIT_PARTIAL_RUN`).
