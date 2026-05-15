#!/usr/bin/env bash
#
# practices/scripts/setup-branch-protection.sh — apply branch protection to main.
#
# Reads .github/rulesets/main-protection.json and applies it via `gh api` to the
# current repository. Idempotent — re-running yields no change if the live state
# already matches. Run once after initial repo setup; re-run after editing the
# JSON if policy changes are needed.
#
# Requires:
#   - gh CLI authenticated (gh auth status)
#   - repo admin permission for the calling user/token
#
# Usage:
#   bash practices/scripts/setup-branch-protection.sh           # apply
#   bash practices/scripts/setup-branch-protection.sh --dry-run # show payload, do not apply
#   bash practices/scripts/setup-branch-protection.sh --check   # diff live vs. desired
#
set -euo pipefail

cd "$(dirname "$0")/../.."

POLICY_FILE=".github/rulesets/main-protection.json"
[[ -f "$POLICY_FILE" ]] || { echo "policy file missing: $POLICY_FILE" >&2; exit 1; }

DRY_RUN=false
CHECK_ONLY=false
case "${1:-}" in
    --dry-run) DRY_RUN=true ;;
    --check)   CHECK_ONLY=true ;;
    "")        ;;
    *)         echo "usage: $0 [--dry-run|--check]" >&2; exit 2 ;;
esac

command -v gh  >/dev/null || { echo "gh CLI not installed" >&2; exit 3; }
command -v jq  >/dev/null || { echo "jq not installed (brew install jq)" >&2; exit 3; }

gh auth status >/dev/null 2>&1 || { echo "gh not authenticated (run: gh auth login)" >&2; exit 4; }

# Strip _comment keys before sending to GitHub API
PAYLOAD="$(jq 'del(._comment)' "$POLICY_FILE")"

# Resolve owner/repo from current remote
ORIGIN_URL="$(git config --get remote.origin.url)"
case "$ORIGIN_URL" in
    *github.com:*)  SLUG="$(echo "$ORIGIN_URL" | sed -E 's|.*github.com:([^/]+/[^/.]+)(\.git)?|\1|')" ;;
    *github.com/*) SLUG="$(echo "$ORIGIN_URL" | sed -E 's|.*github.com/([^/]+/[^/.]+)(\.git)?|\1|')" ;;
    *) echo "remote.origin.url is not a github URL: $ORIGIN_URL" >&2; exit 5 ;;
esac

echo "repo:    $SLUG"
echo "branch:  main"
echo "policy:  $POLICY_FILE"
echo ""

if $DRY_RUN; then
    echo "=== payload (dry-run, not applied) ==="
    echo "$PAYLOAD" | jq .
    exit 0
fi

if $CHECK_ONLY; then
    echo "=== live state ==="
    LIVE="$(gh api "/repos/$SLUG/branches/main/protection" 2>/dev/null || echo '{}')"
    echo "$LIVE" | jq .
    echo ""
    echo "=== desired ==="
    echo "$PAYLOAD" | jq .
    echo ""
    echo "Compare manually — fields the API omits when null are normal."
    exit 0
fi

echo "applying branch protection..."
echo "$PAYLOAD" | gh api \
    -X PUT \
    -H "Accept: application/vnd.github+json" \
    "/repos/$SLUG/branches/main/protection" \
    --input -

echo ""
echo "✓ branch protection applied to $SLUG@main"
echo ""
echo "Verify in the GitHub UI:"
echo "  https://github.com/$SLUG/settings/branches"
