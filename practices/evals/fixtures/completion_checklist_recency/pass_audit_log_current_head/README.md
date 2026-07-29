# pass_audit_log_current_head

Fixture for `completion_checklist_recency_guard.sh`.

PASS shape:
- `.ax-verify/runs.jsonl` exists with a valid JSON line.
- Latest line's `head_sha` matches `.ax-verify/expected_head.txt`.
- Latest line's `exit` is 0 and `hard_fail` is 0.
- Latest line's `full_run` is `true` (whole-checklist run, not `--step` partial).
- Latest line identifies the tree it verified and shows a SETTLED one for the whole
  run: `tree_clean` and `tree_clean_end` both `true`, `head_sha_end` /
  `tree_fingerprint_end` equal to the opening values, `tree_stable` `true`.

Expected guard exit: 0.
