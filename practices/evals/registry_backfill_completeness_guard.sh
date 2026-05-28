#!/usr/bin/env bash
# practices/evals/registry_backfill_completeness_guard.sh — R98 49th hard guard.
#
# Mechanical closure of the R97 P12 F10 NEW finding (commit-5 registry
# omission). Per docs/dogfood-ledger/r97-iter2.yaml + persona-registry.yaml
# review_summary.R97.cross_wave_observations.r97_p12_new_gap_signal:
#
#   "wave_kickoff_ledger_guard covers phase α atomic (commits 1-2 boundary).
#    iter ledger guard covers commit-3 ledger schema. iter2 terminator has
#    no specific guard. persona-registry.yaml backfill (commit 5) has no
#    completeness guard — fork-receiver could land commits 1-4 of a wave
#    and skip commit 5, leaving review_summary.R<N> + 10 persona history
#    + next_panel_composition entries stale. Same mechanical-anchor pattern
#    needed; R98 mandatory_iter1_agenda becomes registry_backfill_completeness
#    _guard.sh implementation."
#
# This guard is the R98 mandatory_iter1_agenda item #1 active closure.
#
# Logic:
#   1. Scan docs/dogfood-ledger/r<N>-iter<M>.yaml files.
#   2. For each unique R<N>, find the highest iter file.
#   3. If the highest iter file has `findings: []` AND iter >= 2, the wave
#      is "terminated" (TRUE 0-convergence — R94/R95/R96/R97 pattern) and
#      requires registry backfill.
#   4. For each terminated R<N>:
#        a. Verify `^  R<N>:` exists under persona-registry.yaml
#           review_summary section.
#        b. Verify 10 personas (P3-P12) each carry a `- wave: R<N>` history
#           entry — count occurrences; must equal 10.
#   5. Missing review_summary.R<N> OR persona count != 10 → FAIL.
#
# Effect: any future wave that lands iter terminator (commit 4 in 5-commit
# precedent) MUST follow with persona-registry backfill (commit 5) in the
# same wave commit chain. Pre-push hook (via run-all-guards.sh) refuses the
# push if backfill is incomplete.
#
# Exit codes: 0 PASS · 1 violation · 2 usage error.
#
# Usage:
#   bash practices/evals/registry_backfill_completeness_guard.sh
#   bash practices/evals/registry_backfill_completeness_guard.sh --root <repo_root>

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        *) echo "registry_backfill_completeness_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

LEDGER_DIR="$REPO_ROOT/docs/dogfood-ledger"
REGISTRY="$LEDGER_DIR/persona-registry.yaml"

if [ ! -d "$LEDGER_DIR" ]; then
    echo "registry_backfill_completeness_guard: docs/dogfood-ledger/ not found at $LEDGER_DIR" >&2
    exit 2
fi

if [ ! -f "$REGISTRY" ]; then
    echo "registry_backfill_completeness_guard: persona-registry.yaml not found at $REGISTRY" >&2
    exit 2
fi

# Pass 1: extract unique R-numbers across all r<N>-iter<M>.yaml files.
r_nums=""
for f in "$LEDGER_DIR"/r*-iter*.yaml; do
    [ -f "$f" ] || continue
    base=$(basename "$f")
    # Strict pattern: r<N>-iter<M>.yaml (numeric N and M only).
    case "$base" in
        r[0-9]*-iter[0-9]*.yaml) ;;
        *) continue ;;
    esac
    r_num=$(echo "$base" | sed -nE 's/^r([0-9]+)-iter[0-9]+\.yaml$/\1/p')
    [ -z "$r_num" ] && continue
    case " $r_nums " in
        *" $r_num "*) ;;
        *) r_nums="$r_nums $r_num" ;;
    esac
done
r_nums="${r_nums# }"

violations=0
terminated_waves=""
considered_waves=""

# Pass 2: for each R<N>, find highest iter; if terminator, validate backfill.
for r_num in $r_nums; do
    # Find highest iter for this wave.
    highest_iter=0
    for f in "$LEDGER_DIR"/r${r_num}-iter*.yaml; do
        [ -f "$f" ] || continue
        base=$(basename "$f")
        iter=$(echo "$base" | sed -nE 's/^r[0-9]+-iter([0-9]+)\.yaml$/\1/p')
        [ -z "$iter" ] && continue
        if [ "$iter" -gt "$highest_iter" ]; then
            highest_iter="$iter"
        fi
    done

    # iter >= 2 + findings: [] required for "terminator".
    if [ "$highest_iter" -lt 2 ]; then
        considered_waves="$considered_waves R${r_num}(iter${highest_iter}-in-progress)"
        continue
    fi

    terminator_file="$LEDGER_DIR/r${r_num}-iter${highest_iter}.yaml"
    if ! grep -qE '^findings:[[:space:]]*\[\][[:space:]]*$' "$terminator_file"; then
        considered_waves="$considered_waves R${r_num}(iter${highest_iter}-not-terminated)"
        continue
    fi

    terminated_waves="$terminated_waves R${r_num}"

    # Check (a): review_summary.R<N> entry exists.
    if ! grep -qE "^  R${r_num}:[[:space:]]*$" "$REGISTRY"; then
        echo "VIOLATION: R${r_num} wave terminated (iter${highest_iter} findings: []) but persona-registry.yaml lacks 'review_summary.R${r_num}' entry" >&2
        violations=$((violations + 1))
        continue
    fi

    # Check (b): each of 10 personas (P3-P12) carries at least 1 `- wave: R<N>`
    # history entry. R93 had 5 iters per persona so some personas accumulate
    # multiple R93 entries — count must be >= 10 distinct personas covering it.
    personas_with_wave=$(awk -v r="$r_num" '
/^  - id: / { persona=$3 }
/^      - wave: / { wave=$3; if (wave == "R"r) seen[persona]=1 }
END {
    n=0
    for (p in seen) {
        if (p == "P3" || p == "P4" || p == "P5" || p == "P6" || p == "P7" || \
            p == "P8" || p == "P9" || p == "P10" || p == "P11" || p == "P12") n++
    }
    print n
}' "$REGISTRY")
    [ -z "$personas_with_wave" ] && personas_with_wave=0
    if [ "$personas_with_wave" -ne 10 ]; then
        echo "VIOLATION: R${r_num} wave terminated but only ${personas_with_wave}/10 of P3-P12 personas carry a 'wave: R${r_num}' history entry (expected exactly 10 distinct personas)" >&2
        violations=$((violations + 1))
    fi
done

terminated_waves="${terminated_waves# }"
considered_waves="${considered_waves# }"

if [ "$violations" -gt 0 ]; then
    echo "registry_backfill_completeness_guard: FAIL — $violations terminated wave(s) lack complete persona-registry backfill" >&2
    echo "registry_backfill_completeness_guard: R97 P12 F10 NEW finding mechanical anchor — every terminated wave (iter>=2 with findings: []) MUST land review_summary.R<N> + 10 persona history entries in the same wave commit chain (typically commit 5)." >&2
    exit 1
fi

if [ -z "$terminated_waves" ]; then
    echo "registry_backfill_completeness_guard: PASS — no terminated waves yet (no backfill to enforce). Considered: ${considered_waves:-none}"
    exit 0
fi

echo "registry_backfill_completeness_guard: PASS — terminated waves with complete backfill: $terminated_waves"
exit 0
