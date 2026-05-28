#!/usr/bin/env bash
# practices/evals/wave_kickoff_ledger_guard.sh — R97 48th hard guard.
#
# Mechanical closure of the P12 F10 cycle (R93/R94/R95/R96 = 4 consecutive
# occurrences of "wave kickoff committed without a matching dogfood ledger").
# 4-data-point empirical evidence (per persona-registry.yaml R96 review_summary
# .cross_wave_observations.p12_f10_4_data_point_escalation) established that
# lesson-as-text + procedure-as-text are functionally identical and BOTH are
# self-learning insufficient. This guard is the (a) binary-decision outcome
# from R97 mandatory_iter1_agenda.
#
# Logic:
#   1. Scan specs/*-l0.yaml for `introduced_at:` line.
#   2. Extract every "R<N> phase [αβγ]" mention — each marks a wave kickoff.
#   3. Collect unique R-numbers across all specs.
#   4. For each R-number, verify docs/dogfood-ledger/r<N>-iter1.yaml exists.
#   5. Missing iter1 ledger for a phase-tagged spec → FAIL.
#
# Effect: any future spec yaml whose introduced_at carries "R<N> phase α/β/γ"
# MUST land in a commit (or same-wave commit chain) that also includes
# docs/dogfood-ledger/r<N>-iter1.yaml. The pre-commit guard sequence (this
# script is invoked by run-all-guards.sh which is run by both verify-
# completion and the [practices] all-hard-gates pre-commit hook) will refuse
# the commit otherwise.
#
# Exit codes: 0 PASS · 1 violation · 2 usage error.
#
# Usage:
#   bash practices/evals/wave_kickoff_ledger_guard.sh
#   bash practices/evals/wave_kickoff_ledger_guard.sh --root <repo_root>

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        *) echo "wave_kickoff_ledger_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SPECS_DIR="$REPO_ROOT/specs"
LEDGER_DIR="$REPO_ROOT/docs/dogfood-ledger"

if [ ! -d "$SPECS_DIR" ]; then
    echo "wave_kickoff_ledger_guard: specs/ not found at $SPECS_DIR" >&2
    exit 2
fi

if [ ! -d "$LEDGER_DIR" ]; then
    echo "wave_kickoff_ledger_guard: docs/dogfood-ledger/ not found at $LEDGER_DIR" >&2
    exit 2
fi

violations=0
phase_tagged_specs=0
covered_r_numbers=""

for f in "$SPECS_DIR"/*-l0.yaml; do
    [ -f "$f" ] || continue

    # Extract "R<N> phase [αβγ]" from anywhere in the file (introduced_at line
    # canonically, but tolerate the marker in body comments too).
    matches=$(grep -oE 'R[0-9]+ phase [αβγ]' "$f" 2>/dev/null | sort -u || true)
    [ -z "$matches" ] && continue

    phase_tagged_specs=$((phase_tagged_specs + 1))

    while IFS= read -r match; do
        r_num=$(echo "$match" | grep -oE '[0-9]+')
        [ -z "$r_num" ] && continue

        ledger="$LEDGER_DIR/r${r_num}-iter1.yaml"
        spec_rel="${f#$REPO_ROOT/}"

        if [ ! -f "$ledger" ]; then
            echo "VIOLATION: $spec_rel cites '$match' but $LEDGER_DIR/r${r_num}-iter1.yaml does not exist" >&2
            violations=$((violations + 1))
        else
            # Track for summary line. Use a delimited string for portable uniqueness.
            case " $covered_r_numbers " in
                *" R$r_num "*) ;;
                *) covered_r_numbers="$covered_r_numbers R$r_num" ;;
            esac
        fi
    done <<< "$matches"
done

# Trim leading space.
covered_r_numbers="${covered_r_numbers# }"

if [ "$violations" -gt 0 ]; then
    echo "wave_kickoff_ledger_guard: FAIL — $violations spec(s) cite 'R<N> phase α/β/γ' but lack the matching r<N>-iter1.yaml ledger" >&2
    echo "wave_kickoff_ledger_guard: P12 F10 4-data-point evidence demanded this mechanical anchor; the lesson now refuses to silently drift." >&2
    exit 1
fi

if [ "$phase_tagged_specs" -eq 0 ]; then
    echo "wave_kickoff_ledger_guard: PASS — no specs/*-l0.yaml carry 'R<N> phase α/β/γ' yet (no kickoffs to enforce)"
    exit 0
fi

echo "wave_kickoff_ledger_guard: PASS — $phase_tagged_specs phase-tagged spec(s), covered waves: $covered_r_numbers"
exit 0
