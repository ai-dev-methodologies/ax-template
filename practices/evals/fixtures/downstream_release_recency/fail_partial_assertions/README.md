# fail_partial_assertions

Log's `assertions.A0` is `false` — one behavioral assertion did not pass, even
though everything else (head_sha, tree_clean, digests) is otherwise correct.
Expected: exit 1, AX_DOWNSTREAM_LOG_PARTIAL_ASSERTIONS.
