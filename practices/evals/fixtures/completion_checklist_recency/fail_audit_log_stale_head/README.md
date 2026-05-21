# fail_audit_log_stale_head

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape:
- `.ax-verify/runs.jsonl` exists with a valid JSON line.
- Latest line's `head_sha` is `deadbeef...` but `expected_head.txt` is
  `aaaaa...` — i.e. a commit happened AFTER the last verify-completion run.

Expected guard exit: 1 (code AUDIT_STALE_HEAD).
