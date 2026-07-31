# fail_audit_line_forged_shape — a HAND-AUTHORED audit line (P1-4, ROUND 4)

`.ax-verify/runs.jsonl` is an ordinary append-only text file. This fixture's latest line is
green on EVERY pre-round-4 axis — head match, exit 0, hard_fail 0, full_run true, usable
fingerprint, clean at both endpoints, stable tree with 9 samples, anchor recorded and settled —
because every one of those values was supplied by its author. It is a forgery, and the only
thing that gives it away is its SHAPE: it carries one field (`verified_by`) that
verify-completion.sh does not emit.

Expected: exit 1, AUDIT_LINE_SCHEMA_MISMATCH. The pre-round-4 guard exits 0 on it.
