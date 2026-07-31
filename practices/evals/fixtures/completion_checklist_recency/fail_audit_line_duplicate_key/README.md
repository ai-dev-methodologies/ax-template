# fail_audit_line_duplicate_key — one record, two claims (P1-4, ROUND 4)

The reviewer's note: a DUPLICATED end-field lets a placeholder pass. `json.loads` keeps the LAST
occurrence, so this line reads as a perfect record while a human reading it sees
`"tree_fingerprint_end": "x"` first. A parser that silently picks a winner lets the writer decide
which of its two statements is the audited one.

Expected: exit 1, AUDIT_LINE_DUPLICATE_KEY. Under the pre-round-4 guard the second value wins and
the line passes.
