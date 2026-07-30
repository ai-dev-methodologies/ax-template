# fail_anchor_forged

Fixture for `completion_checklist_recency_guard.sh` check 8 (P1-X layer 3,
TD-2026-07-30-P1-anchor-authenticity).

`refs/remotes/origin/main` is an ORDINARY LOCAL REF. `git update-ref` can aim it at a synthetic
commit whose tree merely DROPS the ratcheting guards; both cross-release ratchets then take their
"first-release bootstrap" skip and R25 passes on a downgrade. No tree fingerprint can see this —
a ref is not part of the working tree.

FAIL shape: the audit line is green in every other respect, but its `anchor_sha` names a commit
the remote does not have (`expected_anchor.txt` holds what the remote advertises).

Expected guard exit: 1 (`AUDIT_ANCHOR_FORGED`).

Non-vacuity: the pre-round-3 guard exits 0 on this exact fixture — it never read `anchor_sha`.
