#!/usr/bin/env bash
# practices/consumer-proof/run-consumer-proof.sh
#
# ADVERSARIAL CONSUMER-PROOF HARNESS
# ----------------------------------
# Proves ax-template's catalog MECHANICALLY BLOCKS rule-violating code in a
# consumer context, and does NOT block the correct rewrite. The proof is
# falsifiable:
#   * every VIOLATING fixture MUST make its gate exit non-zero  (blocked)
#   * every CLEAN fixture MUST make its gate exit zero          (passes)
# If ANY violating fixture slips through, or ANY clean fixture is blocked, the
# thesis is FALSIFIED and this harness exits 1 LOUDLY naming the case.
#
# Lane A — React/ESLint  (@ax/eslint-plugin-ax): ZERO path coupling. Just install
#          the plugin and the rules fire on arbitrary React/TSX.
# Lane B — Java/Spring shell guards: convention-path coupling. The consumer must
#          adopt the repo's package path (com.ax.template.authblueprint) for the
#          guards' --root scan to reach the code — honest about that coupling.
#
# This is a STANDALONE probe. It is NOT wired into R25 (keeps R25 fast +
# dependency-light). Run it manually or as a CI probe (see README.md).
#
# Usage:  bash practices/consumer-proof/run-consumer-proof.sh
# Exit:   0 = proof holds · 1 = proof FALSIFIED (or a lane could not run)

set -u

# ── self-locate ──────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
EVALS="$REPO_ROOT/practices/evals"
REACT_DIR="$SCRIPT_DIR/react"
JAVA_DIR="$SCRIPT_DIR/java"
PKG_SUFFIX="backend/src/main/java/com/ax/template/authblueprint"

PASS=0
FAIL=0
SKIP=0
FAILED_CASES=""

green() { printf '\033[32m%s\033[0m' "$1"; }
red()   { printf '\033[31m%s\033[0m' "$1"; }

# record_case <label> <expectation-met? 0/1> <detail>
record_case() {
    local label="$1" ok="$2" detail="$3"
    if [ "$ok" -eq 0 ]; then
        PASS=$((PASS + 1))
        printf '  [%s] %-48s %s\n' "$(green PASS)" "$label" "$detail"
    else
        FAIL=$((FAIL + 1))
        FAILED_CASES="$FAILED_CASES\n    - $label ($detail)"
        printf '  [%s] %-48s %s\n' "$(red FAIL)" "$label" "$detail"
    fi
}

echo "=================================================================="
echo " ax-template — ADVERSARIAL CONSUMER-PROOF"
echo " (violating -> MUST block · clean -> MUST pass)"
echo "=================================================================="

# ══════════════════════════════════════════════════════════════════════════════
# LANE A — React / ESLint (convention-free)
# ══════════════════════════════════════════════════════════════════════════════
echo
echo "── LANE A: React / @ax/eslint-plugin-ax (zero path coupling) ──"

if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
    echo "  SKIP: node/npm not on PATH — cannot run Lane A."
    echo "        Install Node >=20 and run: (cd $REACT_DIR && npm install)"
    SKIP=$((SKIP + 1))
elif [ ! -d "$REACT_DIR/node_modules" ]; then
    echo "  Lane A deps not installed — installing now..."
    if ( cd "$REACT_DIR" && npm install --no-audit --no-fund >/dev/null 2>&1 ); then
        echo "  npm install OK"
    else
        echo "  SKIP: npm install failed — cannot run Lane A."
        SKIP=$((SKIP + 1))
    fi
fi

# Only proceed with Lane A if deps are present.
if [ -d "$REACT_DIR/node_modules" ]; then
    # Each violating fixture -> the intended rule. We assert the specific rule id
    # appears AND exit is non-zero (blocked).
    #   fixture-relpath | expected-rule-id
    LANE_A_VIOLATING="\
fixtures/violating/array-mutate.tsx|ax/no-array-mutate-on-state
fixtures/violating/functional-setstate.tsx|ax/prefer-functional-setstate
fixtures/violating/server-state.tsx|ax/no-server-state-in-local-state
fixtures/violating/src/app/dashboard/page.tsx|ax/no-god-route"

    while IFS='|' read -r relpath rule_id; do
        [ -z "$relpath" ] && continue
        out="$( cd "$REACT_DIR" && npx eslint "$relpath" 2>&1 )"
        code=$?
        if [ "$code" -ne 0 ] && printf '%s' "$out" | grep -q "$rule_id"; then
            record_case "A/violating: $(basename "$relpath")" 0 "blocked by $rule_id (exit $code)"
        else
            record_case "A/violating: $(basename "$relpath")" 1 "NOT blocked / wrong rule (exit $code, wanted $rule_id)"
        fi
    done <<EOF
$LANE_A_VIOLATING
EOF

    # Clean dir -> must be entirely silent (exit 0).
    out="$( cd "$REACT_DIR" && npx eslint fixtures/clean 2>&1 )"; code=$?
    if [ "$code" -eq 0 ]; then
        record_case "A/clean: fixtures/clean" 0 "passes (exit 0)"
    else
        record_case "A/clean: fixtures/clean" 1 "clean fixtures were flagged (exit $code)"
        printf '%s\n' "$out" | sed 's/^/      /'
    fi
fi

# ══════════════════════════════════════════════════════════════════════════════
# LANE B — Java / Spring shell guards (convention-path coupling)
# ══════════════════════════════════════════════════════════════════════════════
echo
echo "── LANE B: Java shell guards (consumer adopts the package path) ──"

VR="$JAVA_DIR/violating-root"
CR="$JAVA_DIR/clean-root"

# Each guard has its OWN --root convention (verified by reading the guard):
#   * repo-root style  (guard appends the package suffix / cd's + relative):
#       controller_problemdetail_guard, money_boundary_seam_guard,
#       entity_migration_guard, role_literal_guard   -> pass the *root* dir
#   * scan-dir style   (guard globs *Controller.java under --root directly):
#       controller_repository_shell_guard            -> pass the *package* dir
#
# SIGNATURE-AWARE BLOCKING (hardening — mirrors Lane A's rigor)
# ---------------------------------------------------------------------------
# A raw non-zero exit is NOT sufficient proof that the guard blocked the
# INTENDED violation. Each guard can also exit non-zero for reasons that have
# nothing to do with the fixture's deliberate defect:
#   * ZERO_SCAN (controller_problemdetail_guard) — the naming-convention walk
#     found no @ExceptionHandler at all (e.g. after a fixture rename) and
#     fails closed on principle, not because it detected the Map violation.
#   * exit 2 (any guard) — a usage/environment error (missing dir, missing
#     python3, unparsable enum, ...), not a detection.
#   * an unrelated parse failure that happens to exit 1 for a different
#     reason than the one this case is supposed to prove.
# So each case below carries the SIGNATURE string that guard prints ONLY when
# it has actually fired on the intended defect (read from the guard's own
# violation-message format — see the per-guard scripts in practices/evals/).
# A case counts as "blocked" ONLY IF: exit != 0 AND exit != 2 AND the
# captured stdout+stderr CONTAINS that signature. Otherwise the case is
# INDETERMINATE/ERROR, not a valid block, and the harness fails loudly naming
# what actually happened (exit 2 gets its own explicit "environment error"
# message so it is never silently folded into "blocked").
#
# run_guard <label> <guard-script> <violating-arg> <clean-arg> <signature>
run_guard() {
    local label="$1" guard="$2" varg="$3" carg="$4" signature="$5"
    if [ ! -f "$EVALS/$guard" ]; then
        record_case "B: $label" 1 "guard script not found: $guard"
        return
    fi
    local vout vc cout cc
    vout="$(bash "$EVALS/$guard" --root "$varg" 2>&1)"; vc=$?
    cout="$(bash "$EVALS/$guard" --root "$carg" 2>&1)"; cc=$?

    # exit 2 = usage/environment error on EITHER side — never a valid signal
    # in either direction (not a block, not a clean pass). Report and fail.
    if [ "$vc" -eq 2 ] || [ "$cc" -eq 2 ]; then
        record_case "B/$label" 1 "ENVIRONMENT ERROR — guard exited 2 (violating=$vc, clean=$cc); not a valid block or pass, cannot prove the thesis"
        return
    fi

    # A "block" requires BOTH a non-zero exit AND the intended violation
    # signature actually present in the guard's captured output. Exit != 0
    # with the signature ABSENT (e.g. ZERO_SCAN) is an unrelated failure
    # masquerading as a block and must NOT be credited.
    local v_blocked=1
    if [ "$vc" -ne 0 ] && printf '%s' "$vout" | grep -qF "$signature"; then
        v_blocked=0
    fi

    if [ "$v_blocked" -eq 0 ] && [ "$cc" -eq 0 ]; then
        record_case "B/$label" 0 "violating blocked (exit $vc, signature '$signature' present), clean passes (exit $cc)"
    else
        local why=""
        if [ "$v_blocked" -ne 0 ]; then
            if [ "$vc" -eq 0 ]; then
                why="violating SLIPPED THROUGH (exit 0)"
            else
                why="violating exited $vc but signature '$signature' ABSENT — INDETERMINATE (unrelated failure, e.g. ZERO_SCAN — not a valid block)"
            fi
        fi
        [ "$cc" -ne 0 ] && why="${why:+$why; }clean was BLOCKED (exit $cc)"
        record_case "B/$label" 1 "$why"
    fi
}

run_guard "controller_problemdetail_guard"    controller_problemdetail_guard.sh    "$VR"              "$CR"              "Map<String,String>"
run_guard "controller_repository_shell_guard" controller_repository_shell_guard.sh "$VR/$PKG_SUFFIX"  "$CR/$PKG_SUFFIX"  "OrderAdminController"
run_guard "money_boundary_seam_guard"         money_boundary_seam_guard.sh         "$VR"              "$CR"              "InvoiceCalculator"
run_guard "entity_migration_guard"            entity_migration_guard.sh            "$VR"              "$CR"              "widget_ghost"
run_guard "role_literal_guard"                role_literal_guard.sh                "$VR"              "$CR"              "ROLE_ADMINS"

# ══════════════════════════════════════════════════════════════════════════════
# VERDICT
# ══════════════════════════════════════════════════════════════════════════════
echo
echo "=================================================================="
echo " RESULT: $PASS passed · $FAIL failed · $SKIP lane(s) skipped"
echo "=================================================================="

if [ "$FAIL" -ne 0 ]; then
    echo
    echo "PROOF FALSIFIED — the following case(s) broke the thesis:"
    printf "$FAILED_CASES\n"
    echo
    echo "A violating fixture that slipped through, or a clean fixture that was"
    echo "blocked, means the catalog is NOT mechanically enforcing as claimed."
    exit 1
fi

if [ "$SKIP" -ne 0 ]; then
    echo
    echo "NOTE: $SKIP lane(s) were skipped (missing toolchain). Every case that"
    echo "DID run held the proof, but the skipped lane was not exercised."
    # A skipped lane is not a falsification, but it is not a full proof either.
    # Exit non-zero so CI cannot green a partial run silently.
    exit 1
fi

echo
echo "PROOF HOLDS: every violating fixture was blocked and every clean fixture"
echo "passed. ax-template's catalog mechanically enforces in a consumer context."
exit 0
