#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S3.e-commerce/run-scenario-proof.sh
#
# DOGFOOD cell S3.e-commerce — Korean-enterprise CHECKOUT vertical slice
# (cart -> order -> payment -> receipt; ADMIN refund/list). Proves the catalog
# composition (CRUD L4 + payment/notification/audit-log L4 + L2 blocks + L0
# kit) still mechanically BLOCKS realistic AI-generated rule violations when
# assembled into a slice, and lets the correct rewrite through.
#
# Reuses the SAME harness contract as practices/consumer-proof/run-consumer-proof.sh
# (see that script + its README.md for the full rationale):
#   - a VIOLATING fixture counts as BLOCKED only when its guard exits EXACTLY 1
#     AND the captured output contains the INTENDED signature string (never a
#     bare non-zero exit, never exit 2 usage/env error).
#   - a CLEAN fixture must exist, be actually SCANNED (positive-scan proof —
#     no vacuous "nothing to check"), and exit EXACTLY 0.
#   - every expected case MUST run (cardinality gate) — a dropped case fails
#     the whole proof loudly.
#
# ISOLATION: everything this script touches lives under this scenario dir
# (java/, react/, scenario-guards/) plus READ-ONLY calls into the real
# practices/evals/*.sh catalog guards. It never edits backend/src or
# frontend/src, and is NOT wired into run-all-guards.sh or R25.
#
# Usage: bash practices/consumer-proof/scenarios/S3.e-commerce/run-scenario-proof.sh
# Exit:  0 = proof holds · 1 = proof FALSIFIED / a case could not run.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
EVALS="$REPO_ROOT/practices/evals"
SG="$SCRIPT_DIR/scenario-guards"

VR_JAVA="$SCRIPT_DIR/java/violating-root"
CR_JAVA="$SCRIPT_DIR/java/clean-root"
PKG_SUFFIX="backend/src/main/java/com/ax/template/authblueprint"
VR_REACT="$SCRIPT_DIR/react/violating"
CR_REACT="$SCRIPT_DIR/react/clean"

PASS=0
FAIL=0
FAILED_CASES=""
EXPECTED_TOTAL=4
ran=0

green() { printf '\033[32m%s\033[0m' "$1"; }
red()   { printf '\033[31m%s\033[0m' "$1"; }

record_case() {
    local label="$1" ok="$2" detail="$3"
    if [ "$ok" -eq 0 ]; then
        PASS=$((PASS + 1))
        printf '  [%s] %-46s %s\n' "$(green PASS)" "$label" "$detail"
    else
        FAIL=$((FAIL + 1))
        FAILED_CASES="$FAILED_CASES
    - $label ($detail)"
        printf '  [%s] %-46s %s\n' "$(red FAIL)" "$label" "$detail"
    fi
}

# run_guard <label> <guard-path> <violating-root> <clean-root> <signature> <clean-scan-dir>
run_guard() {
    local label="$1" guard="$2" varg="$3" carg="$4" signature="$5" scandir="$6"
    ran=$((ran + 1))

    if [ ! -f "$guard" ]; then
        record_case "$label" 1 "guard script not found: $guard"
        return
    fi

    local vout vc cout cc
    vc=0; vout="$(bash "$guard" --root "$varg" 2>&1)" || vc=$?
    cc=0; cout="$(bash "$guard" --root "$carg" 2>&1)" || cc=$?

    if [ "$vc" -eq 2 ] || [ "$cc" -eq 2 ]; then
        record_case "$label" 1 "ENVIRONMENT ERROR — guard exited 2 (violating=$vc clean=$cc)"
        return
    fi
    if [ "$vc" -gt 1 ] || [ "$cc" -gt 1 ]; then
        record_case "$label" 1 "ENVIRONMENT ERROR — unexpected exit (violating=$vc clean=$cc)"
        return
    fi

    local v_blocked=1
    if [ "$vc" -eq 1 ] && printf '%s' "$vout" | grep -qF -- "$signature"; then
        v_blocked=0
    fi

    local clean_ok=0 clean_why=""
    if [ "$cc" -ne 0 ]; then
        clean_ok=1; clean_why="clean was BLOCKED (exit $cc)"
    fi
    if [ "$clean_ok" -eq 0 ]; then
        local n
        n="$(find "$scandir" \( -name '*.java' -o -name '*.tsx' \) 2>/dev/null | wc -l | tr -d ' ')"
        if [ "${n:-0}" -eq 0 ]; then
            clean_ok=1; clean_why="NO source files under clean scan dir ($scandir) — vacuous"
        fi
    fi
    if [ "$clean_ok" -eq 0 ]; then
        if printf '%s' "$cout" | grep -Eq 'nothing to check|SKIP|0 \*'; then
            clean_ok=1; clean_why="VACUOUS clean pass — guard scanned nothing: $(printf '%s' "$cout" | tr '\n' ' ' | cut -c1-90)"
        fi
    fi

    if [ "$v_blocked" -eq 0 ] && [ "$clean_ok" -eq 0 ]; then
        record_case "$label" 0 "violating BLOCKED (exit 1, signature '$signature'), clean scanned+PASS (exit 0)"
    else
        local why=""
        if [ "$v_blocked" -ne 0 ]; then
            if [ "$vc" -eq 0 ]; then
                why="violating SLIPPED THROUGH (exit 0)"
            else
                why="violating exit $vc but signature '$signature' ABSENT"
            fi
        fi
        [ "$clean_ok" -ne 0 ] && why="${why:+$why; }$clean_why"
        record_case "$label" 1 "$why"
    fi
}

echo "══════════════════════════════════════════════════════════════════════════"
echo " S3.e-commerce — consumer-proof scenario harness"
echo " CHECKOUT slice: cart -> order -> payment -> receipt (ADMIN list/refund)"
echo "══════════════════════════════════════════════════════════════════════════"
echo
echo "── money-boundary seam (catalog-reused: money_boundary_seam_guard.sh) ──"
run_guard "money-boundary-seam" "$EVALS/money_boundary_seam_guard.sh" \
    "$VR_JAVA" "$CR_JAVA" \
    "money-seam violation" \
    "$CR_JAVA/$PKG_SUFFIX"

echo
echo "── controller/repository layering (catalog-reused: controller_repository_shell_guard.sh) ──"
run_guard "controller-repository-layering" "$EVALS/controller_repository_shell_guard.sh" \
    "$VR_JAVA/$PKG_SUFFIX" "$CR_JAVA/$PKG_SUFFIX" \
    "OrderAdminController" \
    "$CR_JAVA/$PKG_SUFFIX"

echo
echo "── unbounded order findAll (HAND-ROLLED — catalog has no standalone shell asset) ──"
run_guard "unbounded-order-findall" "$SG/unbounded_repository_read_guard.sh" \
    "$VR_JAVA" "$CR_JAVA" \
    "UNBOUNDED_REPOSITORY_READ" \
    "$CR_JAVA/$PKG_SUFFIX"

echo
echo "── FE locale-aware number/date formatting (HAND-ROLLED — confirmed catalog gap, CANARY-001) ──"
run_guard "locale-aware-formatting" "$SG/locale_format_guard.sh" \
    "$VR_REACT" "$CR_REACT" \
    "LOCALE_FORMAT_VIOLATION" \
    "$CR_REACT"

echo
echo "══════════════════════════════════════════════════════════════════════════"
if [ "$ran" -ne "$EXPECTED_TOTAL" ]; then
    echo "CARDINALITY GATE FAILED: expected $EXPECTED_TOTAL cases, ran $ran"
    exit 1
fi
echo "Cases: $PASS passed, $FAIL failed (of $EXPECTED_TOTAL expected)"
if [ "$FAIL" -gt 0 ]; then
    echo "PROOF FALSIFIED. Failing cases:$FAILED_CASES"
    exit 1
fi
echo "PROOF HOLDS — S3.e-commerce composition mechanically blocks every named"
echo "violation and lets every clean rewrite through, non-vacuously."
exit 0
