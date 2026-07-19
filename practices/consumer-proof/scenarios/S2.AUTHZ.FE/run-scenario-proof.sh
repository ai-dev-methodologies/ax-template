#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S2.AUTHZ.FE/run-scenario-proof.sh
#
# DOGFOOD cell S2.AUTHZ.FE — FE admin-action-without-role-gate slice + SSRF
# URL-allowlist additional requirement, composed on the L4 webhook vertical's
# admin surface (templates/L4/webhook/app/(admin)/webhooks/page.tsx).
#
# Proves two realistic AI-generated defects, each with a BLOCKED violating
# fixture and a scanned+PASS clean fixture:
#   1. NAMED GAP (S2.AUTHZ.FE, status: partial in coverage-map.yaml): a new
#      admin-only action ("Send test delivery" — triggers a real outbound
#      server-side fetch) is added to an already role-gated admin surface as
#      a standalone component, and — because the ENCLOSING PAGE already gates
#      on useCallerRole() — the new component itself renders unconditionally
#      with no gate of its own. Rule docs + a real L0 primitive
#      (use-caller-id.ts) exist, but NONE of the 14 ax/* ESLint rules enforce
#      self-gating at the component/function level (OWASP API5:2023 BFLA).
#   2. ADDITIONAL REQUIREMENT (SSRF, matches canary-gaps.yaml CANARY-005's gap
#      class but at a different call site — test-delivery time, not
#      registration time): that same "Send test delivery" action causes the
#      backend to re-fetch the endpoint's STORED target URL on demand, with
#      NO allowlist re-check — a URL that was benign at registration can be
#      repointed at an internal host (169.254.169.254, 127.0.0.1, RFC 1918)
#      by the time this later fetch runs. Confirmed absent from the catalog:
#      `grep -rliE "ssrf|url.?allowlist|allowlist.?url" practices/rules/*.md
#      practices/evals/*.sh` — 0 matches.
#
# Reuses the SAME harness contract as practices/consumer-proof/run-consumer-proof.sh
# and its sibling scenario-harness precedent (S2.AUDIT-PII.FE/run-scenario-proof.sh):
#   - a VIOLATING fixture counts as BLOCKED only when its guard exits EXACTLY 1
#     AND the captured output contains the INTENDED signature string.
#   - a CLEAN fixture must exist, be actually SCANNED (positive-scan proof),
#     and exit EXACTLY 0.
#   - every expected case MUST run (cardinality gate).
#
# ISOLATION: everything this script touches lives under this scenario dir
# (java/, react/, scenario-guards/). No catalog file (practices/rules,
# practices-react/eslint-plugin-ax, backend/src, frontend/src) is read,
# written, or wired into run-all-guards.sh / R25 by this script.
#
# Usage: bash practices/consumer-proof/scenarios/S2.AUTHZ.FE/run-scenario-proof.sh
# Exit:  0 = proof holds · 1 = proof FALSIFIED / a case could not run.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SG="$SCRIPT_DIR/scenario-guards"

VR_JAVA="$SCRIPT_DIR/java/violating-root"
CR_JAVA="$SCRIPT_DIR/java/clean-root"
PKG_SUFFIX="backend/src/main/java/com/ax/template/authblueprint/webhooktest"
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
echo " S2.AUTHZ.FE — consumer-proof scenario harness"
echo " FE admin-action role-gate slice + SSRF URL-allowlist additional requirement"
echo "══════════════════════════════════════════════════════════════════════════"
echo
echo "── Admin-only action rendered without its own caller-role gate (HAND-ROLLED — confirmed catalog gap) ──"
run_guard "fe-admin-action-missing-role-gate" "$SG/fe_admin_action_missing_role_gate_guard.sh" \
    "$VR_REACT" "$CR_REACT" \
    "FE_ADMIN_ACTION_MISSING_ROLE_GATE" \
    "$CR_REACT" "tsx"

echo
echo "── Outbound server-side fetch of user-supplied URL with no SSRF allowlist check (HAND-ROLLED — confirmed catalog gap, CANARY-005) ──"
run_guard "ssrf-missing-allowlist-check" "$SG/ssrf_missing_allowlist_check_guard.sh" \
    "$VR_JAVA" "$CR_JAVA" \
    "SSRF_MISSING_URL_ALLOWLIST_CHECK" \
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
echo "PROOF HOLDS — S2.AUTHZ.FE composition mechanically blocks every named"
echo "violation and lets every clean rewrite through, non-vacuously."
exit 0
