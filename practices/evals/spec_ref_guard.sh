#!/usr/bin/env bash
# spec_ref_guard.sh — hard gate: every {catalog}/rules/*.md must have a valid spec_ref
# Exit 1 if any rule is missing spec_ref or references a non-existent specs/*.yaml file.
# Exit 0 if no rules exist or all rules pass.
#
# Usage:
#   bash practices/evals/spec_ref_guard.sh                       # default catalog=practices
#   bash practices/evals/spec_ref_guard.sh --catalog practices-react
#   CATALOG=practices-react bash practices/evals/spec_ref_guard.sh

set -euo pipefail

CATALOG="${CATALOG:-practices}"
while [ $# -gt 0 ]; do
    case "$1" in
        --catalog) CATALOG="$2"; shift 2 ;;
        --catalog=*) CATALOG="${1#--catalog=}"; shift ;;
        *) echo "spec_ref_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RULES_DIR="$REPO_ROOT/$CATALOG/rules"

if [ ! -d "$RULES_DIR" ]; then
    echo "spec_ref_guard: catalog '$CATALOG' has no rules/ dir at $RULES_DIR — nothing to check"
    exit 0
fi

# Extract spec_ref value from YAML frontmatter (between first pair of --- lines)
extract_spec_ref() {
    local file="$1"
    awk '
        /^---$/ { fence++; next }
        fence == 1 && /^spec_ref:/ {
            sub(/^spec_ref:[[:space:]]*/, "")
            gsub(/^["'"'"']|["'"'"']$/, "")
            print
            exit
        }
        fence >= 2 { exit }
    ' "$file"
}

# Collect all rule files (nullglob: empty match → empty array, not literal glob string)
shopt -s nullglob
rules=("$RULES_DIR"/*.md)
shopt -u nullglob

# Empty rules/ is not an error — skeleton is valid before any rules land
if [ ${#rules[@]} -eq 0 ]; then
    exit 0
fi

violations=0

# R88 strengthening — item-id matching with grandfathered allow-list.
# Pre-R88 the guard only verified file existence (spec_file_abs check
# below); ~52 legacy rules accumulated spec_refs to imagined item-ids.
# R88 now verifies the item-id is actually present in the spec yaml,
# with an explicit allow-list for the legacy orphans (see
# practices/.spec-ref-legacy-orphans.txt).
LEGACY_ORPHAN_FILE="$REPO_ROOT/$CATALOG/.spec-ref-legacy-orphans.txt"

# Load allow-list into a flat env var (Bash 3.2: no associative array).
# Newline-separated entries, comments stripped.
LEGACY_ORPHANS=""
if [ -f "$LEGACY_ORPHAN_FILE" ]; then
    LEGACY_ORPHANS=$(grep -vE '^[[:space:]]*(#|$)' "$LEGACY_ORPHAN_FILE" 2>/dev/null || true)
fi

# Extract item-ids from a spec yaml. Uses python+yaml so quoted /
# unquoted / single-quoted forms are all parsed consistently. The
# previous grep-based extractor only matched `- id: "..."` (double-
# quoted), missing 117 unquoted IDs across the catalog and producing
# false grandfather entries in the legacy-orphan allow-list.
extract_spec_ids() {
    local file="$1"
    python3 - "$file" <<'PY'
import pathlib, sys, yaml
doc = yaml.safe_load(pathlib.Path(sys.argv[1]).read_text()) or {}
ids = set()
def walk(n):
    if isinstance(n, dict):
        if isinstance(n.get("id"), str):
            ids.add(n["id"])
        for v in n.values():
            walk(v)
    elif isinstance(n, list):
        for v in n:
            walk(v)
walk(doc)
for i in sorted(ids):
    print(i)
PY
}

for rule in "${rules[@]}"; do
    name="$(basename "$rule")"

    spec_ref="$(extract_spec_ref "$rule")"

    # (a) field must be non-empty
    if [ -z "$spec_ref" ]; then
        echo "VIOLATION [$name]: spec_ref field is missing or empty" >&2
        violations=$((violations + 1))
        continue
    fi

    # (b) referenced specs/*.yaml file must exist
    # spec_ref format: specs/{file}.yaml#{ITEM-ID} — extract parts
    spec_file_rel="${spec_ref%%#*}"
    spec_file_abs="$REPO_ROOT/$spec_file_rel"

    if [ ! -f "$spec_file_abs" ]; then
        echo "VIOLATION [$name]: spec_ref '$spec_ref' → file '$spec_file_rel' does not exist" >&2
        violations=$((violations + 1))
        continue
    fi

    # (c) R88 — item-id portion (after the '#') must exist in the spec yaml,
    # OR the full spec_ref must appear verbatim in the legacy-orphans allow-list.
    case "$spec_ref" in
        *"#"*) item_id="${spec_ref#*#}" ;;
        *) item_id="" ;;
    esac

    if [ -n "$item_id" ]; then
        # Whitelist check: full spec_ref present in legacy-orphans file
        if [ -n "$LEGACY_ORPHANS" ] \
            && echo "$LEGACY_ORPHANS" | grep -Fxq -- "$spec_ref"; then
            continue
        fi

        # Hard check: item-id present in the spec yaml
        if ! extract_spec_ids "$spec_file_abs" | grep -Fxq -- "$item_id"; then
            echo "VIOLATION [$name]: spec_ref '$spec_ref' → item-id '$item_id' not found in $spec_file_rel" >&2
            echo "  (resolve by adding '$item_id' as a real spec item, OR by retargeting spec_ref to an existing id)" >&2
            echo "  (legacy orphans grandfathered via $CATALOG/.spec-ref-legacy-orphans.txt — new rules must use real ids)" >&2
            violations=$((violations + 1))
        fi
    fi
done

if [ "$violations" -gt 0 ]; then
    echo "spec_ref_guard: $violations violation(s) found — merge BLOCKED" >&2
    exit 1
fi

# ── templates/ walk extension (§4.10) ────────────────────────────────────────
# Walk templates/**/*.{md,tsx,ts,yaml,java}.
# Zero-scan guard: if templates/ exists but produces zero matching files → FAIL ZERO_SCAN.

TEMPLATES_DIR="$REPO_ROOT/templates"
if [ -d "$TEMPLATES_DIR" ]; then
    templates_count=0
    while IFS= read -r f; do
        [ -f "$f" ] && templates_count=$((templates_count + 1))
    done < <(find "$TEMPLATES_DIR" \
        -name "*.md" -o -name "*.tsx" -o -name "*.ts" -o -name "*.yaml" -o -name "*.java" 2>/dev/null)

    if [ "$templates_count" -eq 0 ]; then
        echo "spec_ref_guard: ZERO_SCAN — templates/ exists but no scannable files found — merge BLOCKED" >&2
        exit 1
    fi
    echo "spec_ref_guard: templates/ walk found ${templates_count} file(s)"
fi

exit 0
