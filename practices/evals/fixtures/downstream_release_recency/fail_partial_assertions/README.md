# fail_partial_assertions

Log's `assertions.A0` is `false` — one behavioral assertion did not pass, even
though everything else (head_sha, tree_clean, digests) is otherwise correct.
Expected: exit 1, AX_DOWNSTREAM_LOG_PARTIAL_ASSERTIONS.

The line carries the complete declared key set (only `A0`'s VALUE is false), so
this fixture still fails for its own reason rather than for an incomplete key
set — the two failure modes are tested separately (see
`fail_missing_assertion_key`).
