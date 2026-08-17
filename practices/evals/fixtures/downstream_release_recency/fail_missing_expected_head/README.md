# fail_missing_expected_head

No `.ax-downstream/expected_head.txt`, so in FIXTURE-SHAPED mode the gate has no
sha to hold the audit log against — and the staleness check is an EQUALITY. The
log line's `"head_sha"` is the empty string, so `"" == ""` and the gate certified
a run about no commit at all.

This is the same class the precedent pins with
`completion_checklist_recency/fail_git_context_redirected`: when the gate cannot
establish WHICH head the record is supposed to be about, it must refuse rather
than default to a value that compares equal to anything.

MEASURED against the pre-P2-106 guard: exit 0, "PASS — audit log matches the
pushed sha". Expected now: exit 1 (AX_DOWNSTREAM_EXPECTED_HEAD_UNRESOLVED).

Single-reason by construction: restore expected_head.txt (or neuter the
refusal) and every remaining check passes.
