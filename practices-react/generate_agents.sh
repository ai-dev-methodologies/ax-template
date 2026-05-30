#!/usr/bin/env bash
# practices-react/generate_agents.sh — keep AGENTS.md and SKILL.md in lockstep
# with the rules/ directory.
#
# Generates two artifacts:
#   1. AGENTS.md  — full concatenation of rules/*.md in lexical order with a
#                   sha256 sentinel in the frontmatter.
#   2. SKILL.md   — rule_count + family table refreshed; the rest of the file
#                   stays as authored (intent + how-to-use sections).
#
# Idempotent: a second run with no rule changes produces no diff.
# Portable: works on bash 3.2 (macOS) and bash 4+/5 (Linux).
set -euo pipefail

cd "$(dirname "$0")"

OUT_AGENTS="AGENTS.md"
OUT_SKILL="SKILL.md"
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

# ---- AGENTS.md ----
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

  # ── Kit index (FMW4b) ───────────────────────────────────────────────────────
  # FDW2 found a context-0 AI entering via this sentinel never learned the L0
  # fork-receiver-kit exists (grep=1). Surface it here. The L0 list is GLOBBED
  # from disk so the existence claim is disk-truth; descriptions are curated
  # with a graceful fallback so a newly-added primitive still lists honestly.
  printf '## Fork-receiver kit (L0) + key data-flow blocks (L2)\n\n'
  printf 'Cross-cutting client primitives every L4 frontend trio needs but the\n'
  printf 'catalog refuses to duplicate. The L0 kit sits BELOW L1 (pure TS, no JSX);\n'
  printf 'import via the absolute-style path `templates/L0/fork-receiver-kit/<file>`.\n'
  printf 'Full table + trade-offs: `templates/L0/fork-receiver-kit/README.md`.\n\n'
  printf '### L0 — fork-receiver-kit\n\n'
  for f in ../templates/L0/fork-receiver-kit/*.ts; do
    base="$(basename "$f" .ts)"
    case "$base" in
      use-caller-id) desc="useCallerId / useCallerRole / normalizeUserId / sameUser — caller identity (prod hard-stop + dev warn)";;
      parse-error) desc="parseError (RFC 9457 unwrap + Korean PII deny-list) + CodedError + sanitizeStoredError";;
      entity-key) desc="assertSafeEntityRef — path-segment defense for polymorphic entity refs";;
      parse-field-errors) desc="parseFieldErrors / extractFieldErrors — ProblemDetail per-field array -> Record<field,message> for setError";;
      use-url-list-state) desc="useUrlListState + listStateToQuery — typed page/sort/search/filter <-> query string (the URL-as-state easy path)";;
      use-idempotency-key) desc="useIdempotencyKey -> { key, regenerate } — Idempotency-Key lifecycle (pairs with backend IdempotencyKeyStore)";;
      money) desc="toMinorUnits / toMajorUnits / parseMinor / serializeMinor / fractionDigitsFor — integer minor units, no float, JSON wire-type";;
      use-conflict-resolution) desc="useConflictResolution / parseConflict / classifyConflict — 428/412/409 optimistic-lock conflict -> read-fresh-then-reconcile";;
      *) desc="(see templates/L0/fork-receiver-kit/README.md)";;
    esac
    printf -- '- `%s` — %s\n' "$base" "$desc"
  done
  printf '\n### L2 — data-flow seam blocks (mirror a backend contract)\n\n'
  printf 'Presentational halves of a backend contract — prop-driven, used beside the L0 hook:\n\n'
  for pair in \
    "bulk-result-panel|render a backend BulkResult (207 partial success): succeeded/failed + per-item errors" \
    "conflict-banner|present a useConflictResolution ConflictState: your-value vs server-value vs validator + reload/overwrite"; do
    blk="${pair%%|*}"; d="${pair#*|}"
    if [ -f "../templates/L2/blocks/${blk}.tsx" ]; then
      printf -- '- `%s` — %s\n' "$blk" "$d"
    fi
  done
  printf '\nThe full L2 catalog (90+ blocks: data-table, filter-bar, pagination, crud-*-form, ...) lives in `templates/L2/blocks/`.\n\n'

  for f in "${SORTED[@]}"; do
    printf -- '<!-- @source %s -->\n\n' "$f"
    cat "$f"
    printf '\n\n'
  done
} > "$OUT_AGENTS"

# ---- SKILL.md — count + family table ----
# Bash 3.2-portable: derive family from filename prefix, count via grep.
# Normalize `next-` → `nextjs-` so the rule next-async-params-parallel groups
# with the nextjs-use-cache* siblings in the family table.
count_family() {
    local family="$1"
    local n=0
    for f in "${SORTED[@]}"; do
        local base="$(basename "$f" .md)"
        local prefix="${base%%-*}"
        if [[ "$prefix" == "next" || "$prefix" == "nextjs" ]]; then
            prefix="nextjs"
        fi
        if [[ "$prefix" == "$family" ]]; then
            n=$((n + 1))
        fi
    done
    printf '%d' "$n"
}

render_family_row() {
    local family="$1" coverage="$2"
    local n
    n="$(count_family "$family")"
    if [[ "$n" -gt 0 ]]; then
        printf '| %s- | %s | %s |\n' "$family" "$n" "$coverage"
    fi
}

FAMILY_TABLE=$(
  printf '| Family | Rules | Coverage |\n'
  printf '|---|---|---|\n'
  render_family_row "async"    "Eliminating waterfalls + Next.js async params"
  render_family_row "bundle"   "Bundle size (lazy imports, third-party defer, conditional, preload)"
  render_family_row "server"   "Server-side (Cache Components, use-cache variants, parallel fetching, RSC serialization, auth actions, after())"
  render_family_row "client"   "Client data-fetching (server-state dedup, listeners, passive events, localStorage schema)"
  render_family_row "rerender" "Re-render correctness + perf (memo, derived state, refs, transitions, no-inline-components)"
  render_family_row "rendering" "Rendering performance (Activity, content-visibility, hydration, scripts, resource hints)"
  render_family_row "js"       "JavaScript performance (Set/Map, immutable arrays, regex, caching, iteration)"
  render_family_row "advanced" "Advanced effect callback patterns (useEffectEvent, init-once, handler refs)"
  render_family_row "nextjs"   "Next.js-specific extensions (use-cache directive, async params, use-cache private/remote)"
  # Reconciliation row — the named families above are curated buckets; the
  # remaining rules (ESLint-style no-/prefer- enforcement rules + L2/domain
  # singletons like saved-/currency-/combobox-) do not form a named family but
  # MUST still be counted so the table never under-reports against rule_count.
  # The practices_react_sentinel_disk_truth_guard asserts the Rules column sums
  # to rule_count; this row keeps that invariant true as the catalog grows.
  NAMED_TOTAL=0
  for fam in async bundle server client rerender rendering js advanced nextjs; do
    NAMED_TOTAL=$((NAMED_TOTAL + $(count_family "$fam")))
  done
  OTHER=$((COUNT - NAMED_TOTAL))
  if [ "$OTHER" -gt 0 ]; then
    printf '| (other) | %d | ESLint-style no-/prefer- enforcement + L2/domain singletons (saved-, currency-, combobox-, impersonation-, rich-, traceid-, virtualized-, business-) not in a named family |\n' "$OTHER"
  fi
)

if [[ -f "$OUT_SKILL" ]]; then
    python3 - "$OUT_SKILL" "$COUNT" "$FAMILY_TABLE" <<'PYEOF'
import pathlib, re, sys
path = pathlib.Path(sys.argv[1])
count = int(sys.argv[2])
table = sys.argv[3]

text = path.read_text()

# 1) Replace "N rules" / "N-rule" patterns inside the description and intro
#    (first 60 lines only — body has its own "N rules" mentions in audit
#    snippets that should not be touched).
lines = text.splitlines()
# Edit the intro region only: frontmatter + title + intro paragraph, i.e.
# everything BEFORE the first "## " H2 heading. This auto-maintains the
# intro "N rules" sentence (which sat past the old fixed 60-line cutoff and
# silently drifted 68→86). The body keeps its own "N rules" mentions in the
# pipeline/audit snippets, which must NOT be rewritten.
split_idx = next((i for i, l in enumerate(lines) if l.startswith('## ')), 60)
head = lines[:split_idx]
body = lines[split_idx:]
new_head = []
for line in head:
    line2 = re.sub(r'\b(\d+) rules\b', f'{count} rules', line)
    line2 = re.sub(r'\b(\d+)-rule\b', f'{count}-rule', line2)
    new_head.append(line2)
text = '\n'.join(new_head + body)

# 2) Replace the auto-managed family table block.
begin = '<!-- BEGIN:auto-family-table -->'
end = '<!-- END:auto-family-table -->'
new_block = f'{begin}\n{table}\n{end}'
if begin in text and end in text:
    text = re.sub(
        re.escape(begin) + r'.*?' + re.escape(end),
        new_block,
        text,
        flags=re.DOTALL,
    )
else:
    # No block yet — inject one before "## Pipeline" if present, otherwise at end.
    anchor = '## Pipeline'
    if anchor in text:
        text = text.replace(anchor, new_block + '\n\n' + anchor, 1)
    else:
        text = text.rstrip() + '\n\n' + new_block + '\n'

path.write_text(text)
PYEOF
fi

echo "wrote $OUT_AGENTS — $COUNT rules, sha=$SHA"
echo "wrote $OUT_SKILL — count + family table refreshed"
