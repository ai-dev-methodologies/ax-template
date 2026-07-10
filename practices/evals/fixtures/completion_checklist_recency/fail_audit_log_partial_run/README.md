# fail_audit_log_partial_run

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape (dogfood-confirmed P2, 2026-07-10):
- `.ax-verify/runs.jsonl` latest line matches `expected_head.txt`, `exit` is 0
  and `hard_fail` is 0 — i.e. it would have PASSED the pre-full_run guard —
- but `full_run` is `false`: the line was written by a `--step <id>` partial
  run (a single trivial step, e.g. backend-build), not the full checklist.

A partial run must NOT satisfy the completion contract, otherwise `--step`
functions as an undocumented skip flag.

Expected guard exit: 1 (`AUDIT_PARTIAL_RUN`).
