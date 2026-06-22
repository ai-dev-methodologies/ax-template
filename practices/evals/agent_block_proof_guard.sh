#!/usr/bin/env bash
# practices/evals/agent_block_proof_guard.sh
# 2026-06-22 falsification-test regression guard (79th hard guard).
#
# practices/scripts/ax-prove-gate-blocks-agent.sh is the project's FALSIFICATION
# TEST: an "agent" writes a rule-violating @ExceptionHandler (returns
# Map<String,String> instead of RFC-9457 ProblemDetail), the REAL
# controller_problemdetail_guard BLOCKS it (exit 1), the agent corrects the
# return type to ProblemDetail, the SAME guard now PASSES (exit 0), and the
# block→pass pair is logged for actor=agent. That script is the on-disk evidence
# behind the catalog's headline thesis "gates mechanically constrain AI agents".
#
# A proof that can silently stop proving is worse than no proof. If a refactor
# breaks the proof script — the real guard stops being invoked, the block step
# loses its fail-guard, the file loses its +x bit, the toggle stops toggling —
# the thesis quietly becomes unfalsifiable with nothing to catch it. This guard
# is the regression backstop: it runs the proof in an ISOLATED ledger (so it
# never pollutes the real .ax-ledger trace) and proves it genuinely toggles, and
# it asserts the proof's block-step is non-vacuous (the 'blocked_rc -ne 1'
# fail-guard is present and the real guard is referenced).
#
# Checks:
#   (a) the proof script exists and is executable (the +x bit is load-bearing —
#       CI and humans run it directly);
#   (b) the proof TOGGLES end-to-end: run it with AX_LEDGER_DIR pointed at a
#       throwaway mktemp dir and require exit 0 (block-then-pass held) AND that
#       exactly the expected 2 actor=agent events landed in the temp ledger and
#       the real .ax-ledger was NOT written to;
#   (c) NON-VACUITY: the proof's block step depends on the guard ACTUALLY
#       blocking — the script must contain the 'blocked_rc -ne 1' fail-guard and
#       must reference the real controller_problemdetail_guard.sh. Without (c) a
#       proof that exits 0 unconditionally (e.g. the block check deleted) would
#       still pass (b), so (b) and (c) together are what make this regression-safe.
#
# Exit codes:
#   0 — proof script present, executable, toggles, and is non-vacuous
#   1 — a check failed (missing / non-exec / did not toggle / vacuous block step)
#   2 — usage / environment error (script path resolution, mktemp unavailable)
#
# Usage:
#   bash practices/evals/agent_block_proof_guard.sh
#   bash practices/evals/agent_block_proof_guard.sh --root DIR
#
# Bash 3.2 compatible. Fast: runs the proof once against a temp tree, no gradle.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "agent_block_proof_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"

PROOF="$REPO_ROOT/practices/scripts/ax-prove-gate-blocks-agent.sh"
REAL_GUARD_BASENAME="controller_problemdetail_guard.sh"

violations=0
fail() { echo "VIOLATION: $1" >&2; violations=$((violations + 1)); }

# ── (a) exists + executable ───────────────────────────────────────────────────
if [ ! -f "$PROOF" ]; then
    fail "falsification test missing: $PROOF not found"
    echo "agent_block_proof_guard: 1 violation — merge BLOCKED" >&2
    exit 1
fi
if [ ! -x "$PROOF" ]; then
    fail "falsification test not executable: chmod +x $PROOF (the +x bit is load-bearing — it is run directly)"
fi

# ── (c) NON-VACUITY of the block step (read the source) ───────────────────────
# Done before (b) so a vacuous-but-exit-0 proof is still reported, not masked.
# the fail-guard is `[ "$blocked_rc" -ne 1 ]` — match blocked_rc and the -ne 1
# test on one line (tolerant of the surrounding quoting/spacing).
if ! grep -Eq 'blocked_rc"?[[:space:]]+-ne[[:space:]]+1' "$PROOF"; then
    fail "proof block-step is vacuous: the 'blocked_rc -ne 1' fail-guard is gone — the proof would exit 0 even if the guard never blocked"
fi
if ! grep -q "$REAL_GUARD_BASENAME" "$PROOF"; then
    fail "proof no longer references the real guard ($REAL_GUARD_BASENAME) — it cannot be exercising the enforced gate"
fi

# ── (b) it genuinely TOGGLES, against an ISOLATED ledger ──────────────────────
if ! command -v mktemp >/dev/null 2>&1; then
    echo "agent_block_proof_guard: mktemp not in PATH (required to isolate the ledger)" >&2
    exit 2
fi
TMP_LEDGER="$(mktemp -d 2>/dev/null)" || { echo "agent_block_proof_guard: mktemp -d failed" >&2; exit 2; }
trap 'rm -rf "$TMP_LEDGER"' EXIT

# snapshot the real ledger size so we can prove the proof did NOT touch it
REAL_LEDGER="$REPO_ROOT/.ax-ledger/events.jsonl"
real_before="$( [ -f "$REAL_LEDGER" ] && wc -c < "$REAL_LEDGER" | tr -d ' ' || echo 0 )"

set +e
AX_LEDGER_DIR="$TMP_LEDGER" bash "$PROOF" >/dev/null 2>&1
proof_rc=$?
set -e 2>/dev/null || true

if [ "$proof_rc" -ne 0 ]; then
    fail "falsification test did NOT toggle: ax-prove-gate-blocks-agent.sh exited $proof_rc (expected 0 = block-then-pass held)"
else
    # the toggle must have recorded exactly the block→pass pair in the TEMP ledger
    agent_events="$( [ -f "$TMP_LEDGER/events.jsonl" ] && grep -c '"actor": "agent"' "$TMP_LEDGER/events.jsonl" || echo 0 )"
    if [ "$agent_events" -lt 2 ]; then
        fail "proof exited 0 but did not record the block→pass pair in the isolated ledger (actor=agent events: $agent_events, expected >= 2)"
    fi
fi

# the proof must NOT have written to the real ledger (AX_LEDGER_DIR isolation held)
real_after="$( [ -f "$REAL_LEDGER" ] && wc -c < "$REAL_LEDGER" | tr -d ' ' || echo 0 )"
if [ "$real_after" != "$real_before" ]; then
    fail "proof leaked into the real ledger: $REAL_LEDGER changed ($real_before → $real_after bytes) despite AX_LEDGER_DIR isolation"
fi

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "agent_block_proof_guard: $violations check(s) failed — the falsification test is broken; merge BLOCKED" >&2
    exit 1
fi

echo "agent_block_proof_guard: falsification test present, executable, non-vacuous, and toggles (block→pass held in isolated ledger)"
exit 0
