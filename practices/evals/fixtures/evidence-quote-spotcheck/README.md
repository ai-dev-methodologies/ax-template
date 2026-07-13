# evidence-quote-spotcheck fixtures

Self-contained roots proving `evidence_quote_spotcheck_guard.sh --strict
--allow-missing-snapshot` is non-vacuous: a QUOTE mismatch still blocks, while a
missing-snapshot finding is downgraded to advisory.

Invoke each with `--root <fixture-dir>`; the guard scans `<root>/practices/rules/*.md`
against `<root>/practices/upstream/{upstream_id}.snapshot.md`.

- `fail_quote_mismatch_snapshot_missing/` — one rule whose quote is genuinely absent
  from its (present) snapshot + one rule pointing at a nonexistent snapshot.
  `--strict --allow-missing-snapshot` ⇒ exit 1 (the QUOTE mismatch is still fatal;
  the missing snapshot is downgraded but does not rescue the exit code).
- `pass_only_missing_snapshot/` — one rule whose quote DOES match a present snapshot
  + one rule pointing at a nonexistent snapshot.
  `--strict --allow-missing-snapshot` ⇒ exit 0 (no QUOTE mismatch; the missing
  snapshot is the only finding and is downgraded to advisory).
