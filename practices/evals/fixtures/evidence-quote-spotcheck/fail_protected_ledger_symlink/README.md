# fail_protected_ledger_symlink

Byte-copy of `pass_template_correct_anchor` whose protected-anchor ledger
(`practices/evals/evidence_protected_template_anchors.txt`) is a SYMLINK to a sibling holding the
identical bytes — and nothing else differs. So `pass_template_correct_anchor` ⇒ exit 0 and this
⇒ exit 2 is attributable to the link alone.

THE CLASS (P1-Y, TD-2026-07-30-P1-anchor-authenticity): the ANCHOR side reads GIT OBJECTS and the
SELF side reads the FILESYSTEM. `open()` follows a symlink to the real content; `git show` /
`git ls-tree` return the LINK BLOB, which is just the target PATHNAME — and a pathname can be
spelled as parseable-but-weakened source. Any place those two sides disagree is a laundering
channel, so an anchor-critical path must be a regular file on BOTH sides.

Expected guard exit: 2 (`SELF_PATH_NOT_REGULAR`), under
`--strict --strict-templates --templates-only-protected --root <this dir>`.

Non-vacuity: the pre-round-3 guard exits 0 on this exact fixture.
