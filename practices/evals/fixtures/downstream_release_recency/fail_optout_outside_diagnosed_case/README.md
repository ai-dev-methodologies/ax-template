# fail_optout_outside_diagnosed_case

BACKLOG P2-111(a). This fixture is run by `--fixtures` with
`AX_SKIP_DOWNSTREAM_RELEASE_GATE=1` EXPORTED — the fixture runner sets that variable for
any fixture whose basename mentions `optout`, and explicitly unsets it for every other
fixture, so no fixture's verdict can be moved by an ambient export in the operator's shell.

The tree is `pass_fresh` with exactly one field changed: the latest audit line's
`"verdict"` is `"fail"` instead of `"pass"`. Everything else — head_sha, tree_clean, the
complete assertion set, the empty `override`, the artifact digests — is still valid, so
the ONLY reason this fixture can exit non-zero is `AX_DOWNSTREAM_LOG_NOT_PASS`.

Expected: **exit 1**. The opt-out must not rescue it.

## What this pins, and what it does not

The opt-out is honored ONLY inside the one diagnosed case it was written for: a base
commit that could not be resolved. FIXTURE-SHAPED mode has no base COMMIT at all — the
previous version comes from `.ax-downstream/prev_version.txt`, which is either present or
absent, and "absent" is a legitimate "no previous version", not a resolution failure. So
the diagnosed case cannot arise here and the variable is never consulted on this path.
This fixture pins that: with the kill switch on, a failing tree still fails.

It is NOT a differential for the P2-111(a) fix, and saying so is the point. Before that
fix the variable was ignored in `--root` mode for a *different* reason (any `--root` call
was treated as controlled), so this fixture would have exited 1 then too. The differential
is a LIVE-mode one and is recorded in the guard's header: with a base that resolves fine,
the old copy exited 0 on this same shape and the current one exits 1. What this fixture
buys is regression protection — if the opt-out is ever re-broadened to fire before
applicability, this fixture flips to exit 0 and `--fixtures` goes red.
