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

## templates/** roots

Invoked with `--include-templates`; the guard additionally scans
`<root>/templates/**/*.{tsx,ts}` frontmatter.

- `fail_template_fabricated_anchor/` — widget quote absent from its snapshot.
- `pass_template_correct_anchor/` — widget quote present in its snapshot.

## protected-anchor ledger roots (`--templates-only-protected`)

`--templates-only-protected` restricts the templates sweep to the anchors listed in
`<root>/practices/evals/evidence_protected_template_anchors.txt` and makes them FATAL under
`--strict --strict-templates` (the full templates sweep stays advisory — see the guard
header). The two roots above carry a ledger too, so they double as the quote pass/fail pair
for this mode (exit 0 / exit 1). The roots below prove the ledger cannot be emptied into a
silent pass — **each exits 2**, and each is built so the declared defect is its ONLY defect:

- `fail_protected_ledger_missing/` — no ledger file at all.
- `fail_protected_ledger_empty/` — valid `# min_entries:` directive, zero anchors.
- `fail_protected_ledger_no_min/` — one anchor, no `# min_entries:` directive.
- `fail_protected_ledger_shrunk/` — declares `min_entries: 2`, lists one anchor.
- `fail_protected_entry_missing_file/` — anchor path that does not exist.
- `fail_protected_entry_no_evidence/` — protected file carries zero `upstream_id` evidence.
- `fail_protected_anchor_absent/` — declared `upstream_id` is not cited by that file.

Not fixture-covered by construction: `PROTECTED_LEDGER_FLOOR` (declared `min_entries` below
the guard-pinned `LIVE_MIN_PROTECTED_ENTRIES`) only applies to the real repo tree, i.e. when
no `--root` is passed, so no fixture root can reach it.
