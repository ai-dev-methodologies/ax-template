# fixtures/background-poll-refresh-state — R82b [41] pass/fail pairs

BACKLOG **P3-97**. These fixtures pin the two properties the guard gained when its
`aria-busy` leg stopped being a bare `grep 'aria-busy'`:

1. a **comment** naming the attribute does not satisfy the guard (only a real JSX
   attribute does), and
2. under the P2-42 presentational split the attribute may live in the page's
   **ledgered co-located view** instead of the page itself.

A third, adjacent defect surfaced while writing these: the **`dataUpdatedAt` leg had the
identical hole**, and this directory's own fixture proved it — the fixture's explanatory
comment mentioned the token, and the fixture PASSED. Both compliance legs now go through
the same non-comment code probe.

Every fixture is a minimal `templates/L4/demo/...` tree plus its own `ledger.yaml`, run
with `--root <fixture> --ledger <fixture>/ledger.yaml`. All four trip the same trigger
(line-anchored `refetchInterval:` + `useQuery` + `useMutation`), so exactly one compliance
leg varies per fixture.

| Fixture | Shape | Expected |
|---|---|---|
| `pass_clean` | ledgered pair; page has only the truthful comment, view has the real attribute | **exit 0** |
| `fail_comment_only` | ledgered pair; NEITHER file has the attribute, page has an aria-busy comment | **exit 1** |
| `fail_unledgered_comment_only` | page with no ledger entry (unconverted); comment only | **exit 1** |
| `fail_no_data_updated_at` | real aria-busy on the page, but `dataUpdatedAt` appears only in a comment | **exit 1** |

`pass_clean` and `fail_comment_only` differ by exactly one line — the view's
`aria-busy={...}` attribute — which is what makes the fail fixture non-vacuous.
