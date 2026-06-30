#!/usr/bin/env bash
# practices/evals/vacuity_guard_selfproof_guard.sh — anti-meta-trap for the vacuity gate.
#
# A guard that catches hollow tests can ITSELF rot into a hollow guard (METHODOLOGY honest
# limit: "a guard that only asserts the keyword vacuity_class appears is itself a vacuous
# check — the same trap it purports to catch"). This self-proof keeps vacuity_class_proof_guard.sh
# honest by asserting it STILL discriminates, on committed fixtures, with no gradle:
#
#   1. it FAILS (exit 1) on the bundled HOLLOW fixture (a SURVIVED gate mutant);
#   2. it PASSES (exit 0) on the bundled TIGHT fixture (the same gate, KILLED) — so the
#      failure in (1) is non-vacuous (the guard is not a constant "always exit 1");
#   3. its source still contains the SURVIVED/non-KILLED → `exit 1` blocking branch.
#
# Exit 0 = the vacuity guard still blocks hollow gates. Exit 1 = the guard has gone vacuous (BLOCK).
# Exit 2 = usage / tooling error.

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GUARD="$SCRIPT_DIR/vacuity_class_proof_guard.sh"
FIX="$SCRIPT_DIR/fixtures/vacuity-class-proof"
HOLLOW="$FIX/hollow_survived.xml"
TIGHT="$FIX/tight_killed.xml"
GATE="com.example.Foo#bar"

FAIL=0

[ -f "$GUARD" ]  || { echo "vacuity_guard_selfproof_guard: FAIL — vacuity_class_proof_guard.sh missing" >&2; exit 1; }
[ -f "$HOLLOW" ] || { echo "vacuity_guard_selfproof_guard: FAIL — hollow fixture missing: $HOLLOW" >&2; exit 1; }
[ -f "$TIGHT" ]  || { echo "vacuity_guard_selfproof_guard: FAIL — tight fixture missing: $TIGHT" >&2; exit 1; }

# 1. HOLLOW fixture must still BLOCK (exit 1)
bash "$GUARD" --report "$HOLLOW" --gate-method "$GATE" --kill-mutator TRUE_RETURNS \
    --vacuity-class fail_closed_default >/dev/null 2>&1
rc=$?
if [ "$rc" -ne 1 ]; then
    echo "vacuity_guard_selfproof_guard: FAIL — vacuity guard did NOT block the hollow (SURVIVED) fixture (exit $rc, expected 1)." >&2
    echo "    The non-vacuity gate has itself gone vacuous — it no longer catches a green-but-hollow test." >&2
    FAIL=1
fi

# 2. TIGHT fixture must PASS (exit 0) — proves (1) is discriminating, not a constant failure
bash "$GUARD" --report "$TIGHT" --gate-method "$GATE" --kill-mutator TRUE_RETURNS \
    --vacuity-class fail_closed_default >/dev/null 2>&1
rc=$?
if [ "$rc" -ne 0 ]; then
    echo "vacuity_guard_selfproof_guard: FAIL — vacuity guard rejected the TIGHT (KILLED) fixture (exit $rc, expected 0)." >&2
    echo "    A guard that fails even on a killed mutant is a constant blocker, not a real check." >&2
    FAIL=1
fi

# 3. source still carries the SURVIVED/non-KILLED → exit 1 blocking branch
if ! grep -qE "status != 'KILLED'|non_killed|SURVIVED" "$GUARD"; then
    echo "vacuity_guard_selfproof_guard: FAIL — vacuity guard source lost its SURVIVED/non-KILLED detection." >&2
    FAIL=1
fi
if ! grep -qE 'sys\.exit\(1\)|exit 1' "$GUARD"; then
    echo "vacuity_guard_selfproof_guard: FAIL — vacuity guard source lost its 'exit 1' blocking branch." >&2
    FAIL=1
fi

if [ "$FAIL" -ne 0 ]; then
    echo "" >&2
    echo "vacuity_guard_selfproof_guard: FAIL — the non-vacuity gate is no longer trustworthy." >&2
    exit 1
fi

echo "vacuity_guard_selfproof_guard: PASS — vacuity gate still blocks hollow (SURVIVED) and admits tight (KILLED)"
exit 0
