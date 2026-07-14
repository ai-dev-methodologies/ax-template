#!/usr/bin/env bash
# FIXTURE stub (FAIL side) — P3-47. This stub is deliberately BROKEN in exactly
# one way: it exits 0 WITHOUT ever writing the block->pass pair to the ledger.
#
# It is engineered to pass agent_block_proof_guard checks (a) exists+executable
# and (c) non-vacuity — the source below contains the 'blocked_rc -ne 1'
# fail-guard token and references controller_problemdetail_guard.sh — so the ONLY
# check it trips is (b): "exited 0 but did not record the block->pass pair"
# (actor=agent events: 0, expected >= 2). This proves guard[76] check (b) is
# non-vacuous: a proof that silently stops logging the toggle is caught.
#
# Non-vacuity tokens the guard greps for (present so (a)/(c) pass, (b) is the
# sole failure): blocked_rc -ne 1 ; controller_problemdetail_guard.sh
set -uo pipefail

# A real proof would run controller_problemdetail_guard.sh, capture blocked_rc,
# assert `blocked_rc -ne 1`, then log two actor=agent events. This stub does
# none of that — it just exits 0 with an empty ledger.
exit 0
