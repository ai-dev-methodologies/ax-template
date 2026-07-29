# fail_audit_log_no_tree_sampling

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape: a line whose ENDPOINTS are complete and green (`tree_clean`,
`tree_clean_end`, `head_sha_end`, `tree_fingerprint_end` all present and
agreeing) but which carries no `tree_stable` / `tree_samples` — i.e. the producer
never sampled the tree BETWEEN the endpoints.

Fail-closed on purpose: an absent field is not evidence of a settled tree, it is
the absence of the measurement, and endpoints alone cannot see an edit that was
made after the run started and undone before it finished. Re-running the contract
at the pushed commit is always available, so refusing costs nothing.

(A wholly legacy line — one that lacks `tree_clean_end` too — is refused earlier,
by the clean-endpoints check, with `AUDIT_DIRTY_TREE_EVIDENCE`.)

Expected guard exit: 1 (`AUDIT_TREE_MUTATED_MIDRUN`).
