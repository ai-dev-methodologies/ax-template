#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S2.AUDIT-PII.FE/run-scenario-proof.sh
#
# DOGFOOD cell S2.AUDIT-PII.FE — FE parse-error PII slice + multi-tenant
# TenantContext additional requirement.
#
# Proves two realistic AI-generated defects, each with a BLOCKED violating
# fixture and a scanned+PASS clean fixture:
#   1. NAMED GAP: a FE component reads a failed request's ProblemDetail body
#      directly (res.json().detail/.message) instead of routing it through
#      templates/L0/fork-receiver-kit/parse-error.ts's parseError() — the
#      only seam that applies the PII deny-list (sanitizeStoredError). This
#      dogfood ALSO found and CLOSED a real defect INSIDE parseError() itself
#      (its deny-list previously covered only the text/html fallback branch,
#      never the JSON detail/message branch every RFC 9457 ProblemDetail
#      actually takes) — see frontend/tests/parse-error-denylist.vitest.ts,
#      which failed 3/10 before the fix and passes 10/10 after. That fix does
#      not, by itself, protect a consumer who bypasses parseError() entirely,
#      which is what THIS scenario's guard proves.
#   2. ADDITIONAL REQUIREMENT: a runtime multi-tenant TenantContext primitive
#      that scopes every repository query to the caller tenant. The catalog
#      has no such runtime common/ primitive (only a design-time skeleton in
#      blueprints/multi-tenant-manifest.yaml and a guard-fixture double) — a
#      *Service that imports TenantContext but calls the plain
#      findById/findAll finders leaks cross-tenant data.
#
# Reuses the SAME harness contract as practices/consumer-proof/run-consumer-proof.sh
# (see that script + S2.AUDIT-PII.XB's run-scenario-proof.sh for the sibling
# scenario-harness precedent this file mirrors):
#   - a VIOLATING fixture counts as BLOCKED only when its guard exits EXACTLY 1
#     AND the captured output contains the INTENDED signature string.
#   - a CLEAN fixture must exist, be actually SCANNED (positive-scan proof),
#     and exit EXACTLY 0.
#   - every expected case MUST run (cardinality gate).
#
# ISOLATION: everything this script touches lives under this scenario dir
# (java/, react/, scenario-guards/). The ONE exception, tracked explicitly
# below, is the catalog closure itself: templates/L0/fork-receiver-kit/
# parse-error.ts (the specific file the gap lives in) plus its new test
# frontend/tests/parse-error-denylist.vitest.ts — both are the CLOSURE
# artifact, not scenario scaffolding, and are NOT touched by this script.
# This script never edits backend/src or frontend/src, and is NOT wired into
# run-all-guards.sh or R25.
#
# Usage: bash practices/consumer-proof/scenarios/S2.AUDIT-PII.FE/run-scenario-proof.sh
# Exit:  0 = proof holds · 1 = proof FALSIFIED / a case could not run.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SG="$SCRIPT_DIR/scenario-guards"

VR_JAVA="$SCRIPT_DIR/java/violating-root"
CR_JAVA="$SCRIPT_DIR/java/clean-root"
PKG_SUFFIX="backend/src/main/java/com/ax/template/authblueprint/tenantdocfe"
VR_REACT="$SCRIPT_DIR/react/violating"
CR_REACT="$SCRIPT_DIR/react/clean"

PASS=0
FAIL=0
FAILED_CASES=""
EXPECTED_TOTAL=2
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

# run_guard <label> <guard-path> <violating-root> <clean-root> <signature> <clean-scan-dir> <scan-ext>
run_guard() {
    local label="$1" guard="$2" varg="$3" carg="$4" signature="$5" scandir="$6" ext="${7:-java}"
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
        n="$(find "$scandir" -name "*.${ext}" 2>/dev/null | wc -l | tr -d ' ')"
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
echo " S2.AUDIT-PII.FE — consumer-proof scenario harness"
echo " FE parse-error PII slice + multi-tenant TenantContext additional requirement"
echo "══════════════════════════════════════════════════════════════════════════"
echo
echo "── FE error-display bypasses parseError() deny-list seam (HAND-ROLLED) ──"
run_guard "fe-error-display-pii" "$SG/fe_error_display_pii_guard.sh" \
    "$VR_REACT" "$CR_REACT" \
    "FE_ERROR_DISPLAY_PII_UNSANITIZED" \
    "$CR_REACT" "tsx"

echo
echo "── Repository query not scoped to caller tenant (HAND-ROLLED — confirmed catalog gap) ──"
run_guard "tenant-scope-missing" "$SG/tenant_scope_missing_guard.sh" \
    "$VR_JAVA" "$CR_JAVA" \
    "TENANT_SCOPE_MISSING" \
    "$CR_JAVA/$PKG_SUFFIX" "java"

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
echo "PROOF HOLDS — S2.AUDIT-PII.FE composition mechanically blocks every named"
echo "violation and lets every clean rewrite through, non-vacuously."
echo
echo "SEPARATE closure evidence (not part of this scenario's guard run — run"
echo "independently): cd frontend && npx vitest run tests/parse-error-denylist.vitest.ts"
exit 0
