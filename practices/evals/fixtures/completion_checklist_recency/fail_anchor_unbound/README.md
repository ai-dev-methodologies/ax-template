# fail_anchor_unbound

Fixture for `completion_checklist_recency_guard.sh` check 8 (P1-X layer 3,
TD-2026-07-30-P1-anchor-authenticity).

FAIL shape: the audit line carries NO `anchor_sha` at all, so there is nothing to compare against
the sha the remote advertises. Fail-closed rather than assumed honest: a producer that did not
record which release it measured against is exactly the state the ref-forgery attack wants.

Expected guard exit: 1 (`AUDIT_ANCHOR_UNBOUND`).
