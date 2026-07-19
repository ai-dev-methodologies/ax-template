#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S2.AUDIT-PII.XB/run-scenario-proof.sh
#
# DOGFOOD cell S2.AUDIT-PII.XB — cross-boundary AUDIT-PII vertical slice
# (admin audit-event viewer + inbound webhook signature-verification status).
# Composed from the catalog's audit-log L4 PII-redaction posture
# (AuditLogPiiRedactor / common.AuditPiiHelper#piiHash) and the
# webhook-signing-l0 BE spec's four-outcome verification contract,
# assembled into a thin AuditEventController/Service/Response slice under
# java/{violating-root,clean-root} plus a matching FE component under
# react/{violating,clean}.
#
# Proves the catalog composition mechanically BLOCKS two realistic
# AI-generated defects named by this dogfood brief, and lets the correct
# rewrite through:
#   1. the GAP: a BE audit record's PII field (actorEmail) crosses the
#      BE->FE boundary through a *Response DTO factory with no redaction —
#      nothing proved PII redaction survives BE->FE before this scenario.
#   2. the ADDITIONAL REQUIREMENT: a FE component that surfaces inbound
#      webhook signature-verification status ignores it and always renders
#      a static "Delivered" success UI, never a "signature could not be
#      verified" failure state (mirroring BE WEBHOOK-SIGN).
#
# Reuses the SAME harness contract as
# practices/consumer-proof/run-consumer-proof.sh (see that script + its
# README.md, and S3.b2b-admin's run-scenario-proof.sh for the sibling
# scenario-harness precedent this file mirrors):
#   - a VIOLATING fixture counts as BLOCKED only when its guard exits EXACTLY 1
#     AND the captured output contains the INTENDED signature string (never a
#     bare non-zero exit, never exit 2 usage/env error).
#   - a CLEAN fixture must exist, be actually SCANNED (positive-scan proof —
#     no vacuous "nothing to check"), and exit EXACTLY 0.
#   - every expected case MUST run (cardinality gate) — a dropped case fails
#     the whole proof loudly.
#
# ISOLATION: everything this script touches lives under this scenario dir
# (java/, react/, scenario-guards/). It never edits backend/src or
# frontend/src, and is NOT wired into run-all-guards.sh or R25.
#
# Usage: bash practices/consumer-proof/scenarios/S2.AUDIT-PII.XB/run-scenario-proof.sh
# Exit:  0 = proof holds · 1 = proof FALSIFIED / a case could not run.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SG="$SCRIPT_DIR/scenario-guards"

VR_JAVA="$SCRIPT_DIR/java/violating-root"
CR_JAVA="$SCRIPT_DIR/java/clean-root"
PKG_SUFFIX="backend/src/main/java/com/ax/template/authblueprint/auditeventxb"
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
echo " S2.AUDIT-PII.XB — consumer-proof scenario harness"
echo " Cross-boundary AUDIT-PII slice: audit-event viewer + webhook signature status"
echo "══════════════════════════════════════════════════════════════════════════"
echo
echo "── AUDIT-PII cross-boundary redaction (HAND-ROLLED — catalog has no standalone shell asset) ──"
run_guard "audit-pii-cross-boundary" "$SG/audit_pii_cross_boundary_guard.sh" \
    "$VR_JAVA" "$CR_JAVA" \
    "AUDIT_PII_CROSS_BOUNDARY_UNREDACTED" \
    "$CR_JAVA/$PKG_SUFFIX" "java"

echo
echo "── FE webhook signature-verification status UX (HAND-ROLLED — confirmed catalog gap) ──"
run_guard "webhook-signature-status-ux" "$SG/webhook_signature_status_ux_guard.sh" \
    "$VR_REACT" "$CR_REACT" \
    "WEBHOOK_SIGNATURE_STATUS_UX_MISSING" \
    "$CR_REACT" "tsx"

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
echo "PROOF HOLDS — S2.AUDIT-PII.XB composition mechanically blocks every named"
echo "violation and lets every clean rewrite through, non-vacuously."
exit 0
