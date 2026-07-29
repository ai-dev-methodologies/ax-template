# fail_audit_log_no_tree_fingerprint

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape (cross-family review P1, 2026-07-29):

- `.ax-verify/runs.jsonl` latest line matches `expected_head.txt`, `exit` is 0,
  `hard_fail` is 0, `full_run` is `true`, `tree_clean` is `true`.
- BUT there is no `tree_fingerprint`: the line does not say WHICH working tree it
  verified. That is the shape of every audit line written before the tree binding
  existed, and of a run whose fingerprint helper degraded (`nogit`,
  `unverifiable-*`).

`head_sha` alone is satisfied by any tree at that commit, so such a line cannot
show that the code being pushed is the code that passed. Fail closed — re-running
the contract at the pushed commit is always available.

Expected guard exit: 1 (`AUDIT_TREE_UNIDENTIFIED`).
