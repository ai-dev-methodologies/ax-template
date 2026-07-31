# fail_run_ledger_status_conflict — the steps disagree with the summary (P1-4, ROUND 4)

The summary claims exit 0 / hard_fail 0 while the run's own per-step ledger records a step that
did not pass. A step that did not pass cannot be part of a run that certifies a push.

Expected: exit 1, AUDIT_RUN_LEDGER_STATUS_CONFLICT.
