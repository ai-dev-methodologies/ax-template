# fail_tree_fingerprint_not_a_digest

The audit line records `tree_fingerprint: "x"`.

Before round 5 that satisfied the "the tree was identified" test, which only asked for a
non-empty string that is neither `nogit` nor `unverifiable-*` — so ANY string passed, and the
value was compared to a recompute that could itself be switched off. The recorded value must
now BE a fingerprint: 64 lowercase hex characters. Expected exit 1 (AUDIT_TREE_UNIDENTIFIED).
