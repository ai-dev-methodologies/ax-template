#!/usr/bin/env bash
# recipes/_check-anchors.sh
#
# Validates evidence blocks in all recipe RECIPE.md files under recipes/*/
# Rules:
#   - Every external provenance_class entry must have url + citation fields
#   - Every internal_design provenance_class entry must have rationale field
#   - At least 1 external citation with a Korean URL (channel.io, tosspayments.com,
#     coupangcorp.com, kakaocorp.com, or similarly recognized Korean vendor) per recipe
#
# Exit 0  — all anchors valid
# Exit 1  — one or more violations (details printed to stdout)
#
# Usage:
#   bash recipes/_check-anchors.sh
#   bash recipes/_check-anchors.sh --verbose
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

VERBOSE=0
while [ $# -gt 0 ]; do
    case "$1" in
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "_check-anchors: unknown arg: $1" >&2; exit 2 ;;
    esac
done

violations=0

shopt -s nullglob
recipe_files=("$SCRIPT_DIR"/*/RECIPE.md)
shopt -u nullglob

if [ ${#recipe_files[@]} -eq 0 ]; then
    echo "_check-anchors: no RECIPE.md files found under recipes/ — nothing to check"
    exit 0
fi

for recipe in "${recipe_files[@]}"; do
    pattern_dir="$(basename "$(dirname "$recipe")")"

    # --- check: at least 1 external provenance with url+citation ---
    external_count=$(python3 - <<PY
import re, pathlib, sys
text = pathlib.Path("$recipe").read_text()
# Find evidence block entries with provenance_class: external
blocks = re.findall(r'provenance_class:\s*external.*?(?=\nprovenance_class:|\Z)', text, re.DOTALL)
valid = 0
for b in blocks:
    has_url = bool(re.search(r'url:\s*\S+', b))
    has_citation = bool(re.search(r'citation:\s*\S+', b))
    if has_url and has_citation:
        valid += 1
print(valid)
PY
    )

    if [ "$external_count" -lt 1 ]; then
        echo "VIOLATION [$pattern_dir/RECIPE.md]: requires ≥1 external citation with url + citation fields (found $external_count)"
        violations=$((violations + 1))
    elif [ "$VERBOSE" -eq 1 ]; then
        echo "OK [$pattern_dir/RECIPE.md]: $external_count external citation(s) with url+citation"
    fi

    # --- check: internal_design entries have rationale ---
    internal_missing=$(python3 - <<PY
import re, pathlib
text = pathlib.Path("$recipe").read_text()
blocks = re.findall(r'provenance_class:\s*internal_design.*?(?=\nprovenance_class:|\Z)', text, re.DOTALL)
missing = 0
for b in blocks:
    if not re.search(r'rationale:\s*\S+', b):
        missing += 1
print(missing)
PY
    )

    if [ "$internal_missing" -gt 0 ]; then
        echo "VIOLATION [$pattern_dir/RECIPE.md]: $internal_missing internal_design evidence block(s) missing rationale field"
        violations=$((violations + 1))
    elif [ "$VERBOSE" -eq 1 ]; then
        echo "OK [$pattern_dir/RECIPE.md]: all internal_design blocks have rationale"
    fi

    # --- check: at least 1 Korean-vendor URL per recipe ---
    korean_count=$(python3 - <<PY
import re, pathlib
text = pathlib.Path("$recipe").read_text()
korean_domains = ['tosspayments.com', 'channel.io', 'coupangcorp.com', 'kakao.com',
                  'kakaocorp.com', 'naver.com', 'navercorp.com', 'yanoljacorp.com', 'catchteam.kr']
urls = re.findall(r'url:\s*(\S+)', text)
count = sum(1 for u in urls if any(d in u for d in korean_domains))
print(count)
PY
    )

    if [ "$korean_count" -lt 1 ]; then
        echo "VIOLATION [$pattern_dir/RECIPE.md]: requires ≥1 Korean-vendor URL in evidence block (found $korean_count)"
        violations=$((violations + 1))
    elif [ "$VERBOSE" -eq 1 ]; then
        echo "OK [$pattern_dir/RECIPE.md]: $korean_count Korean-vendor URL(s)"
    fi
done

# Emit observability signal
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u +"%Y-%m-%dT%H:%M:%SZ")
if [ "$violations" -eq 0 ]; then
    echo '{"signal":"recipe.anchor_check.pass_count","value":'"${#recipe_files[@]}"',"ts":"'"$TIMESTAMP"'"}'
else
    echo '{"signal":"recipe.anchor_check.violation_count","value":'"$violations"',"ts":"'"$TIMESTAMP"'"}'
fi

exit $violations
