# fail_stale_head

Same as pass_fresh except the audit log's `head_sha` (3333333333333333333333333333333333333333) does not match
`.ax-downstream/expected_head.txt` (2222222222222222222222222222222222222222) — the log was written for a
different (older) commit than the one being pushed. Expected: exit 1,
AX_DOWNSTREAM_LOG_STALE_HEAD.
