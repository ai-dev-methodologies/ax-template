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
- `fail_protected_entry_empty_quote/` — matching entry's `quote` is `""`. Closes a codex
  round-2 finding: an absent/blank quote is vacuously a substring of every snapshot body, so
  blanking or deleting a protected quote used to pass instead of failing.
- `fail_protected_entry_empty_section/` — matching entry's `section` is `""` (the analogous
  structural bypass — a protected anchor with no declared section verifies nothing).

### codex round-3 — type strictness and identity pinning

The gate was bypassed a **third** time, with a third trick, because rounds 1-2 validated the
*shape* of a ledger entry and never pinned the **identity** of what must be protected nor the
**type** of the scalars compared. Each root below reproduces one bypass; all but the last two
exit 2, and every one of them is non-vacuous — the pre-round-3 guard exits **0** on seven of
the eight (the eighth, `snapshot_missing`, exited 0 on any invocation that omitted
`--strict-templates`).

Type coercion — nothing is `str()`-coerced into the comparison any more:

- `fail_protected_entry_int_quote/` — `quote: 0` (unquoted ⇒ int). `str(0)` = `"0"`, which
  occurs verbatim in the real Stripe snapshot body, so the substring check passed and the
  protected anchor carried no evidence at all ⇒ `PROTECTED_LEDGER_NON_STRING_QUOTE`.
- `fail_protected_entry_null_section/` — `section: null` (⇒ `None`). `str(None)` = `"None"`,
  which is not blank, so the blank-section check never fired ⇒
  `PROTECTED_LEDGER_NON_STRING_SECTION`.
- `fail_protected_entry_short_quote/` — `quote: "the"`: a genuine non-empty string that clears
  both the type and blank checks yet is a substring of nearly every snapshot body — the
  empty-quote bypass wearing a valid type ⇒ `PROTECTED_LEDGER_QUOTE_TOO_SHORT`.

Identity vs row count — the ledger is a SET of `(path, upstream_id)` identities, not a count:

- `fail_protected_ledger_duplicate_identity/` — the reviewer's exact trick: delete the row
  that matters, duplicate the clean row, keep `min_entries: 2`. Two rows satisfied the floor,
  the clean anchor was verified twice, the gate exited 0 ⇒
  `PROTECTED_LEDGER_DUPLICATE_IDENTITY`.
- `fail_protected_required_identity_missing/` — a ledger that is well-formed by every
  pre-round-3 rule (one unique, disk-clean, fully-verified anchor; matching `min_entries`)
  but is missing an identity the gate exists to protect ⇒
  `PROTECTED_LEDGER_REQUIRED_IDENTITY_MISSING`. Required identities come from `# require:`
  directives in the ledger (all roots) plus `LIVE_REQUIRED_PROTECTED_IDENTITIES` pinned in the
  guard (real repo tree only), so dropping one takes two coordinated edits.

Same-pass closures found while re-reading the whole `protected_only` path:

- `fail_protected_snapshot_missing/` — the protected anchor's snapshot body is absent from
  disk. Previously a *finding*, fatal only when `--strict-templates` happened to be passed,
  making snapshot deletion a cheaper bypass than quote falsification ⇒ now
  `PROTECTED_LEDGER_SNAPSHOT_MISSING`, structural and flag-independent (registered twice:
  with and without the strict flags).
- `fail_protected_unsafe_path/` — `templates/L1/components/../components/sample-widget.tsx`
  resolves to the real file, so the old guard passed — but a path is an identity component
  and an identity with two spellings defeats the required-identity comparison ⇒
  `PROTECTED_LEDGER_UNSAFE_PATH`.
- `fail_protected_fabricated_section/` — honest quote, **fabricated section**. `section` used
  to be checked for blankness and then never compared against anything, i.e. unverified free
  text inside a provenance gate ⇒ `TEMPLATE_SECTION_NOT_IN_SNAPSHOT`, a falsified-evidence
  finding (**exit 1**, same family as a fabricated quote — not a structural exit 2).

Not fixture-covered by construction: `PROTECTED_LEDGER_FLOOR` (declared `min_entries` below
the guard-pinned `LIVE_MIN_PROTECTED_ENTRIES`) and the guard-pinned half of
`PROTECTED_LEDGER_REQUIRED_IDENTITY_MISSING` only apply to the real repo tree, i.e. when no
`--root` is passed, so no fixture root can reach them. The ledger-declared `# require:` half
IS fixture-covered above, and the live half is proven by the reproductions recorded in the
round-3 closure report.
