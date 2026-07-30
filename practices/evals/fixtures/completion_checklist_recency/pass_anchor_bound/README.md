# pass_anchor_bound

Fixture for `completion_checklist_recency_guard.sh` check 8 — the RELEASE ANCHOR binding
(P1-X layer 3, TD-2026-07-30-P1-anchor-authenticity).

The guard reads the anchor it must authenticate against from `.ax-verify/expected_anchor.txt`,
which stands in for the sha `git` hands `.githooks/pre-push` for the ref being pushed (that value
comes from the REMOTE's own advertisement — the one copy no local `git update-ref` can rewrite).

PASS shape: everything `pass_audit_log_current_head` requires, PLUS the audit line's `anchor_sha`
EQUALS `expected_anchor.txt` — the run ratcheted against the release the remote actually has.

Expected guard exit: 0.
