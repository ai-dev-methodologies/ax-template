#!/usr/bin/env bash
# practices/evals/dogfood_ledger_guard.sh — R37 39th hard guard (ralplan codex P0-3).
#
# Closes the "dogfood theater" drift identified in the R37 integrated review:
# every dogfood iteration must classify each persona finding as
# {real_bug | scope_deferral | methodology_gap}; two consecutive rounds resolving
# to ALL scope_deferral for the same domain fails the guard.
#
# Source of truth: docs/dogfood-ledger/<domain>-iter<N>.yaml — NOT commit message
# parsing (the latter is gameable per codex anti-gaming change #4).
#
# Schema each ledger entry MUST contain:
#   - iteration: N
#   - findings: [{ persona, finding, classification, references_artifact_path }]
#
# Fixtures cover: pass_real_bug, pass_methodology_gap, pass_mixed,
#   fail_two_consecutive_all_deferral, fail_omitted_findings, fail_renamed_categories.
#
# Usage:
#   bash practices/evals/dogfood_ledger_guard.sh                # live repo
#   bash practices/evals/dogfood_ledger_guard.sh --fixtures
#   bash practices/evals/dogfood_ledger_guard.sh --root DIR     # for fixture sub-runs
#
# Exit codes: 0 PASS · 1 violation · 2 usage error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "dogfood_ledger_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/dogfood_ledger"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "dogfood_ledger_guard: fixtures dir missing: $FIXTURES_DIR" >&2
        exit 2
    fi
    pass=0; fail=0
    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [dogfood_ledger/$(basename "$sub")]"; pass=$((pass+1))
        else
            echo "FAIL [dogfood_ledger/$(basename "$sub")] (expected pass)"; fail=$((fail+1))
        fi
    done
    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "FAIL [dogfood_ledger/$(basename "$sub")] (expected fail but passed)"; fail=$((fail+1))
        else
            echo "PASS [dogfood_ledger/$(basename "$sub")]"; pass=$((pass+1))
        fi
    done
    if [ "$fail" -ne 0 ]; then
        echo "dogfood_ledger_guard: fixtures FAIL ($fail / $((pass+fail)))" >&2
        exit 1
    fi
    echo "dogfood_ledger_guard: fixtures PASS ($pass)"
    exit 0
fi

# ── Live mode ────────────────────────────────────────────────────────────────
ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
LEDGER_DIR="$ROOT/docs/dogfood-ledger"

# Empty ledger dir is fine (no dogfood done yet → cannot violate).
if [ ! -d "$LEDGER_DIR" ]; then
    exit 0
fi

# Group ledger files by domain. Detect 2-consecutive-all-deferral runs.
ALLOWED_CLASSIFICATIONS='^(real_bug|scope_deferral|methodology_gap)$'

# Bash 3.2 compatibility — no associative arrays. Group by domain via two-pass.
violations=0

# Pass 1: validate each file's schema + classification values.
for f in $(find "$LEDGER_DIR" -maxdepth 1 -name "*-iter*.yaml" 2>/dev/null | sort); do
    if ! grep -q '^findings:' "$f"; then
        echo "VIOLATION: $f missing top-level 'findings:' block" >&2
        violations=$((violations+1))
        continue
    fi

    finding_lines=$(grep -c '^  - persona:' "$f" 2>/dev/null || true)
    [ -z "$finding_lines" ] && finding_lines=0
    classification_lines=$(grep -c '^    classification:' "$f" 2>/dev/null || true)
    [ -z "$classification_lines" ] && classification_lines=0

    if [ "$finding_lines" -ne "$classification_lines" ]; then
        echo "VIOLATION: $f finding count ($finding_lines) != classification count ($classification_lines)" >&2
        violations=$((violations+1))
        continue
    fi

    bad=$(grep '^    classification:' "$f" 2>/dev/null | awk '{print $2}' | grep -Ev "$ALLOWED_CLASSIFICATIONS" || true)
    if [ -n "$bad" ]; then
        echo "VIOLATION: $f has classification(s) outside allowed set: $bad" >&2
        violations=$((violations+1))
        continue
    fi
done

# Pass 2: per-domain 2-consecutive-all-deferral detection.
DOMAINS=$(find "$LEDGER_DIR" -maxdepth 1 -name "*-iter*.yaml" 2>/dev/null | sed -E 's|.*/||; s|-iter[0-9]+\.yaml$||' | sort -u)
for domain in $DOMAINS; do
    prev_all_deferral=0
    for f in $(find "$LEDGER_DIR" -maxdepth 1 -name "${domain}-iter*.yaml" 2>/dev/null | sort -V); do
        finding_lines=$(grep -c '^  - persona:' "$f" 2>/dev/null || true)
        [ -z "$finding_lines" ] && finding_lines=0
        non_deferral=$(grep '^    classification:' "$f" 2>/dev/null | awk '{print $2}' | grep -v 'scope_deferral' | wc -l | tr -d ' ')
        if [ "$non_deferral" -eq 0 ] && [ "$finding_lines" -gt 0 ]; then
            if [ "$prev_all_deferral" -eq 1 ]; then
                echo "VIOLATION: $domain has 2 consecutive iterations resolving to all scope_deferral (latest: $f)" >&2
                violations=$((violations+1))
            fi
            prev_all_deferral=1
        else
            prev_all_deferral=0
        fi
    done
done

if [ "$violations" -ne 0 ]; then
    echo "dogfood_ledger_guard: FAIL with $violations violations" >&2
    exit 1
fi

echo "dogfood_ledger_guard: PASS"
exit 0
