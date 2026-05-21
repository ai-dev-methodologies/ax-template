# fail_audit_log_missing

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape:
- `.ax-verify/runs.jsonl` does NOT exist.
- An `.ax-verify/expected_head.txt` is present so the guard reaches the
  audit-log check (otherwise it would skip due to non-git fixture).

Expected guard exit: 1 (code AUDIT_LOG_MISSING).
