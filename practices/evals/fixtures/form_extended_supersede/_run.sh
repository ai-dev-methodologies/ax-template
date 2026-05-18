#!/usr/bin/env bash
# practices/evals/fixtures/form_extended_supersede/_run.sh
#
# Binary guard: asserts that SP15 form shells carry @deprecated JSDoc AND
# re-export from their corresponding -extended file.
#
# Checked shells: field-array.tsx, conditional-field.tsx,
#                 form-error-summary.tsx, form-section.tsx
#
# Exit 0 — all shells are superseded correctly (PASS)
# Exit 1 — any shell missing @deprecated or re-export (FAIL: SHELL_SUPERSEDE_INCOMPLETE)
#
# Usage:
#   bash practices/evals/fixtures/form_extended_supersede/_run.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
BLOCKS="$REPO_ROOT/templates/L2/blocks"

echo "=== form_extended_supersede guard ==="
echo ""

FAILED=0

check_shell() {
    local shell_file="$1"
    local extended_file="$2"
    local shell_name
    shell_name="$(basename "$shell_file")"

    # Shell must exist
    if [ ! -f "$shell_file" ]; then
        echo "  FAIL: $shell_name not found (expected SP15 shell at $shell_file)" >&2
        echo "  error: SHELL_SUPERSEDE_INCOMPLETE" >&2
        FAILED=1
        return
    fi

    # Extended file must exist
    if [ ! -f "$extended_file" ]; then
        echo "  FAIL: $extended_file not found (SP27 extended file missing)" >&2
        echo "  error: SHELL_SUPERSEDE_INCOMPLETE" >&2
        FAILED=1
        return
    fi

    # Shell must contain @deprecated
    if ! grep -q "@deprecated" "$shell_file"; then
        echo "  FAIL: $shell_name missing @deprecated JSDoc" >&2
        echo "  error: SHELL_SUPERSEDE_INCOMPLETE" >&2
        FAILED=1
        return
    fi

    # Shell must re-export from extended
    local extended_name
    extended_name="$(basename "$extended_file" .tsx)"
    if ! grep -q "$extended_name" "$shell_file"; then
        echo "  FAIL: $shell_name does not re-export from '$extended_name'" >&2
        echo "  error: SHELL_SUPERSEDE_INCOMPLETE" >&2
        FAILED=1
        return
    fi

    echo "  PASS: $shell_name — @deprecated present, re-exports from $extended_name"
}

# Check all 4 SP15 shells
check_shell "$BLOCKS/field-array.tsx"        "$BLOCKS/field-array-extended.tsx"
check_shell "$BLOCKS/conditional-field.tsx"  "$BLOCKS/conditional-field-extended.tsx"
check_shell "$BLOCKS/form-error-summary.tsx" "$BLOCKS/form-error-summary-extended.tsx"
check_shell "$BLOCKS/form-section.tsx"       "$BLOCKS/form-section-extended.tsx"

# Self-test: fail fixture must NOT pass the checks
FAIL_FIXTURE="$SCRIPT_DIR/fail_shell_no_reexport/field-array.tsx"
if [ -f "$FAIL_FIXTURE" ]; then
    if grep -q "@deprecated" "$FAIL_FIXTURE" && grep -q "field-array-extended" "$FAIL_FIXTURE"; then
        echo "  FAIL: fail fixture incorrectly has @deprecated + re-export — negative test broken" >&2
        exit 1
    fi
    echo "  PASS [fail fixture correctly missing @deprecated or re-export]"
fi

echo ""
if [ "$FAILED" -ne 0 ]; then
    echo "=== form_extended_supersede guard: FAIL ===" >&2
    exit 1
fi

echo "=== form_extended_supersede guard: PASS ==="
exit 0
