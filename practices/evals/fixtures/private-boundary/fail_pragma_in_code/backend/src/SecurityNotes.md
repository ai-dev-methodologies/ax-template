# SecurityNotes.md — f1 falsification fixture for private_boundary_guard [R26]

This file demonstrates that pragma: allow-secret does NOT suppress secrets
in a `.md` file inside a code path (backend/src/).

EXPECTED RESULT: exit 1 (Layer 2 violation — real AKIA key in code-tree .md)

With OLD is_doc_path() (`*.md` matched anywhere): SecurityNotes.md matches *.md
→ is_doc_path returns true → pragma accepted → suppressed → exit 0 (BUG).

With CORRECT is_doc_path() (only docs/ or practices/rules/ subtree): SecurityNotes.md
is in backend/src/ → NOT a doc path → pragma ignored → violation → exit 1 (CORRECT).

The real key below has `# pragma: allow-secret` — but it is in backend/src/, so the
pragma is ignored regardless of the file extension:

AWS_KEY = "AKIA0123456789ABCDEF" # pragma: allow-secret
