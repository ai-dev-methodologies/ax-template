#!/usr/bin/env bash
# practices/evals/broadleaf_absorption_parity_guard.sh — Broadleaf-absorption METHODOLOGY
# completeness + VERIFICATION-GOAL-PARITY guard.
#
# THE INVARIANT (binary, NO EXCEPTION): every Broadleaf-absorbed vertical MUST carry a
# parity record in docs/broadleaf-parity/<vertical>.md proving the methodology ran in full
# AND that the absorption captured the same VERIFICATION GOAL as Broadleaf's own tests
# (the test INTENT — never the test code, which is FUL-licensed). Each record MUST declare:
#   - vertical            (name)
#   - broadleaf_source    (the file:line the invariant was mined from)
#   - spec_items          (our spec item id(s) — each MUST resolve in specs/*.yaml)
#   - rule                (our evidence-anchored rule path, or a REVIEW-TIER/COMPOSED marker)
#   - behavioral_test     (our test file path, or a DEFERRED/REVIEW-TIER marker)
#   - violation_proof     (our ViolationProofTest path proving the invariant is by-construction
#                          impossible to violate, or a REVIEW-TIER marker when the vertical has no
#                          backend domain — added after the 2026-06-26 completeness audit found the
#                          payment vertical shipped without one and this guard could not catch it)
#   - adversarial_review  (the opus refute-by-default verdict — mandatory, non-empty)
# AND a "Verification-goal parity" table mapping >=1 Broadleaf test-INTENT scenario to our
# behavioral assertion (so we prove the same verification goal, not the same test code).
#
# This makes the absorption methodology MECHANICALLY ENFORCED: a vertical cannot be declared
# done with a hollow or missing parity record. Referenced artifacts (rule, test, spec items)
# are checked to EXIST on disk — a parity record cannot lie about its artifacts.
#
# Usage:
#   bash practices/evals/broadleaf_absorption_parity_guard.sh
#   bash practices/evals/broadleaf_absorption_parity_guard.sh --root DIR   # fixture mode
# Exit 0 = every parity record is complete + artifacts exist. Exit 1 = a record is incomplete.

set -u

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        *) echo "broadleaf_absorption_parity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

if [ -n "$ROOT_OVERRIDE" ]; then
    [ -d "$ROOT_OVERRIDE" ] || { echo "broadleaf_absorption_parity_guard: root not found: $ROOT_OVERRIDE" >&2; exit 2; }
    PARITY_DIR="$ROOT_OVERRIDE"
else
    PARITY_DIR="$REPO_ROOT/docs/broadleaf-parity"
    [ -d "$PARITY_DIR" ] || { echo "broadleaf_absorption_parity_guard: docs/broadleaf-parity not found (no absorbed verticals registered yet)" >&2; exit 0; }
fi

REQUIRED_FIELDS="vertical broadleaf_source spec_items rule behavioral_test violation_proof adversarial_review"
FAIL=0

field_value() {  # $1=file $2=key  -> prints value after "- <key>:"
    grep -m1 -E "^- ${2}:" "$1" 2>/dev/null | sed -E "s/^- ${2}:[[:space:]]*//"
}

is_marker() {  # value is a non-path marker (REVIEW-TIER / DEFERRED / COMPOSED / n/a)
    echo "$1" | grep -qiE 'REVIEW-TIER|DEFERRED|COMPOSED|^n/a$|^none$'
}

for f in "$PARITY_DIR"/*.md; do
    [ -e "$f" ] || continue
    base="$(basename "$f")"
    case "$base" in REGISTRY.md|README.md) continue ;; esac

    # 1. required fields present + non-empty
    for key in $REQUIRED_FIELDS; do
        val="$(field_value "$f" "$key")"
        if [ -z "$val" ]; then
            echo "broadleaf_absorption_parity_guard: FAIL [$base] — missing/empty required field '- $key:'" >&2
            FAIL=1
        fi
    done

    # 2. >=1 verification-goal parity data row (a table row that is neither header nor separator)
    rows="$(grep -E '^\|' "$f" 2>/dev/null \
        | grep -vE '^\|[[:space:]]*Broadleaf test' \
        | grep -vE '^\|[[:space:]]*-{2,}' \
        | grep -vE '^\|[[:space:]]*:?-+:?[[:space:]]*\|' \
        | wc -l | tr -d ' ')"
    if [ "${rows:-0}" -lt 1 ]; then
        echo "broadleaf_absorption_parity_guard: FAIL [$base] — needs >=1 verification-goal parity row (Broadleaf test intent -> our assertion)" >&2
        FAIL=1
    fi

    # 3. artifact existence (skip markers); paths resolve from REPO_ROOT
    rule_val="$(field_value "$f" rule)"
    if [ -n "$rule_val" ] && ! is_marker "$rule_val"; then
        rule_path="$(echo "$rule_val" | grep -oE 'practices/rules/[^ ]+\.md' | head -1)"
        if [ -n "$rule_path" ] && [ ! -f "$REPO_ROOT/$rule_path" ]; then
            echo "broadleaf_absorption_parity_guard: FAIL [$base] — rule path does not exist: $rule_path" >&2
            FAIL=1
        fi
    fi

    test_val="$(field_value "$f" behavioral_test)"
    if [ -n "$test_val" ] && ! is_marker "$test_val"; then
        test_path="$(echo "$test_val" | grep -oE '(backend|frontend)/[^ ]+\.(java|ts|tsx)' | head -1)"
        if [ -n "$test_path" ] && [ ! -f "$REPO_ROOT/$test_path" ]; then
            echo "broadleaf_absorption_parity_guard: FAIL [$base] — behavioral_test path does not exist: $test_path" >&2
            FAIL=1
        fi
    fi

    vproof_val="$(field_value "$f" violation_proof)"
    if [ -n "$vproof_val" ] && ! is_marker "$vproof_val"; then
        vproof_path="$(echo "$vproof_val" | grep -oE '(backend|frontend)/[^ ]+\.(java|ts|tsx)' | head -1)"
        if [ -n "$vproof_path" ] && [ ! -f "$REPO_ROOT/$vproof_path" ]; then
            echo "broadleaf_absorption_parity_guard: FAIL [$base] — violation_proof path does not exist: $vproof_path" >&2
            FAIL=1
        fi
    fi

    spec_val="$(field_value "$f" spec_items)"
    if [ -n "$spec_val" ]; then
        for id in $(echo "$spec_val" | tr ',' ' '); do
            id="$(echo "$id" | tr -d ' ')"
            [ -z "$id" ] && continue
            if ! grep -rqE "id:[[:space:]]*\"?${id}\"?" "$REPO_ROOT/specs/" 2>/dev/null; then
                echo "broadleaf_absorption_parity_guard: FAIL [$base] — spec item not found in specs/: $id" >&2
                FAIL=1
            fi
        done
    fi
done

if [ "$FAIL" -ne 0 ]; then
    echo "" >&2
    echo "  Every Broadleaf-absorbed vertical MUST have a COMPLETE parity record (methodology + verification-goal" >&2
    echo "  parity). Fill the missing fields / parity rows / artifacts in docs/broadleaf-parity/<vertical>.md." >&2
    exit 1
fi

echo "broadleaf_absorption_parity_guard: PASS — all parity records complete + artifacts exist ($PARITY_DIR)"
exit 0
