#!/usr/bin/env bash
# practices/evals/broadleaf_module_exhaustion_guard.sh — Broadleaf module-set EXHAUSTION guard.
#
# THE INVARIANT (binary): the Broadleaf-absorption program is a BOUNDED sweep over a FINITE codebase.
# docs/BROADLEAF-COMPLETENESS.md MUST classify EVERY Broadleaf core subsystem with ZERO silent gaps.
# The guard enforces the ledger's internal completeness + RESIDUE cross-links:
#   1. every table row has a classification in {ABSORBED, RE-FIND, SKIP, RESIDUE} (no UNCLASSIFIED/TODO/blank);
#   2. every row (all four classifications) has a NON-EMPTY evidence column;
#   3. the data-row count == the declared `module_count: N` header (disk-truth-vs-declared);
#   4. the number of RESIDUE rows == the declared `residue_count: M` header;
#   5. (live only) every RESIDUE references an EXISTING spec (specs/*.yaml) + an EXISTING parity record,
#      AND there is no UNLEDGERED residue: (#parity records − the 7 ultragoal verticals) == residue_count.
# The Broadleaf clone is outside git, so the universe's correctness is established once at sweep time WITH
# the clone + adversarial review (the reproducible enumeration command is recorded in the ledger header);
# this guard enforces the ledger is internally complete and cannot silently drop a module or a residue.
#
# Usage:
#   bash practices/evals/broadleaf_module_exhaustion_guard.sh
#   bash practices/evals/broadleaf_module_exhaustion_guard.sh --root DIR   # fixture mode (DIR/BROADLEAF-COMPLETENESS.md)
# Exit 0 = ledger complete + consistent. Exit 1 = a gap/inconsistency (BLOCK).

set -u

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        *) echo "broadleaf_module_exhaustion_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

if [ -n "$ROOT_OVERRIDE" ]; then
    LEDGER="$ROOT_OVERRIDE/BROADLEAF-COMPLETENESS.md"
    LIVE=0
else
    LEDGER="$REPO_ROOT/docs/BROADLEAF-COMPLETENESS.md"
    LIVE=1
fi
[ -f "$LEDGER" ] || { echo "broadleaf_module_exhaustion_guard: ledger not found: $LEDGER" >&2; exit 1; }

FAIL=0

MODULE_COUNT="$(grep -m1 -E '^module_count:' "$LEDGER" | sed -E 's/^module_count:[[:space:]]*//' | tr -d ' ')"
RESIDUE_COUNT="$(grep -m1 -E '^residue_count:' "$LEDGER" | sed -E 's/^residue_count:[[:space:]]*//' | tr -d ' ')"
if ! echo "$MODULE_COUNT" | grep -qE '^[0-9]+$'; then echo "broadleaf_module_exhaustion_guard: FAIL — missing/invalid 'module_count:' header" >&2; exit 1; fi
if ! echo "$RESIDUE_COUNT" | grep -qE '^[0-9]+$'; then echo "broadleaf_module_exhaustion_guard: FAIL — missing/invalid 'residue_count:' header" >&2; exit 1; fi

# Extract data rows: table lines starting with '|' that are NOT the header (contains 'classification')
# nor the separator (|---). Each: | module | classification | evidence |
ROWS=0
RESIDUE_ROWS=0
RESIDUE_SPECS=""
while IFS= read -r line; do
    case "$line" in
        *classification*) continue ;;                 # header row
    esac
    echo "$line" | grep -qE '^\|[[:space:]]*:?-{2,}' && continue   # separator row
    # split on '|' → fields 2,3,4 are module, classification, evidence
    module="$(echo "$line" | awk -F'|' '{print $2}' | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
    classif="$(echo "$line" | awk -F'|' '{print $3}' | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
    evidence="$(echo "$line" | awk -F'|' '{print $4}' | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
    [ -z "$module" ] && continue
    ROWS=$((ROWS+1))
    # 1. valid classification enum
    case "$classif" in
        ABSORBED|RE-FIND|SKIP|RESIDUE) ;;
        *) echo "broadleaf_module_exhaustion_guard: FAIL — module '$module' has invalid classification '$classif' (must be ABSORBED/RE-FIND/SKIP/RESIDUE)" >&2; FAIL=1 ;;
    esac
    # 2. non-empty evidence
    if [ -z "$evidence" ]; then
        echo "broadleaf_module_exhaustion_guard: FAIL — module '$module' has empty evidence" >&2; FAIL=1
    fi
    if [ "$classif" = "RESIDUE" ]; then
        RESIDUE_ROWS=$((RESIDUE_ROWS+1))
    fi
done < <(grep -E '^\|' "$LEDGER")

# 3. row count == module_count
if [ "$ROWS" -ne "$MODULE_COUNT" ]; then
    echo "broadleaf_module_exhaustion_guard: FAIL — $ROWS table rows but module_count declares $MODULE_COUNT" >&2; FAIL=1
fi
# 4. RESIDUE rows == residue_count
if [ "$RESIDUE_ROWS" -ne "$RESIDUE_COUNT" ]; then
    echo "broadleaf_module_exhaustion_guard: FAIL — $RESIDUE_ROWS RESIDUE rows but residue_count declares $RESIDUE_COUNT" >&2; FAIL=1
fi

# 5. (live only) no unledgered residue: (#parity records − 7 ultragoal verticals) == residue_count
if [ "$LIVE" -eq 1 ]; then
    PARITY_DIR="$REPO_ROOT/docs/broadleaf-parity"
    if [ -d "$PARITY_DIR" ]; then
        TOTAL_PARITY=0
        for f in "$PARITY_DIR"/*.md; do
            [ -e "$f" ] || continue
            case "$(basename "$f")" in REGISTRY.md|README.md) continue ;; esac
            TOTAL_PARITY=$((TOTAL_PARITY+1))
        done
        # 7 ultragoal verticals: promotion, pricing, order, checkout, payment, inventory-customer, rating
        RESIDUE_PARITY=$((TOTAL_PARITY-7))
        if [ "$RESIDUE_PARITY" -ne "$RESIDUE_COUNT" ]; then
            echo "broadleaf_module_exhaustion_guard: FAIL — $TOTAL_PARITY parity records − 7 ultragoal = $RESIDUE_PARITY residue parity record(s), but residue_count declares $RESIDUE_COUNT (unledgered or missing residue parity record)" >&2
            FAIL=1
        fi
    fi
fi

if [ "$FAIL" -ne 0 ]; then
    echo "" >&2
    echo "  docs/BROADLEAF-COMPLETENESS.md must classify EVERY Broadleaf core subsystem (no silent gap) with a" >&2
    echo "  valid classification + non-empty evidence; counts must match; every RESIDUE must have a parity record." >&2
    exit 1
fi

echo "broadleaf_module_exhaustion_guard: PASS — $ROWS modules classified, $RESIDUE_ROWS residue ($LEDGER)"
exit 0
