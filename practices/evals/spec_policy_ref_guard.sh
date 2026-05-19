#!/usr/bin/env bash
# spec_policy_ref_guard.sh — R17 hard guard
#
# Detects orphaned policy_ref entries in specs/*.yaml files.
# Each spec item that declares policy_ref: "blueprints/<file>.yaml#<anchor>"
# MUST have:
#   (1) the blueprint file exists on disk
#   (2) the <anchor> appears as a top-level YAML key in that blueprint file
#
# Motivation (R17): P2 ralplan persona simulation (R16 post-merge 3rd visit)
# discovered specs/multi-tenant-l0.yaml's 6 policy_ref entries all pointed to
# blueprints/multi-tenant-manifest.yaml#<anchor> but the blueprint file did
# not exist. cross_trio_guard only checks templates/ imports — spec-internal
# policy_ref dangling references slipped through.
#
# This is a CRITICAL hard guard: a violation means the catalog ships Spec Trio
# self-violation (spec declares a policy anchor that resolves to nothing).
#
# Usage: bash practices/evals/spec_policy_ref_guard.sh
# Exit code: 0 = all policy_refs resolve; 1 = at least one orphan found.

set -euo pipefail
cd "$(dirname "$0")/../.."

VIOLATIONS=0
TOTAL_REFS=0

# Find every policy_ref in specs/*.yaml
# Match pattern: policy_ref: "blueprints/<file>.yaml#<anchor>"
while IFS= read -r line; do
    spec_file=$(echo "$line" | cut -d: -f1)
    # Extract the policy_ref value (between quotes or after ': ')
    ref=$(echo "$line" | sed -E 's/.*policy_ref:[[:space:]]*"?([^"]+)"?.*/\1/' | tr -d '"' | xargs)

    [ -z "$ref" ] && continue

    # Skip if ref doesn't have the expected blueprints/.../#anchor shape
    if ! echo "$ref" | grep -qE '^blueprints/.+\.yaml#.+'; then
        continue
    fi

    TOTAL_REFS=$((TOTAL_REFS + 1))

    blueprint_file=$(echo "$ref" | cut -d'#' -f1)
    anchor=$(echo "$ref" | cut -d'#' -f2)

    # (1) blueprint file must exist
    if [ ! -f "$blueprint_file" ]; then
        echo "VIOLATION [spec-policy-ref-orphan]: $spec_file → $ref" >&2
        echo "  blueprint file does NOT exist: $blueprint_file" >&2
        VIOLATIONS=$((VIOLATIONS + 1))
        continue
    fi

    # (2) anchor must exist in blueprint — support nested keys (a.b.c)
    # Nested: "policy.max_per_window" → check "policy:" top-level + "max_per_window:" under it
    if echo "$anchor" | grep -q '\.'; then
        top=$(echo "$anchor" | cut -d. -f1)
        sub=$(echo "$anchor" | cut -d. -f2-)   # supports a.b.c.d (sub = "b.c.d")
        sub_first=$(echo "$sub" | cut -d. -f1)

        # Top key exists?
        if ! grep -qE "^${top}:" "$blueprint_file"; then
            echo "VIOLATION [spec-policy-ref-orphan]: $spec_file → $ref" >&2
            echo "  top-level anchor '$top' does NOT exist in $blueprint_file" >&2
            VIOLATIONS=$((VIOLATIONS + 1))
            continue
        fi

        # Sub-key exists under top? awk: enter top section, find sub-key indented
        # Note: variable name "sub" conflicts with awk builtin sub() function — use "subkey"
        if ! awk -v top="$top" -v subkey="$sub_first" 'BEGIN { top_re = "^" top ":"; sub_re = "^[[:space:]]+" subkey ":" }
            $0 ~ top_re { intop=1; next }
            intop && /^[^[:space:]#]/ { intop=0 }
            intop && $0 ~ sub_re { print "FOUND"; exit }
        ' "$blueprint_file" | grep -q FOUND; then
            echo "VIOLATION [spec-policy-ref-orphan]: $spec_file → $ref" >&2
            echo "  nested anchor '$anchor' does NOT resolve under '$top:' in $blueprint_file" >&2
            VIOLATIONS=$((VIOLATIONS + 1))
        fi
    else
        # Top-level anchor pattern: ^<anchor>:
        if ! grep -qE "^${anchor}:" "$blueprint_file"; then
            echo "VIOLATION [spec-policy-ref-orphan]: $spec_file → $ref" >&2
            echo "  anchor '$anchor' does NOT exist as top-level key in $blueprint_file" >&2
            VIOLATIONS=$((VIOLATIONS + 1))
        fi
    fi
done < <(grep -nE 'policy_ref:[[:space:]]+"?blueprints/' specs/*.yaml 2>/dev/null || true)

echo ""
echo "spec_policy_ref_guard: scanned $TOTAL_REFS policy_ref entries; $VIOLATIONS violation(s)."

if [ "$VIOLATIONS" -gt 0 ]; then
    echo "spec_policy_ref_guard: FAIL — Spec Trio integrity violated (spec declares anchor that resolves to nothing)" >&2
    exit 1
fi

echo "spec_policy_ref_guard: PASS — all policy_ref entries resolve to existing blueprint anchors"
exit 0
