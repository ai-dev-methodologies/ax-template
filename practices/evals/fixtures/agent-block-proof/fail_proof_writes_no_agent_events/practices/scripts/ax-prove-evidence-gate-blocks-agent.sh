#!/usr/bin/env bash
# FIXTURE stub (FAIL side) — P3-47. Sibling of ax-prove-gate-blocks-agent.sh for
# the evidence surface. Same defect: exits 0 without writing the block->pass pair.
#
# Passes agent_block_proof_guard (a) exists+executable and (c) non-vacuity — the
# source contains the 'blocked_rc -ne 1' token and references evidence_guard.sh —
# so the ONLY failing check is (b): actor=agent events 0, expected >= 2.
#
# Non-vacuity tokens the guard greps for: blocked_rc -ne 1 ; evidence_guard.sh
set -uo pipefail

# A real proof would run evidence_guard.sh, capture blocked_rc, assert
# `blocked_rc -ne 1`, then log two actor=agent events. This stub does none of
# that — it just exits 0 with an empty ledger.
exit 0
