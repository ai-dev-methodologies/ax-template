#!/usr/bin/env bash
# practices/evals/multi_tenant_deferral_citation_guard.sh — R37 40th hard guard (codex P1-2).
#
# Closes the multi-tenant deferral traceability gap. Every blueprint manifest
# whose `not_for:` block mentions "Multi-tenant" MUST cite the spec ID
# `MULTI-TENANT-ISOLATION-DEFAULT-001` (verified at codex critic iter-2 to exist
# in specs/multi-tenant-l0.yaml).
#
# Usage:
#   bash practices/evals/multi_tenant_deferral_citation_guard.sh
#   bash practices/evals/multi_tenant_deferral_citation_guard.sh --fixtures
#   bash practices/evals/multi_tenant_deferral_citation_guard.sh --root DIR
#
# Exit codes: 0 PASS · 1 violation · 2 usage error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""
SPEC_ID="MULTI-TENANT-ISOLATION-DEFAULT-001"

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "multi_tenant_deferral_citation_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/multi_tenant_deferral_citation"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "multi_tenant_deferral_citation_guard: fixtures dir missing" >&2
        exit 2
    fi
    pass=0; fail=0
    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [multi_tenant_deferral_citation/$(basename "$sub")]"; pass=$((pass+1))
        else
            echo "FAIL [multi_tenant_deferral_citation/$(basename "$sub")] (expected pass)"; fail=$((fail+1))
        fi
    done
    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "FAIL [multi_tenant_deferral_citation/$(basename "$sub")] (expected fail)"; fail=$((fail+1))
        else
            echo "PASS [multi_tenant_deferral_citation/$(basename "$sub")]"; pass=$((pass+1))
        fi
    done
    if [ "$fail" -ne 0 ]; then exit 1; fi
    echo "multi_tenant_deferral_citation_guard: fixtures PASS ($pass)"
    exit 0
fi

ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
BLUEPRINTS_DIR="$ROOT/blueprints"

[ ! -d "$BLUEPRINTS_DIR" ] && exit 0

violations=0
for f in $(find "$BLUEPRINTS_DIR" -maxdepth 1 -name "*-manifest.yaml" 2>/dev/null); do
    # Exclude the manifest that DEFINES the multi-tenant primitives — it is the
    # source, not a deferral.
    base="$(basename "$f")"
    [ "$base" = "multi-tenant-manifest.yaml" ] && continue

    if grep -qE "Multi-tenant|multi-tenant|멀티테넌트" "$f"; then
        if ! grep -q "$SPEC_ID" "$f"; then
            echo "VIOLATION: $f mentions Multi-tenant deferral but does not cite $SPEC_ID" >&2
            violations=$((violations+1))
        fi
    fi
done

if [ "$violations" -ne 0 ]; then
    echo "multi_tenant_deferral_citation_guard: FAIL with $violations violations" >&2
    exit 1
fi

echo "multi_tenant_deferral_citation_guard: PASS"
exit 0
