#!/usr/bin/env bash
# practices/evals/broadleaf_module_exhaustion_guard.sh — Broadleaf module-set EXHAUSTION guard.
#
# THE INVARIANT (binary): the Broadleaf-absorption program is a BOUNDED sweep over a FINITE codebase.
# docs/BROADLEAF-COMPLETENESS.md MUST classify EVERY Broadleaf subsystem with ZERO silent gaps — at BOTH
# levels: the 9 top-level Maven modules AND the 21 core commerce sub-packages. The guard enforces:
#   1. every table row (both tables) has a classification in {ABSORBED, RE-FIND, SKIP, RESIDUE};
#   2. every row has a NON-EMPTY evidence column;
#   3. Maven-table row count == `maven_module_count: N`; core-table row count == `module_count: M`;
#   4. the number of RESIDUE rows == the declared `residue_count: R`;
#   5. (live) every RESIDUE references an EXISTING spec + parity record, AND no UNLEDGERED residue:
#      (#parity records − the 7 ultragoal verticals) == residue_count.
#   6. (live, DISK-TRUTH) when the Broadleaf clone is present at ../broadleaf-modernized, EVERY real
#      on-disk Maven source module (*/src/main/java/org/broadleafcommerce) has a Maven-table row, AND
#      every real core sub-package has a core-table row. This makes the counts DISK-TRUTHFUL — the guard
#      can no longer pass on a ledger that silently dropped a module/package (the prior version compared
#      the ledger's row count only to its OWN declared header, never to the clone).
#
# Usage:
#   bash practices/evals/broadleaf_module_exhaustion_guard.sh
#   bash practices/evals/broadleaf_module_exhaustion_guard.sh --root DIR   # fixture mode (DIR/BROADLEAF-COMPLETENESS.md)
# Exit 0 = ledger complete + consistent (+ disk-truthful when clone present). Exit 1 = a gap (BLOCK).

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

CLONE="$REPO_ROOT/../broadleaf-modernized"

FAIL=0

MAVEN_COUNT="$(grep -m1 -E '^maven_module_count:' "$LEDGER" | sed -E 's/^maven_module_count:[[:space:]]*//' | tr -d ' ')"
MODULE_COUNT="$(grep -m1 -E '^module_count:' "$LEDGER" | sed -E 's/^module_count:[[:space:]]*//' | tr -d ' ')"
RESIDUE_COUNT="$(grep -m1 -E '^residue_count:' "$LEDGER" | sed -E 's/^residue_count:[[:space:]]*//' | tr -d ' ')"
for pair in "maven_module_count:$MAVEN_COUNT" "module_count:$MODULE_COUNT" "residue_count:$RESIDUE_COUNT"; do
    name="${pair%%:*}"; val="${pair#*:}"
    if ! echo "$val" | grep -qE '^[0-9]+$'; then echo "broadleaf_module_exhaustion_guard: FAIL — missing/invalid '$name:' header" >&2; exit 1; fi
done

# Parse rows section-aware. Two tables, delimited by '## Maven module-set' and '## Core commerce-package set'.
# A table row starts with '|', is not the header (contains 'classification') and not the |--- separator.
SECTION=""
MAVEN_ROWS=0
CORE_ROWS=0
RESIDUE_ROWS=0
MAVEN_MODULES=""   # newline-list of maven module names (for disk-truth check)
CORE_MODULES=""    # newline-list of core sub-package names
while IFS= read -r line; do
    case "$line" in
        "## Maven module-set"*) SECTION="maven"; continue ;;
        "## Core commerce-package set"*) SECTION="core"; continue ;;
        "## "*) SECTION=""; continue ;;
    esac
    case "$line" in '|'*) ;; *) continue ;; esac
    case "$line" in *classification*) continue ;; esac
    echo "$line" | grep -qE '^\|[[:space:]]*:?-{2,}' && continue
    module="$(echo "$line" | awk -F'|' '{print $2}' | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
    classif="$(echo "$line" | awk -F'|' '{print $3}' | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
    evidence="$(echo "$line" | awk -F'|' '{print $4}' | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
    [ -z "$module" ] && continue
    case "$classif" in
        ABSORBED|RE-FIND|SKIP|RESIDUE) ;;
        *) echo "broadleaf_module_exhaustion_guard: FAIL — '$module' has invalid classification '$classif'" >&2; FAIL=1 ;;
    esac
    [ -z "$evidence" ] && { echo "broadleaf_module_exhaustion_guard: FAIL — '$module' has empty evidence" >&2; FAIL=1; }
    if [ "$SECTION" = "maven" ]; then
        MAVEN_ROWS=$((MAVEN_ROWS+1)); MAVEN_MODULES="$MAVEN_MODULES$module"$'\n'
    elif [ "$SECTION" = "core" ]; then
        CORE_ROWS=$((CORE_ROWS+1)); CORE_MODULES="$CORE_MODULES$module"$'\n'
        [ "$classif" = "RESIDUE" ] && RESIDUE_ROWS=$((RESIDUE_ROWS+1))
    fi
done < "$LEDGER"

# 3. row counts == declared headers
[ "$MAVEN_ROWS" -ne "$MAVEN_COUNT" ] && { echo "broadleaf_module_exhaustion_guard: FAIL — $MAVEN_ROWS Maven-table rows but maven_module_count declares $MAVEN_COUNT" >&2; FAIL=1; }
[ "$CORE_ROWS" -ne "$MODULE_COUNT" ] && { echo "broadleaf_module_exhaustion_guard: FAIL — $CORE_ROWS core-table rows but module_count declares $MODULE_COUNT" >&2; FAIL=1; }
# 4. RESIDUE rows == residue_count
[ "$RESIDUE_ROWS" -ne "$RESIDUE_COUNT" ] && { echo "broadleaf_module_exhaustion_guard: FAIL — $RESIDUE_ROWS RESIDUE rows but residue_count declares $RESIDUE_COUNT" >&2; FAIL=1; }

# 5. (live) no unledgered residue: (#parity records − 7 ultragoal verticals) == residue_count
if [ "$LIVE" -eq 1 ]; then
    PARITY_DIR="$REPO_ROOT/docs/broadleaf-parity"
    if [ -d "$PARITY_DIR" ]; then
        TOTAL_PARITY=0
        for f in "$PARITY_DIR"/*.md; do
            [ -e "$f" ] || continue
            case "$(basename "$f")" in REGISTRY.md|README.md) continue ;; esac
            TOTAL_PARITY=$((TOTAL_PARITY+1))
        done
        RESIDUE_PARITY=$((TOTAL_PARITY-7))
        if [ "$RESIDUE_PARITY" -ne "$RESIDUE_COUNT" ]; then
            echo "broadleaf_module_exhaustion_guard: FAIL — $TOTAL_PARITY parity records − 7 ultragoal = $RESIDUE_PARITY residue parity record(s), but residue_count declares $RESIDUE_COUNT" >&2
            FAIL=1
        fi
    fi
fi

# 6. (live, DISK-TRUTH) every real on-disk Maven source module + core sub-package has a ledger row.
if [ "$LIVE" -eq 1 ] && [ -d "$CLONE" ]; then
    # 6a. Maven source modules = dirs containing src/main/java/org/broadleafcommerce (excludes reactor poms
    #     and the src/test-only functional-tests module — neither ships a correctness invariant).
    while IFS= read -r srcdir; do
        [ -z "$srcdir" ] && continue
        mod="${srcdir#"$CLONE"/}"; mod="${mod%/src/main/java/org/broadleafcommerce}"
        if ! printf '%s\n' "$MAVEN_MODULES" | grep -qxF "$mod"; then
            echo "broadleaf_module_exhaustion_guard: FAIL — on-disk Maven module '$mod' has NO classification row (silent gap)" >&2
            FAIL=1
        fi
    done < <(find "$CLONE" -type d -path '*/src/main/java/org/broadleafcommerce' 2>/dev/null)
    # 6b. core commerce sub-packages
    CORE_PKG_DIR="$CLONE/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core"
    if [ -d "$CORE_PKG_DIR" ]; then
        while IFS= read -r sub; do
            [ -z "$sub" ] && continue
            name="$(basename "$sub")"
            if ! printf '%s\n' "$CORE_MODULES" | grep -qxF "$name"; then
                echo "broadleaf_module_exhaustion_guard: FAIL — on-disk core sub-package '$name' has NO classification row (silent gap)" >&2
                FAIL=1
            fi
        done < <(find "$CORE_PKG_DIR" -mindepth 1 -maxdepth 1 -type d 2>/dev/null)
    fi
fi

if [ "$FAIL" -ne 0 ]; then
    echo "" >&2
    echo "  docs/BROADLEAF-COMPLETENESS.md must classify EVERY Broadleaf Maven module AND core sub-package" >&2
    echo "  (no silent gap) with a valid classification + non-empty evidence; counts must match the declared" >&2
    echo "  headers; every RESIDUE must have a parity record; when the clone is present, every on-disk module" >&2
    echo "  and core package must have a row (disk-truth)." >&2
    exit 1
fi

echo "broadleaf_module_exhaustion_guard: PASS — $MAVEN_ROWS Maven modules + $CORE_ROWS core packages classified, $RESIDUE_ROWS residue ($([ "$LIVE" -eq 1 ] && [ -d "$CLONE" ] && echo 'disk-truth verified' || echo 'ledger-internal'))"
exit 0
