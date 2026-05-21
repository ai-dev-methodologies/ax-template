#!/usr/bin/env bash
# practices/generate_agents.sh — produce practices/AGENTS.md by concatenating
# rules/*.md (sentinel sha covers rule concat ONLY — TD-024 sha-input clause)
# and appending an observability TOC outside the fingerprint (TD-033 R13).
# Idempotent: 2nd run with no rule/L4/recipe/verdict changes produces no diff.
set -euo pipefail
cd "$(dirname "$0")"
OUT="AGENTS.md"
RULES_GLOB="rules/*.md"

# Section A: rule concat + sha (UNCHANGED from R12) --------------------------
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

# Section B: parse recipes/_MANIFEST.yaml ONCE (Codex soft #1) ---------------
# One line per active recipe: "pattern|spec|verdict|domain1, domain2, ..."
MANIFEST="../recipes/_MANIFEST.yaml"
MANIFEST_ROWS="$(awk '
    /^recipes:/ {in_recipes=1; next}
    in_recipes && /^  - pattern:/ {
        if (pat != "") emit()
        pat=$3; spec=""; verdict=""; doms=""; in_doms=0; next
    }
    in_recipes && /^    spec:/             {spec=$2; in_doms=0; next}
    in_recipes && /^    sealed_verdict:/   {verdict=$2; in_doms=0; next}
    in_recipes && /^    enabled_l4_domains:/ {in_doms=1; next}
    in_recipes && in_doms && /^      - / {
        if (doms == "") doms=$2; else doms=doms ", " $2; next
    }
    in_recipes && in_doms && /^    [a-z]/ {in_doms=0}
    in_recipes && /^[a-z]/ {in_recipes=0}
    END {if (pat != "") emit()}
    function emit() {printf "%s|%s|%s|%s\n", pat, spec, verdict, doms}
' "$MANIFEST" | sort)"

# Helper: comma-space join over stdin (Codex L fix — replaces broken
# `paste -sd ", "` which cycles delimiter chars). Pure awk; deterministic.
join_cs() { awk 'NR>1{printf ", "} {printf "%s", $0} END{if (NR>0) print ""}'; }

{
  # frontmatter sentinel (covers rule concat ONLY; TD-024 sha-input)
  printf -- '---\nsentinel:\n  source_concat_sha256: "%s"\n  rule_count: %s\n  generated_by: "practices/generate_agents.sh"\n---\n\n' "$SHA" "$COUNT"
  printf '# Practices — AGENTS.md (auto-generated)\n\nThis file is auto-generated from `practices/rules/*.md` in lexical order.\nDo not edit by hand — re-run `practices/generate_agents.sh` after rule changes.\n\nSentinel sha covers rule concat ONLY (TD-024 sha-input clause).\nTOC section below is observability outside the fingerprint (TD-033 R13).\n\n## MANDATORY (R25) before declaring any task done\n\nAI agents MUST run `bash practices/scripts/verify-completion.sh` and confirm\nexit 0 before stating the task is complete. The 49th hard guard\n(`completion_checklist_recency_guard.sh`) audits the resulting log and\nBLOCKS push when no entry matches HEAD. There is no opt-out flag.\n\n'

  # rule concat body (UNCHANGED from R12)
  for f in "${SORTED[@]}"; do
    printf -- '<!-- @source %s -->\n\n' "$f"
    cat "$f"
    printf '\n\n'
  done

  # NEW: TOC section (outside sentinel fingerprint; TD-033)
  printf -- '---\n\n# Catalog TOC (observability — not part of sentinel sha)\n\n## L4 domains\n\n'
  for d in ../templates/L4/*/; do
    name="$(basename "$d")"
    applied="$(awk '
        /^applied_recipes:/ {flag=1; next}
        flag && /^  - / {print $2}
        flag && /^[a-z]/ {flag=0}
    ' "$d/README.md" 2>/dev/null | join_cs)"
    [[ -z "$applied" ]] && applied="(none)"
    printf -- '- **%s** — applied by: %s\n' "$name" "$applied"
  done
  printf '\n## Active recipes\n\n'
  while IFS='|' read -r pat spec verdict doms; do
    [[ -z "$doms" ]] && doms="(unknown)"
    printf -- '- **%s** — enabled L4: %s\n' "$pat" "$doms"
  done <<< "$MANIFEST_ROWS"
  printf '\n## Sealed verdicts\n\n'
  for v in ../skills/_tests/sealed-verdict/*.md; do
    [[ "$(basename "$v")" == "README.md" ]] && continue
    printf -- '- %s\n' "$(basename "$v" .md)"
  done
  printf '\n'
} > "$OUT"

# Inline disk-truth assertions (proof of 12 / 11 / 13)
L4_COUNT=$(ls -d ../templates/L4/*/ 2>/dev/null | wc -l | tr -d ' ')
REC_COUNT=$(printf '%s\n' "$MANIFEST_ROWS" | grep -c '|' || true)
VER_COUNT=$(ls ../skills/_tests/sealed-verdict/*.md 2>/dev/null | grep -v README | wc -l | tr -d ' ')
[[ "$L4_COUNT"  == "12" ]] || { echo "ASSERT FAIL: L4 $L4_COUNT != 12"  >&2; exit 1; }
[[ "$REC_COUNT" == "11" ]] || { echo "ASSERT FAIL: recipes $REC_COUNT != 11" >&2; exit 1; }
[[ "$VER_COUNT" == "13" ]] || { echo "ASSERT FAIL: verdicts $VER_COUNT != 13" >&2; exit 1; }
echo "wrote $OUT — $COUNT rules, sha=$SHA, TOC: $L4_COUNT L4 / $REC_COUNT recipes / $VER_COUNT verdicts"
