# fail_receipts_symlink

Byte-copy of `pass_clean` whose receipts ledger (`practices/upstream/_FETCH-RECEIPTS.yaml`) is a
SYMLINK to a sibling holding the identical bytes — and nothing else differs. So `pass_clean` ⇒
exit 0 and this ⇒ exit 2 is attributable to the link alone.

THE CLASS (P1-Y, TD-2026-07-30-P1-anchor-authenticity): the ANCHOR side reads GIT OBJECTS and the
SELF side reads the FILESYSTEM. With the ledger symlinked, `git show <anchor>:…` returns the
target PATHNAME — which YAML parses as a plain STRING — while this process parses the real
mapping through the link. Every append-only layer (by-id identity, row-order prefix, byte-chunk
prefix) used to be nested under `isinstance(prior_doc, dict)` with NO blocking else, so the whole
ratchet retired itself in silence and a released receipt row could be rewritten freely.

Expected guard exit: 2 (`SELF_PATH_NOT_REGULAR`).

Non-vacuity: the pre-round-3 guard exits 0 on this exact fixture.
