# fail_anchor_moved_midrun — the reference point moved while the run happened (P1-2, ROUND 4)

`refs/remotes/origin/main` is an ordinary local ref. Aim it at a commit that merely LACKS the
ratcheting files for the minutes the guards run — every ratchet then takes its first-release
bootstrap skip — and restore it before the audit line is written. Check 8 (`anchor_sha` equals
what the remote advertises) is perfectly satisfied, because the RECORDED value is the honest one.
Only the producer can see the drift, so it now reports both endpoints and this guard verifies the
relation.

Expected: exit 1, AUDIT_ANCHOR_MOVED_MIDRUN. The pre-round-4 guard has no such field and exits 0.
