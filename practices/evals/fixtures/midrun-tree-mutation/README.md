# fixtures/midrun-tree-mutation

Fixtures for `practices/evals/midrun_tree_mutation_guard.sh` [98].

Each directory carries a `neuter-mode` file, **not** a frozen copy of the scripts:
the guard derives the mutant from the LIVE `verify-completion.sh` +
`completion_checklist_recency_guard.sh` every time, so a fixture cannot rot into
testing a stale artifact.

| fixture | mode | meaning | expected exit |
|---|---|---|---|
| `pass_fixed` | `none` | the real pair | 0 |
| `fail_unfixed_producer` | `s` | the producer never notices a sample disagreeing with the start, so every line claims a settled tree | 1 |
| `fail_unfixed_consumer` | `t` | the recency guard stops requiring a settled tree | 1 |

(S) and (T) are SERIAL links: a producer that always claims stability defeats an
attentive consumer, and an indifferent consumer defeats an honest producer. So each
fail fixture reproduces the laundering on its own — that is the non-vacuity proof
for the live assertions, not a claim that either layer is redundant.
