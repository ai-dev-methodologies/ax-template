#!/usr/bin/env bash
# practices/generate_agents.sh — produce practices/AGENTS.md by concatenating rules/*.md,
# embedding a sha256 of the concatenated content into the frontmatter sentinel.
# Idempotent: a second run with no rule changes produces no diff.
set -euo pipefail

cd "$(dirname "$0")"

OUT="AGENTS.md"
RULES_GLOB="rules/*.md"

# Collect rule files in lexical order, skip the .gitkeep
shopt -s nullglob
RULE_FILES=()
for f in $RULES_GLOB; do
    [[ "$(basename "$f")" == ".gitkeep" ]] && continue
    RULE_FILES+=("$f")
done
IFS=$'\n' SORTED=($(printf '%s\n' "${RULE_FILES[@]}" | sort)); unset IFS

CONCAT="$(printf '' && for f in "${SORTED[@]}"; do cat "$f"; printf '\n'; done)"
SHA="$(printf '%s' "$CONCAT" | shasum -a 256 | awk '{print $1}')"
COUNT="${#SORTED[@]}"

{
  printf -- '---\n'
  printf 'sentinel:\n'
  printf '  source_concat_sha256: "%s"\n' "$SHA"
  printf '  rule_count: %s\n' "$COUNT"
  printf '  generated_by: "practices/generate_agents.sh"\n'
  printf -- '---\n\n'
  printf '# Practices — AGENTS.md (auto-generated)\n\n'
  printf 'This file is auto-generated from `practices/rules/*.md` in lexical order.\n'
  printf 'Do not edit by hand — re-run `practices/generate_agents.sh` after rule changes.\n\n'
  for f in "${SORTED[@]}"; do
    printf -- '<!-- @source %s -->\n\n' "$f"
    cat "$f"
    printf '\n\n'
  done
} > "$OUT"

echo "wrote $OUT — $COUNT rules, sha=$SHA"
