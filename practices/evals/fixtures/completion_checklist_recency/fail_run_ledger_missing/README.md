# fail_run_ledger_missing — a summary with nothing behind it (P1-4, ROUND 4)

One line is one claim. A genuine run also publishes `.ax-verify/last_run.jsonl`, a record per
step written incrementally as the run proceeds. This fixture has a flawless summary line and no
step ledger — which is exactly what `echo '{…}' >> runs.jsonl` produces.

Expected: exit 1, AUDIT_RUN_LEDGER_MISSING.
