# resume-provenance fixtures — resume_provenance_guard.sh [96]

Each fixture is a directory holding a single `neuter-mode` file. The guard copies the
**live** `practices/scripts/verify-completion.sh`, applies that mode's mutation to the
copy, and runs its full assertion set against the result.

| fixture                    | neuter-mode | meaning                                                      | expected exit |
|----------------------------|-------------|--------------------------------------------------------------|---------------|
| `pass_fixed`               | `none`      | the real script, unmutated                                   | 0             |
| `fail_unfixed`             | `ab`        | both ledger-loss layers removed — the pre-fix P0-30 script    | 1             |
| `fail_unfixed_workdir`     | `cd`        | both absent-directory layers removed — the pre-fix 2026-07-29 script | 1     |
| `fail_unfixed_fingerprint` | `e`         | resume records bound to head_sha only — the pre-fix stale-tree script | 1     |
| `fail_unfixed_emptystep`   | `f`         | a selected step with no commands silently dropped from the plan | 1          |

`none` selects NOTHING: the mode is read as a SET of layer letters, not as a substring to
search for. (It has to be — the word "none" literally contains an `e`, so a substring test
would neuter layer `e` on the supposedly-unmutated subject and quietly hollow out the live
assertion. That happened once during development and is why the parsing is explicit.)

The six layers the modes address:

- **a** — the step verdict records `UNRUN` (not `PASS`) for a step that produced no
  observed command outcome. Neutering it restores `no failure ⇒ PASS`.
- **b** — a run ending in `LEDGER_BROKEN` discards its resume record instead of
  publishing it. Neutering it restores the incremental publish.
- **c** — a non-advisory command whose working directory is absent BLOCKS. Neutering it
  restores the pre-fix silent skip, which left a row that looked like an outcome.
- **d** — a step is PASS only when every planned non-advisory command actually executed.
  Neutering it restores "the step has a row ⇒ the step was verified".
- **e** — the resume preloader refuses a record whose working-tree fingerprint differs from
  the tree in front of it. Neutering it restores "same head_sha is good enough", so a record
  produced while an uncommitted change was present is consumed after that change is reverted.
- **f** — a SELECTED checklist step declaring `commands: []` is a parse-time BLOCK.
  Neutering it restores the silent drop: the step emits no plan row, never enters
  STEP_ORDER, and is therefore invisible to every accounting check.

`a`/`b` are exercised by the ledger-loss harness (step 1 wipes the run's own temp dir);
`c`/`d` by the absent-working-directory harness (the reviewer's `mv frontend frontend.off`
reproduction); `e` by a git-backed harness whose COMMITTED state fails the step (so the step
passes only while an uncommitted edit is present, then the edit is reverted at the same head);
`f` by a two-step checklist whose first step declares no commands. Each fail fixture removes
exactly the layer(s) its own harness targets, so the proofs stay independent.

## Why a mode file and not a frozen copy of verify-completion.sh

A committed pre-fix copy would rot the moment the real script changes, and the guard would
then be proving something about a stale artifact rather than about the gate in use. Storing
the *mutation* instead keeps the subject derived from the live script on every run — and if
an anchor ever stops matching exactly once, the guard reports its own mutation proof as
stale rather than silently degrading.

`fail_unfixed` is what makes the live PASS meaningful: it demonstrates the assertions
actually detect the laundering regression instead of passing vacuously.
