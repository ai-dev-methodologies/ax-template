#!/usr/bin/env bash
# Fixture runner: rich-content-must-use-dynamic-import
# Rule: RichTextEditor/MarkdownRenderer must use next/dynamic in Server Components.
#
# Detection logic:
#   A file violates the rule if it:
#   1. Does NOT have 'use client' at the top (Server Component), AND
#   2. Has a static import of RichTextEditor or MarkdownRenderer
#
# Exit 0 = PASS (no violations)
# Exit 1 = FAIL (violation detected)

set -euo pipefail

FIXTURE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FAIL_DIR="$FIXTURE_DIR/fail_static_server_import"
PASS_DIR="$FIXTURE_DIR/pass"

fail_violations=0
pass_violations=0

check_violation() {
  local file="$1"
  # Check if file has 'use client'
  if grep -qE "^['\"]use client['\"]" "$file"; then
    return 1  # Client Component — no violation
  fi
  # Check for static import of RichTextEditor or MarkdownRenderer
  if grep -qE "import.*\{.*RichTextEditor|import.*\{.*MarkdownRenderer" "$file"; then
    return 0  # Violation: static import in Server Component
  fi
  return 1  # No violation
}

# Check FAIL fixture — expect violation
for f in "$FAIL_DIR"/*.tsx "$FAIL_DIR"/*.ts; do
  [ -f "$f" ] || continue
  if check_violation "$f"; then
    echo "✓ FAIL fixture correctly detected: $f"
    fail_violations=$((fail_violations + 1))
  else
    echo "✗ FAIL fixture missed — expected violation in: $f" >&2
    exit 1
  fi
done

# Check PASS fixture — expect no violation
for f in "$PASS_DIR"/*.tsx "$PASS_DIR"/*.ts; do
  [ -f "$f" ] || continue
  if check_violation "$f"; then
    echo "✗ PASS fixture has unexpected violation: $f" >&2
    pass_violations=$((pass_violations + 1))
  else
    echo "✓ PASS fixture is clean: $f"
  fi
done

if [ "$pass_violations" -gt 0 ]; then
  echo "FAIL: $pass_violations pass fixture(s) unexpectedly triggered the rule" >&2
  exit 1
fi

if [ "$fail_violations" -eq 0 ]; then
  echo "FAIL: No violations detected in fail fixtures — rule scanner missed all cases" >&2
  exit 1
fi

echo "PASS: rich-content-must-use-dynamic-import fixture runner OK"
exit 0
