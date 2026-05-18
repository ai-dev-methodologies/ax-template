#!/usr/bin/env bash
# skills/ax-scaffold/scripts/new-business-recipe.sh — /ax-scaffold business subcommand.
#
# Reads recipes/<pattern>/RECIPE.md (YAML frontmatter) to get enabled_l4_domains
# and l2_blocks_used, then either prints a file plan (--dry-run) or runs
# /ax-verify-domain for each enabled L4 (normal mode).
#
# Usage:
#   new-business-recipe.sh <pattern> <project-name> [--dry-run]
#
# Exit 0 = success (dry-run complete, or all L4 domains verified)
# Exit 1 = RECIPE_NOT_FOUND | INVALID_ARGS | L4_MISSING | L4_VERIFY_FAILED
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# ── Parse args ────────────────────────────────────────────────────────────────
PATTERN=""
PROJECT_NAME=""
DRY_RUN=0

for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    -*)
      echo "new-business-recipe: ERROR — unknown flag: $arg" >&2
      exit 1
      ;;
    *)
      if [ -z "$PATTERN" ]; then
        PATTERN="$arg"
      elif [ -z "$PROJECT_NAME" ]; then
        PROJECT_NAME="$arg"
      else
        echo "new-business-recipe: ERROR — unexpected argument: $arg" >&2
        echo "  usage: new-business-recipe.sh <pattern> <project-name> [--dry-run]" >&2
        exit 1
      fi
      ;;
  esac
done

if [ -z "$PATTERN" ] || [ -z "$PROJECT_NAME" ]; then
  echo "new-business-recipe: ERROR — usage: new-business-recipe.sh <pattern> <project-name> [--dry-run]" >&2
  exit 1
fi

# ── Locate recipe ─────────────────────────────────────────────────────────────
RECIPE_MD="$REPO_ROOT/recipes/${PATTERN}/RECIPE.md"

if [ ! -f "$RECIPE_MD" ]; then
  echo "RECIPE_NOT_FOUND: recipes/${PATTERN}/RECIPE.md not found" >&2
  # Show available patterns
  if [ -d "$REPO_ROOT/recipes" ]; then
    available=$(find "$REPO_ROOT/recipes" -maxdepth 1 -mindepth 1 -type d -exec basename {} \; | sort | tr '\n' ' ')
    echo "  Available patterns: ${available:-none}" >&2
  fi
  exit 1
fi

# ── Parse YAML list from YAML frontmatter (between first and second ---) ──────
# Usage: parse_frontmatter_list <file> <key>
parse_frontmatter_list() {
  local file="$1"
  local key="$2"
  awk -v k="$key" '
    /^---$/ { fm++; if (fm >= 2) exit; next }
    fm == 1 && $0 ~ "^"k":" { in_key=1; next }
    in_key && /^  - / { item=$0; sub(/^  - /, "", item); print item; next }
    in_key && /^[a-zA-Z_]/ { in_key=0 }
  ' "$file"
}

# ── Read enabled L4 domains ───────────────────────────────────────────────────
ENABLED_L4=()
while IFS= read -r domain; do
  [ -n "$domain" ] && ENABLED_L4+=("$domain")
done < <(parse_frontmatter_list "$RECIPE_MD" "enabled_l4_domains")

if [ "${#ENABLED_L4[@]}" -eq 0 ]; then
  echo "new-business-recipe: ERROR — could not parse enabled_l4_domains from recipes/${PATTERN}/RECIPE.md" >&2
  exit 1
fi

# ── Read L2 blocks ────────────────────────────────────────────────────────────
L2_BLOCKS=()
while IFS= read -r block; do
  [ -n "$block" ] && L2_BLOCKS+=("$block")
done < <(parse_frontmatter_list "$RECIPE_MD" "l2_blocks_used")

# ── Print header ──────────────────────────────────────────────────────────────
echo "=== ax-scaffold business: new-business-recipe.sh ==="
echo ""
echo "Pattern:  $PATTERN"
echo "Project:  $PROJECT_NAME"
echo "Mode:     $([ "$DRY_RUN" -eq 1 ] && echo "dry-run" || echo "apply")"
echo "Recipe:   recipes/${PATTERN}/RECIPE.md"
echo ""

# ── Verify L4 domains exist in catalog ───────────────────────────────────────
echo "Enabled L4 domains (${#ENABLED_L4[@]}):"
L4_MISSING=()
for domain in "${ENABLED_L4[@]}"; do
  l4_dir="$REPO_ROOT/templates/L4/${domain}"
  if [ -d "$l4_dir" ]; then
    echo "  ✓ templates/L4/${domain}/"
  else
    echo "  ✗ templates/L4/${domain}/  (MISSING from catalog)"
    L4_MISSING+=("$domain")
  fi
done
echo ""

if [ "${#L4_MISSING[@]}" -gt 0 ]; then
  echo "new-business-recipe: ERROR — missing L4 domain(s) in catalog: ${L4_MISSING[*]}" >&2
  echo "  Run /ax-scaffold ${L4_MISSING[0]} to scaffold missing domain(s) first." >&2
  exit 1
fi

# ── L2 blocks ─────────────────────────────────────────────────────────────────
if [ "${#L2_BLOCKS[@]}" -gt 0 ]; then
  echo "L2 blocks to wire (${#L2_BLOCKS[@]}):"
  for block in "${L2_BLOCKS[@]}"; do
    echo "  - ${block}.tsx"
  done
  echo ""
fi

# ── Business composition output plan ─────────────────────────────────────────
echo "Business composition output:"
echo "  + ${PROJECT_NAME}/business-composition.yaml"
echo ""

# ── Dry-run: print plan, write nothing, exit 0 ───────────────────────────────
if [ "$DRY_RUN" -eq 1 ]; then
  echo "Dry-run mode: no files written."
  echo "new-business-recipe: DRY-RUN COMPLETE (exit 0)"
  exit 0
fi

# ── Apply mode: run /ax-verify-domain for each enabled L4 ─────────────────────
VERIFY_SCRIPT="$REPO_ROOT/skills/ax-verify-domain/scripts/run.sh"
VERIFY_FAILED=()

if [ ! -f "$VERIFY_SCRIPT" ]; then
  echo "new-business-recipe: WARNING — ax-verify-domain not found at skills/ax-verify-domain/scripts/run.sh" >&2
  echo "  Skipping L4 verification. Run /ax-verify-domain <domain> manually for each:" >&2
  for domain in "${ENABLED_L4[@]}"; do
    echo "    /ax-verify-domain $domain" >&2
  done
else
  echo "Verifying L4 domains via /ax-verify-domain..."
  for domain in "${ENABLED_L4[@]}"; do
    printf "  → /ax-verify-domain %-20s " "$domain"
    if bash "$VERIFY_SCRIPT" "$domain" >/dev/null 2>&1; then
      echo "✓ PASS"
    else
      echo "✗ FAIL"
      VERIFY_FAILED+=("$domain")
    fi
  done
  echo ""
fi

if [ "${#VERIFY_FAILED[@]}" -gt 0 ]; then
  echo "new-business-recipe: FAIL — L4 verification failed for: ${VERIFY_FAILED[*]}" >&2
  echo "  Run /ax-verify-domain <domain> for details." >&2
  exit 1
fi

# ── Write business-composition.yaml ──────────────────────────────────────────
TODAY="$(date +%Y-%m-%d)"
mkdir -p "$REPO_ROOT/${PROJECT_NAME}"
{
  echo "# ${PROJECT_NAME}/business-composition.yaml"
  echo "# Generated by: ax-scaffold business ${PATTERN} ${PROJECT_NAME}"
  echo "# Date: ${TODAY}"
  echo "pattern: ${PATTERN}"
  echo "project: ${PROJECT_NAME}"
  echo "generated: \"${TODAY}\""
  echo "enabled_l4_domains:"
  for domain in "${ENABLED_L4[@]}"; do
    echo "  - ${domain}"
  done
  if [ "${#L2_BLOCKS[@]}" -gt 0 ]; then
    echo "l2_blocks_used:"
    for block in "${L2_BLOCKS[@]}"; do
      echo "  - ${block}"
    done
  fi
} > "$REPO_ROOT/${PROJECT_NAME}/business-composition.yaml"

echo "Files written:"
echo "  + ${PROJECT_NAME}/business-composition.yaml"
echo ""
echo "Next steps:"
echo "  1. Annotate each enabled L4 domain README with: applied_recipe: ${PATTERN}"
echo "  2. Implement business logic referencing L4 catalog + L2 blocks listed in recipe"
echo "  3. Run /ax-verify-domain <domain> for each domain after implementation"
echo ""
echo "new-business-recipe: COMPLETE (exit 0)"
exit 0
