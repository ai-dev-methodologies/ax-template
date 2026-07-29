# BACKLOG (fixture) — domain_mode_consistency_guard, backlog_ref liveness

Fixture for check 4b: this tree carries a backlog in which **P2-47 is CLOSED** while a
known_gap subject still names it. A known_gap whose row is closed tracks nothing — either the
divergence was resolved (remove the allowance) or the row was closed while the gap remained —
so the guard must exit 1.

The subject comes from this tree's `practices/evals/domain_mode_probe_allowances.yaml`. The
guard's real allowance table is empty (P2-47 was closed by authoring the three frontend Spec
Trios), so a fixture that relied on the table would have no subject and would pass while
proving nothing. Probe subjects are refused on the catalog tree and can only add subjects to
check.

Everything else in this tree is deliberately clean and mirrors `pass_clean`: one domain, one
agreeing declaration triple, no divergence, no floor to breach. Without the 4b check this
tree exits 0, which is what makes the fixture non-vacuous.

The row below is written in the real backlog's shape — `- [x] <id> …` at line start — because
that is the shape the guard's row regex is anchored to.

## P2

- [x] P2-47 (fixture) trio allowlist가 3개 도메인에서 거짓 — closed here on purpose so the
  known_gap subject that references it becomes untracked.

## P3

- [ ] P3-83 (fixture) the row that produced the guard itself; open, and not referenced by
  any allowance — present only so the fixture backlog has both states in it.
