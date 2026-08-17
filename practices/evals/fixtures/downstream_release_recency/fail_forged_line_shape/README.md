# fail_forged_line_shape

Everything guard [114] used to read is correct here: head_sha matches
expected_head.txt, tree_clean is true, the assertion key set is exactly the
declared manifest with every value boolean true, verdict is "pass", override is
empty, and artifact_digests matches the recompute from `skills/*/SKILL.md`.

What is wrong is the SHAPE. The line carries no `timestamp` and no `harness` —
the two fields the audit writer emits that this gate never reads — and one key
(`I_TYPED_THIS_BY_HAND`) that no writer has ever emitted. That is what a
hand-authored record looks like: it carries the fields its author knew the
reader would look at.

MEASURED against the pre-P2-106 guard: exit 0, "PASS — audit log matches the
pushed sha". Expected now: exit 1 (AX_DOWNSTREAM_LOG_SCHEMA_MISMATCH).

Single-reason by construction: neuter the field-set comparison and this tree
passes every remaining check.
