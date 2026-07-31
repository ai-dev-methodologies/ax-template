# fail_run_ledger_head_mismatch — two artifacts, two different runs (P1-4, ROUND 4)

The summary line claims the pushed head; the per-step ledger beside it records steps taken at a
DIFFERENT head. Copying a plausible ledger from an earlier run is the obvious next move once the
ledger is required to exist, so the two artifacts must also agree.

Expected: exit 1, AUDIT_RUN_LEDGER_HEAD_MISMATCH.
