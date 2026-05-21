# fail_audit_log_last_run_red

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape:
- `.ax-verify/runs.jsonl` exists with a valid JSON line.
- Latest line's `head_sha` matches expected (so head is current).
- BUT latest line has `exit=1, hard_fail=2` — i.e. the last verify-completion
  run failed and was not re-driven to GREEN.

Expected guard exit: 1 (code AUDIT_LAST_RUN_FAILED).
