#!/usr/bin/env bash
# lint_own_blocks_guard.sh — the catalog must lint its OWN shipped blocks.
#
# FDW1 (frontend dogfood, 2026-05-30): all 3 personas confirmed
# frontend/eslint.config.mjs globs only src/** and tests/**, so the catalog
# never enforces its own ax/* rules on the very blocks a fork copies into src/.
# column-picker.tsx shipped an ax/no-array-includes-in-loop violation invisible
# until copied (MappingEditor + column-reorder carried the same O(n*m) bug).
# This guard lints the shipped blocks (templates/L2/blocks + templates/L0) with
# the ax plugin and FAILS on any ax/* violation — closing the "catalog doesn't
# eat its own dogfood" gap.
#
# Notes:
#  - eslint v9 flat config refuses to lint files OUTSIDE the base path
#    (frontend/), so we copy the blocks into a temp dir UNDER frontend/ and lint
#    there. The ax rules are purely syntactic (AST, no type info) — copies lint
#    identically.
#  - We judge pass/fail on ax/* ruleIds ONLY (via --format json). Shipped blocks
#    carry inline `eslint-disable` directives for plugins this minimal config
#    does not load (react-hooks/*, @next/next/*); those produce non-ax noise we
#    deliberately ignore — the contract here is "no ax/* violation", not "clean
#    under the full Next lint chain".
#  - _fixtures/ and *.spec.ts/*.test.ts are excluded (test code, not shipped
#    blocks; their sequential awaits are intentional).
#
# Exit 0 = clean. Exit 1 = a shipped block carries an ax/* violation.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
FE="$ROOT/frontend"
CONFIG="eslint.own-blocks.config.mjs"
TMP="$FE/.lint-own-blocks-tmp"

cleanup() { rm -rf "$TMP"; }
trap cleanup EXIT

if [ ! -f "$FE/$CONFIG" ]; then
  echo "lint_own_blocks: FAIL — missing $FE/$CONFIG"
  exit 1
fi

# ── P3-55 (rev-4) — rule-list completeness check ────────────────────────────
# $CONFIG must wire every ax/* rule shipped in
# practices-react/eslint-plugin-ax/rules/, EXCEPT an explicit, reasoned
# exclusion allowlist below. Without this, a new plugin rule can ship and
# silently never run against the catalog's own blocks (exactly the FDW1 class
# this guard exists to close) because nobody remembered to add it to $CONFIG.
# Independent of node_modules/eslint — this is a text-level diff, checked even
# when the eslint-install-required lint pass below is skipped.
#
# EXCLUDED (must NOT be wired here, for a documented reason):
#   ax/no-app-local-ui-primitives — per its own rule doc
#     (practices-react/rules/no-app-local-ui-primitives.md), this rule is
#     scoped to files under an apps/ segment (flags an app re-implementing a
#     catalog primitive locally). templates/L0-L4 — what THIS guard lints —
#     ARE the shared catalog itself, not a consumer of it; applying the rule
#     here would flag the canonical primitives (Button/Input/Card/...) as if
#     they were local reimplementations of themselves. Category mismatch,
#     not a coverage gap.
EXCLUDED_RULES=("ax/no-app-local-ui-primitives")

RULES_DIR="$ROOT/practices-react/eslint-plugin-ax/rules"
if [ -d "$RULES_DIR" ]; then
  catalog_rules=()
  while IFS= read -r f; do
    catalog_rules+=("ax/$(basename "$f" .js)")
  done < <(find "$RULES_DIR" -maxdepth 1 -name '*.js' | sort)

  is_excluded() {
    local needle="$1"
    for ex in "${EXCLUDED_RULES[@]:-}"; do
      [ -n "$ex" ] && [ "$needle" = "$ex" ] && return 0
    done
    return 1
  }

  missing=()
  for r in "${catalog_rules[@]:-}"; do
    [ -z "$r" ] && continue
    is_excluded "$r" && continue
    grep -qF "'$r'" "$FE/$CONFIG" || missing+=("$r")
  done

  contradicted=()
  for ex in "${EXCLUDED_RULES[@]:-}"; do
    [ -z "$ex" ] && continue
    grep -qF "'$ex'" "$FE/$CONFIG" && contradicted+=("$ex")
  done

  if [ "${#missing[@]}" -gt 0 ] || [ "${#contradicted[@]}" -gt 0 ]; then
    echo "lint_own_blocks: FAIL — $CONFIG rule-list drift vs $RULES_DIR:"
    for r in "${missing[@]:-}"; do
      [ -n "$r" ] && echo "  - MISSING (shipped in the plugin, not wired in $CONFIG, not on the exclusion allowlist): $r"
    done
    for r in "${contradicted[@]:-}"; do
      [ -n "$r" ] && echo "  - CONTRADICTION (on the exclusion allowlist but ALSO wired in $CONFIG): $r"
    done
    exit 1
  fi
  echo "lint_own_blocks: rule-list check PASS — ${#catalog_rules[@]} plugin rule(s), $(( ${#catalog_rules[@]} - ${#EXCLUDED_RULES[@]} )) wired, ${#EXCLUDED_RULES[@]} explicitly excluded"
else
  echo "lint_own_blocks: rule-list check SKIP — $RULES_DIR not found"
fi

if [ ! -d "$FE/node_modules/eslint" ]; then
  echo "lint_own_blocks: SKIP — frontend/node_modules/eslint not installed (run npm ci in frontend/)"
  exit 0
fi

rm -rf "$TMP"; mkdir -p "$TMP"
[ -d "$ROOT/templates/L2/blocks" ] && cp -R "$ROOT/templates/L2/blocks" "$TMP/blocks"
[ -d "$ROOT/templates/L0" ] && cp -R "$ROOT/templates/L0" "$TMP/L0"
# P2-22 (audit-seal): templates/L1 (primitives) and templates/L4 (the domain
# verticals fork-receivers copy) were linted by NOTHING — 215 previously-unchecked
# .ts/.tsx files. The catalog must eat its own dogfood across EVERY shipped layer,
# not just L2/L0. Same copy+lint mechanism; ax rules are purely syntactic so copies
# lint identically.
[ -d "$ROOT/templates/L1" ] && cp -R "$ROOT/templates/L1" "$TMP/L1"
[ -d "$ROOT/templates/L4" ] && cp -R "$ROOT/templates/L4" "$TMP/L4"

# Collect shipped blocks (portable — macOS ships bash 3.2, no `mapfile`).
# Exclude test code: *.spec.* / *.test.* / any _fixtures dir / node_modules.
FILES=()
while IFS= read -r f; do
  [ -n "$f" ] && FILES+=("$f")
done < <(find "$TMP" -type f \( -name '*.tsx' -o -name '*.ts' \) \
  -not -name '*.spec.*' -not -name '*.test.*' \
  -not -path '*/_fixtures/*' -not -path '*/node_modules/*' 2>/dev/null | sort)

if [ "${#FILES[@]}" -eq 0 ]; then
  echo "lint_own_blocks: no shipped blocks found (nothing to lint)"
  exit 0
fi

cd "$FE" || { echo "lint_own_blocks: FAIL — cannot cd $FE"; exit 1; }

JSON="$(npx --no-install eslint --no-config-lookup --config "$CONFIG" --format json "${FILES[@]}" 2>/dev/null)"

# Judge pass/fail on ax/* ruleIds only; map temp paths back to templates/.
REPORT="$(printf '%s' "$JSON" | TMP="$TMP" python3 -c '
import json, os, sys
tmp = os.environ["TMP"]
try:
    data = json.load(sys.stdin)
except Exception as e:
    print("PARSE_ERROR " + str(e)); sys.exit(0)
viol = []
for f in data:
    path = f.get("filePath", "")
    rel = (path.replace(tmp + "/blocks", "templates/L2/blocks")
                .replace(tmp + "/L0", "templates/L0")
                .replace(tmp + "/L1", "templates/L1")
                .replace(tmp + "/L4", "templates/L4"))
    for m in f.get("messages", []):
        rid = m.get("ruleId") or ""
        if rid.startswith("ax/"):
            viol.append("%s:%s  %s  %s" % (rel, m.get("line","?"), rid, (m.get("message","") or "")[:90]))
print("AX_VIOLATIONS=%d" % len(viol))
for v in viol:
    print("  " + v)
')"

COUNT="$(printf '%s\n' "$REPORT" | sed -n 's/^AX_VIOLATIONS=//p')"

if printf '%s' "$REPORT" | grep -q '^PARSE_ERROR'; then
  echo "lint_own_blocks: FAIL — could not parse eslint JSON output:"
  printf '%s\n' "$REPORT" | head -3
  exit 1
fi

if [ "${COUNT:-0}" -ne 0 ]; then
  echo "lint_own_blocks: FAIL — ${COUNT} ax/* violation(s) in shipped catalog blocks:"
  printf '%s\n' "$REPORT" | grep '^  ' | head -40
  echo "  (fix the block at source; the catalog must satisfy its own rules — FDW1/FMW1)"
  exit 1
fi

echo "lint_own_blocks: PASS — ${#FILES[@]} shipped blocks (templates/L0 + L1 + L2/blocks + L4) satisfy all ax/* rules"
exit 0
