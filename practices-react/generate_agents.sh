#!/usr/bin/env bash
# practices-react/generate_agents.sh — produce practices-react/AGENTS.md by
# concatenating rules/*.md, embedding a sha256 of the concatenated content into
# the frontmatter sentinel. Mirror of practices/generate_agents.sh.
# Idempotent: a second run with no rule changes produces no diff.
set -euo pipefail

cd "$(dirname "$0")"

OUT="AGENTS.md"
RULES_GLOB="rules/*.md"

shopt -s nullglob
RULE_FILES=()
for f in $RULES_GLOB; do
    [[ "$(basename "$f")" == ".gitkeep" ]] && continue
    [[ "$(basename "$f")" == "_template.md" ]] && continue
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
  printf '  generated_by: "practices-react/generate_agents.sh"\n'
  printf -- '---\n\n'
  printf '# Practices-React — AGENTS.md (auto-generated)\n\n'
  printf 'This file is auto-generated from `practices-react/rules/*.md` in lexical order.\n'
  printf 'Do not edit by hand — re-run `practices-react/generate_agents.sh` after rule changes.\n\n'
  printf '## Pipeline\n\n'
  printf 'Every rule below shipped through a 4-phase curation pipeline:\n\n'
  printf '1. **Reference diversification** — Vercel react-best-practices skill (seed) cross-checked against React 19 / Next.js 16 / MDN canonical docs.\n'
  printf '2. **Per-rule audit** — accuracy, freshness, completeness, gap_check.\n'
  printf '3. **Codex consensus** — independent second opinion via `codex exec -s read-only`.\n'
  printf '4. **Continuous refresh** — each rule has `next_review_by`; time_decay_guard BLOCKs on stale.\n\n'
  printf 'See `practices-react/pilot/pilot-report.md` for the full audit trail.\n\n'
  for f in "${SORTED[@]}"; do
    printf -- '<!-- @source %s -->\n\n' "$f"
    cat "$f"
    printf '\n\n'
  done
} > "$OUT"

echo "wrote $OUT — $COUNT rules, sha=$SHA"
