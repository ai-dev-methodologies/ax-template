#!/usr/bin/env bash
# practices/evals/agent_block_proof_guard.sh
# 2026-06-22 falsification-test regression guard (single guard; verifies N proofs).
# 2026-06-24 (BACKLOG P2-3): generalized to backstop EVERY enforcement-surface
#            falsification proof, not just the ProblemDetail one.
#
# practices/scripts/ax-prove-*-gate-blocks-agent.sh are the project's FALSIFICATION
# TESTS — the on-disk evidence behind the headline thesis "gates mechanically
# constrain AI agents". Each: an "agent" writes a rule-violating artifact, the REAL
# guard BLOCKS it (exit 1), the agent corrects it, the SAME guard PASSES (exit 0),
# and the block→pass pair is logged for actor=agent. Two surfaces are proven:
#   - ax-prove-gate-blocks-agent.sh          — a Map-returning @ExceptionHandler vs
#       controller_problemdetail_guard (run-all-guards surface, RFC-9457 shape);
#   - ax-prove-evidence-gate-blocks-agent.sh — a placeholder/empty `evidence:` block
#       vs evidence_guard (pre-commit surface, the #1 anti-hallucination gate).
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

# Each enforcement-surface falsification proof: "<proof-script> <real-guard-basename>".
# Both prove the block→pass→log(actor=agent) loop; this guard backstops them all
# (BACKLOG P2-3 surface-coverage map):
#   - ax-prove-gate-blocks-agent.sh          → controller_problemdetail_guard.sh
#                                              (run-all-guards surface: RFC-9457 shape)
#   - ax-prove-evidence-gate-blocks-agent.sh → evidence_guard.sh
#                                              (pre-commit surface: evidence anchoring)
PROOFS="
practices/scripts/ax-prove-gate-blocks-agent.sh|controller_problemdetail_guard.sh
practices/scripts/ax-prove-evidence-gate-blocks-agent.sh|evidence_guard.sh
"

REAL_LEDGER="$REPO_ROOT/.ax-ledger/events.jsonl"

violations=0
fail() { echo "VIOLATION: $1" >&2; violations=$((violations + 1)); }

if ! command -v mktemp >/dev/null 2>&1; then
    echo "agent_block_proof_guard: mktemp not in PATH (required to isolate the ledger)" >&2
    exit 2
fi

# Verify one falsification proof: exists + executable (a), non-vacuous (c), toggles
# against an isolated ledger without leaking into the real one (b).
check_proof() {
    local proof="$1" guard_base="$2" tag
    tag="$(basename "$proof")"

    # ── (a) exists + executable ──────────────────────────────────────────────
    if [ ! -f "$proof" ]; then
        fail "falsification test missing: $proof not found"
        return
    fi
    if [ ! -x "$proof" ]; then
        fail "falsification test not executable ($tag): chmod +x $proof (the +x bit is load-bearing — it is run directly)"
    fi

    # ── (c) NON-VACUITY of the block step (read the source) ──────────────────
    # Done before (b) so a vacuous-but-exit-0 proof is still reported, not masked.
    if ! grep -Eq 'blocked_rc"?[[:space:]]+-ne[[:space:]]+1' "$proof"; then
        fail "proof block-step is vacuous ($tag): the 'blocked_rc -ne 1' fail-guard is gone — it would exit 0 even if the guard never blocked"
    fi
    if ! grep -q "$guard_base" "$proof"; then
        fail "proof no longer references the real guard ($guard_base) in $tag — it cannot be exercising the enforced gate"
    fi

    # ── (b) it genuinely TOGGLES, against an ISOLATED ledger ─────────────────
    local tmp_ledger real_before proof_rc agent_events real_after
    tmp_ledger="$(mktemp -d 2>/dev/null)" || { echo "agent_block_proof_guard: mktemp -d failed" >&2; exit 2; }
    real_before="$( [ -f "$REAL_LEDGER" ] && wc -c < "$REAL_LEDGER" | tr -d ' ' || echo 0 )"

    set +e
    AX_LEDGER_DIR="$tmp_ledger" bash "$proof" >/dev/null 2>&1
    proof_rc=$?
    set -e 2>/dev/null || true

    if [ "$proof_rc" -ne 0 ]; then
        fail "falsification test did NOT toggle ($tag): exited $proof_rc (expected 0 = block-then-pass held)"
    else
        # `grep -c` prints "0" AND exits 1 on zero matches — a bare `|| echo 0`
        # emits "0\n0", and `[ "0\n0" -lt 2 ]` is an arithmetic ERROR that this
        # if-branch would silently treat as "condition false" → false-green
        # proof. Capture the count and fall back only on a truly empty capture.
        if [ -f "$tmp_ledger/events.jsonl" ]; then
            agent_events="$(grep -c '"actor": "agent"' "$tmp_ledger/events.jsonl" 2>/dev/null || true)"
        else
            agent_events=""
        fi
        agent_events="${agent_events:-0}"
        if [ "$agent_events" -lt 2 ]; then
            fail "proof exited 0 but did not record the block→pass pair ($tag): actor=agent events: $agent_events, expected >= 2"
        fi
    fi

    # the proof must NOT have written to the real ledger (AX_LEDGER_DIR isolation held)
    real_after="$( [ -f "$REAL_LEDGER" ] && wc -c < "$REAL_LEDGER" | tr -d ' ' || echo 0 )"
    if [ "$real_after" != "$real_before" ]; then
        fail "proof leaked into the real ledger ($tag): $REAL_LEDGER changed ($real_before → $real_after bytes) despite AX_LEDGER_DIR isolation"
    fi

    rm -rf "$tmp_ledger"
}

while IFS='|' read -r proof_rel guard_base; do
    [ -z "$proof_rel" ] && continue
    check_proof "$REPO_ROOT/$proof_rel" "$guard_base"
done <<EOF
$(printf '%s\n' "$PROOFS")
EOF

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "agent_block_proof_guard: $violations check(s) failed — a falsification test is broken; merge BLOCKED" >&2
    exit 1
fi

echo "agent_block_proof_guard: 2 falsification tests present, executable, non-vacuous, and toggle (block→pass held in isolated ledgers)"
exit 0
