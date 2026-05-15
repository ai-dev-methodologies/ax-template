#!/usr/bin/env bash
# spec_ref_guard.sh — hard gate: every practices/rules/*.md must have a valid spec_ref
# Exit 1 if any rule is missing spec_ref or references a non-existent specs/*.yaml file.
# Exit 0 if no rules exist or all rules pass.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RULES_DIR="$REPO_ROOT/practices/rules"

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
    # spec_ref format: specs/{file}.yaml#{ITEM-ID} — extract the file path part
    spec_file_rel="${spec_ref%%#*}"
    spec_file_abs="$REPO_ROOT/$spec_file_rel"

    if [ ! -f "$spec_file_abs" ]; then
        echo "VIOLATION [$name]: spec_ref '$spec_ref' → file '$spec_file_rel' does not exist" >&2
        violations=$((violations + 1))
    fi
done

if [ "$violations" -gt 0 ]; then
    echo "spec_ref_guard: $violations violation(s) found — merge BLOCKED" >&2
    exit 1
fi

exit 0
