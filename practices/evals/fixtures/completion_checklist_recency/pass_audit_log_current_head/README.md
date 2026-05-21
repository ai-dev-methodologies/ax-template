# pass_audit_log_current_head

Fixture for `completion_checklist_recency_guard.sh`.

PASS shape:
- `.ax-verify/runs.jsonl` exists with a valid JSON line.
- Latest line's `head_sha` matches `.ax-verify/expected_head.txt`.
- Latest line's `exit` is 0 and `hard_fail` is 0.

Expected guard exit: 0.
