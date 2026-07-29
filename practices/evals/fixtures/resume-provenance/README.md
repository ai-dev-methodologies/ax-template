# resume-provenance fixtures — resume_provenance_guard.sh [96]

Each fixture is a directory holding a single `neuter-mode` file. The guard copies the
**live** `practices/scripts/verify-completion.sh`, applies that mode's mutation to the
copy, and runs its full assertion set against the result.

| fixture                 | neuter-mode | meaning                                                      | expected exit |
|-------------------------|-------------|--------------------------------------------------------------|---------------|
| `pass_fixed`            | `none`      | the real script, unmutated                                   | 0             |
| `fail_unfixed`          | `ab`        | both ledger-loss layers removed — the pre-fix P0-30 script    | 1             |
| `fail_unfixed_workdir`  | `cd`        | both absent-directory layers removed — the pre-fix 2026-07-29 script | 1     |

The four layers the modes address:

- **a** — the step verdict records `UNRUN` (not `PASS`) for a step that produced no
  observed command outcome. Neutering it restores `no failure ⇒ PASS`.
- **b** — a run ending in `LEDGER_BROKEN` discards its resume record instead of
  publishing it. Neutering it restores the incremental publish.
- **c** — a non-advisory command whose working directory is absent BLOCKS. Neutering it
  restores the pre-fix silent skip, which left a row that looked like an outcome.
- **d** — a step is PASS only when every planned non-advisory command actually executed.
  Neutering it restores "the step has a row ⇒ the step was verified".

`a`/`b` are exercised by the ledger-loss harness (step 1 wipes the run's own temp dir);
`c`/`d` by the absent-working-directory harness (the reviewer's `mv frontend frontend.off`
reproduction). Each fail fixture removes exactly the pair its harness targets, so the two
proofs stay independent.

## Why a mode file and not a frozen copy of verify-completion.sh

A committed pre-fix copy would rot the moment the real script changes, and the guard would
then be proving something about a stale artifact rather than about the gate in use. Storing
the *mutation* instead keeps the subject derived from the live script on every run — and if
an anchor ever stops matching exactly once, the guard reports its own mutation proof as
stale rather than silently degrading.

`fail_unfixed` is what makes the live PASS meaningful: it demonstrates the assertions
actually detect the laundering regression instead of passing vacuously.
