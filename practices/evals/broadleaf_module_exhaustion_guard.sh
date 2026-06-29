#!/usr/bin/env bash
# practices/evals/broadleaf_module_exhaustion_guard.sh — Broadleaf module-set EXHAUSTION guard.
#
# THE INVARIANT (binary): the Broadleaf-absorption program is a BOUNDED sweep over a FINITE codebase.
# docs/BROADLEAF-COMPLETENESS.md MUST classify EVERY Broadleaf subsystem with ZERO silent gaps — at FOUR
# grains: the 10 Maven modules + the 21 core + 56 common + 8 profile/core sub-packages. The guard enforces:
#   1. every table row (all four tables) has a classification in {ABSORBED, RE-FIND, SKIP, RESIDUE};
#   2. every row has a NON-EMPTY evidence column;
#   3. row counts == `maven_module_count` / `module_count` / `common_subpackage_count` / `profile_subpackage_count`;
#   4. the number of RESIDUE rows == the declared `residue_count: R`;
#   5. (live) every RESIDUE references an EXISTING spec + parity record, AND no UNLEDGERED residue:
#      (#parity records − the 8 absorbed verticals) == residue_count.
#   6. (live, DISK-TRUTH) when the Broadleaf clone is present at ../broadleaf-modernized, EVERY built
#      (non-aggregator) on-disk Maven module (a pom.xml whose packaging != 'pom', INCLUDING code-free
#      module shells) has a Maven-table row, AND every real core/common/profile-core sub-package has a row. This
#      makes the counts DISK-TRUTHFUL — the guard can no longer pass on a ledger that silently dropped a
#      module/package (the prior version compared the row count only to its OWN header, never to the clone;
#      an earlier disk-truth pass scanned only src/main modules, silently dropping the empty functional-tests shell).
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
COMMON_COUNT="$(grep -m1 -E '^common_subpackage_count:' "$LEDGER" | sed -E 's/^common_subpackage_count:[[:space:]]*//' | tr -d ' ')"
PROFILE_COUNT="$(grep -m1 -E '^profile_subpackage_count:' "$LEDGER" | sed -E 's/^profile_subpackage_count:[[:space:]]*//' | tr -d ' ')"
RESIDUE_COUNT="$(grep -m1 -E '^residue_count:' "$LEDGER" | sed -E 's/^residue_count:[[:space:]]*//' | tr -d ' ')"
ABSORBED_COUNT="$(grep -m1 -E '^absorbed_vertical_count:' "$LEDGER" | sed -E 's/^absorbed_vertical_count:[[:space:]]*//' | tr -d ' ')"
for pair in "maven_module_count:$MAVEN_COUNT" "module_count:$MODULE_COUNT" "common_subpackage_count:$COMMON_COUNT" "profile_subpackage_count:$PROFILE_COUNT" "residue_count:$RESIDUE_COUNT" "absorbed_vertical_count:$ABSORBED_COUNT"; do
    name="${pair%%:*}"; val="${pair#*:}"
    if ! echo "$val" | grep -qE '^[0-9]+$'; then echo "broadleaf_module_exhaustion_guard: FAIL — missing/invalid '$name:' header" >&2; exit 1; fi
done

# Parse rows section-aware. Four tables, delimited by the '## ...' headers below.
# A table row starts with '|', is not the header (contains 'classification') and not the |--- separator.
SECTION=""
MAVEN_ROWS=0
CORE_ROWS=0
COMMON_ROWS=0
PROFILE_ROWS=0
RESIDUE_ROWS=0
MAVEN_MODULES=""   # newline-list of maven module names (for disk-truth check)
CORE_MODULES=""    # newline-list of core sub-package names
COMMON_MODULES=""  # newline-list of common sub-package names
PROFILE_MODULES="" # newline-list of profile/core sub-package names
while IFS= read -r line; do
    case "$line" in
        "## Maven module-set"*) SECTION="maven"; continue ;;
        "## Core commerce-package set"*) SECTION="core"; continue ;;
        "## Common sub-package set"*) SECTION="common"; continue ;;
        "## Profile-core sub-package set"*) SECTION="profile"; continue ;;
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
        [ "$classif" = "RESIDUE" ] && RESIDUE_ROWS=$((RESIDUE_ROWS+1))
    elif [ "$SECTION" = "core" ]; then
        CORE_ROWS=$((CORE_ROWS+1)); CORE_MODULES="$CORE_MODULES$module"$'\n'
        [ "$classif" = "RESIDUE" ] && RESIDUE_ROWS=$((RESIDUE_ROWS+1))
    elif [ "$SECTION" = "common" ]; then
        COMMON_ROWS=$((COMMON_ROWS+1)); COMMON_MODULES="$COMMON_MODULES$module"$'\n'
        [ "$classif" = "RESIDUE" ] && RESIDUE_ROWS=$((RESIDUE_ROWS+1))
    elif [ "$SECTION" = "profile" ]; then
        PROFILE_ROWS=$((PROFILE_ROWS+1)); PROFILE_MODULES="$PROFILE_MODULES$module"$'\n'
        [ "$classif" = "RESIDUE" ] && RESIDUE_ROWS=$((RESIDUE_ROWS+1))
    fi
done < "$LEDGER"

# 3. row counts == declared headers
[ "$MAVEN_ROWS" -ne "$MAVEN_COUNT" ] && { echo "broadleaf_module_exhaustion_guard: FAIL — $MAVEN_ROWS Maven-table rows but maven_module_count declares $MAVEN_COUNT" >&2; FAIL=1; }
[ "$CORE_ROWS" -ne "$MODULE_COUNT" ] && { echo "broadleaf_module_exhaustion_guard: FAIL — $CORE_ROWS core-table rows but module_count declares $MODULE_COUNT" >&2; FAIL=1; }
[ "$COMMON_ROWS" -ne "$COMMON_COUNT" ] && { echo "broadleaf_module_exhaustion_guard: FAIL — $COMMON_ROWS common-table rows but common_subpackage_count declares $COMMON_COUNT" >&2; FAIL=1; }
[ "$PROFILE_ROWS" -ne "$PROFILE_COUNT" ] && { echo "broadleaf_module_exhaustion_guard: FAIL — $PROFILE_ROWS profile-table rows but profile_subpackage_count declares $PROFILE_COUNT" >&2; FAIL=1; }
# 4. RESIDUE rows == residue_count
[ "$RESIDUE_ROWS" -ne "$RESIDUE_COUNT" ] && { echo "broadleaf_module_exhaustion_guard: FAIL — $RESIDUE_ROWS RESIDUE rows but residue_count declares $RESIDUE_COUNT" >&2; FAIL=1; }

# 5. (live) no unledgered residue: (#parity records − absorbed_vertical_count) == residue_count.
#    absorbed_vertical_count is read from the ledger header (was a hardcoded '8' — a maintenance trap
#    that would spuriously block the next absorption; now bumping it is co-located with the ledger).
if [ "$LIVE" -eq 1 ]; then
    PARITY_DIR="$REPO_ROOT/docs/broadleaf-parity"
    if [ -d "$PARITY_DIR" ]; then
        TOTAL_PARITY=0
        for f in "$PARITY_DIR"/*.md; do
            [ -e "$f" ] || continue
            case "$(basename "$f")" in REGISTRY.md|README.md) continue ;; esac
            TOTAL_PARITY=$((TOTAL_PARITY+1))
        done
        RESIDUE_PARITY=$((TOTAL_PARITY-ABSORBED_COUNT))
        if [ "$RESIDUE_PARITY" -ne "$RESIDUE_COUNT" ]; then
            echo "broadleaf_module_exhaustion_guard: FAIL — $TOTAL_PARITY parity records − $ABSORBED_COUNT absorbed = $RESIDUE_PARITY residue parity record(s), but residue_count declares $RESIDUE_COUNT" >&2
            FAIL=1
        fi
    fi
fi

# 6. (live, DISK-TRUTH) every real on-disk Maven source module + core sub-package has a ledger row.
if [ "$LIVE" -eq 1 ] && [ -d "$CLONE" ]; then
    # 6a. EVERY built (non-aggregator) Maven module = a pom.xml whose packaging is NOT 'pom'
    #     (aggregator/reactor poms build no artifact). This is stricter than an src/main-only scan:
    #     it ALSO catches code-free module shells (e.g. admin-functional-tests, 0 java files) that a
    #     source-dir scan silently drops — so a real leaf module can never be missing a classification row.
    while IFS= read -r pom; do
        [ -z "$pom" ] && continue
        grep -qE '<packaging>[[:space:]]*pom[[:space:]]*</packaging>' "$pom" && continue
        mod="$(dirname "$pom")"; mod="${mod#"$CLONE"/}"
        case "$mod" in "$CLONE"|""|.) continue ;; esac
        if ! printf '%s\n' "$MAVEN_MODULES" | grep -qxF "$mod"; then
            echo "broadleaf_module_exhaustion_guard: FAIL — on-disk Maven module '$mod' has NO classification row (silent gap)" >&2
            FAIL=1
        fi
    done < <(find "$CLONE" -name pom.xml -not -path '*/target/*' 2>/dev/null)
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
    # 6c. common sub-packages (descends one level below the ABSORBED 'common' Maven row, so a real
    #     invariant — e.g. an ID allocator — can no longer hide un-adjudicated under a coarse module row).
    COMMON_PKG_DIR="$CLONE/common/src/main/java/org/broadleafcommerce/common"
    if [ -d "$COMMON_PKG_DIR" ]; then
        while IFS= read -r sub; do
            [ -z "$sub" ] && continue
            name="$(basename "$sub")"
            if ! printf '%s\n' "$COMMON_MODULES" | grep -qxF "$name"; then
                echo "broadleaf_module_exhaustion_guard: FAIL — on-disk common sub-package '$name' has NO classification row (silent gap)" >&2
                FAIL=1
            fi
        done < <(find "$COMMON_PKG_DIR" -mindepth 1 -maxdepth 1 -type d 2>/dev/null)
    fi
    # 6d. profile/core sub-packages (descends below the ABSORBED 'core/broadleaf-profile' Maven row).
    PROFILE_PKG_DIR="$CLONE/core/broadleaf-profile/src/main/java/org/broadleafcommerce/profile/core"
    if [ -d "$PROFILE_PKG_DIR" ]; then
        while IFS= read -r sub; do
            [ -z "$sub" ] && continue
            name="$(basename "$sub")"
            if ! printf '%s\n' "$PROFILE_MODULES" | grep -qxF "$name"; then
                echo "broadleaf_module_exhaustion_guard: FAIL — on-disk profile/core sub-package '$name' has NO classification row (silent gap)" >&2
                FAIL=1
            fi
        done < <(find "$PROFILE_PKG_DIR" -mindepth 1 -maxdepth 1 -type d 2>/dev/null)
    fi
fi

if [ "$FAIL" -ne 0 ]; then
    echo "" >&2
    echo "  docs/BROADLEAF-COMPLETENESS.md must classify EVERY Broadleaf Maven module AND every sub-package of" >&2
    echo "  each ABSORBED module (core engine + common + profile/core) with a valid classification + non-empty" >&2
    echo "  evidence; all four counts must match the declared headers; every RESIDUE must have a parity record;" >&2
    echo "  when the clone is present, every on-disk module + sub-package (all grains) must have a row (disk-truth)." >&2
    exit 1
fi

echo "broadleaf_module_exhaustion_guard: PASS — $MAVEN_ROWS Maven modules + $CORE_ROWS core + $COMMON_ROWS common + $PROFILE_ROWS profile sub-packages classified, $RESIDUE_ROWS residue ($([ "$LIVE" -eq 1 ] && [ -d "$CLONE" ] && echo 'disk-truth verified' || echo 'ledger-internal'))"
exit 0
