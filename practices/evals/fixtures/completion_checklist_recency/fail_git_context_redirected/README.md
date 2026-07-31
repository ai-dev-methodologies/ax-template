# fail_git_context_redirected

A well-formed audit line, and NO `expected_head.txt` — so the guard must decide for itself
which HEAD the record should be about. This directory sits INSIDE a git repository but is not
the top of it, so every `git -C <root>` answer describes a different tree (the same shape a
redirected GIT_DIR/GIT_WORK_TREE produces: measured, a dirty tree reported the clean-tree
fingerprint constant of a shadow checkout).

Before round 5 the fallback was `git rev-parse HEAD` in the root, so the record was audited
against the ENCLOSING repository's HEAD — and if git failed entirely the guard exited 0
("no git, no fixture"). Both are fail-open. Expected exit 1 (GIT_CONTEXT_REDIRECTED).
