# BACKLOG (fixture) — domain_mode_consistency_guard, backlog_ref liveness

Fixture for check 4b: this tree carries a backlog in which **P2-47 has no row at all** — the
id its known_gap subject names has been renumbered or deleted. A ref that resolves to nothing
tracks nothing, so the guard must exit 1. This is the branch that the previous "backlog_ref is
non-empty" check could never see: the string was there, the row was not.

The subject comes from this tree's `practices/evals/domain_mode_probe_allowances.yaml` — the
guard's real allowance table is empty, so a fixture relying on it would have no subject and
would pass vacuously. Probe subjects are refused on the catalog tree and only ever add
subjects to check.

The mention below is the trap the row regex must NOT fall for: `(P2-47)` appears in body
prose, parenthesised the way real sibling references are written, and a scanner that counted
mentions instead of rows would call this ref present and exit 0. (It is the tree's only
occurrence of the id outside this explanatory header.)

Everything else mirrors `pass_clean`: one domain, one agreeing triple, no divergence, no
floor to breach — so without the 4b check this tree exits 0.

## P3

- [ ] P3-83 (fixture) the row that produced the guard. Its body mentions (P2-47) as a
  sibling, which is a MENTION and not a row.
