#!/usr/bin/env bash
# templates/backend/_check-anchors.sh
#
# For each Java template under templates/backend/**/*.java:
#   1. Parses the @ax-template-meta block to extract anchors_rule field
#   2. Verifies the referenced rule file exists in practices/rules/
#   3. Verifies evidence entries carry citation+url (source_type: external)
#      or a registered upstream_id from practices/upstream/_MANIFEST.yaml
#
# Exit 0  — all anchors valid
# Exit 1  — one or more violations (details printed to stdout)
#
# Usage:
#   bash templates/backend/_check-anchors.sh
#   bash templates/backend/_check-anchors.sh --verbose
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

VERBOSE=0
while [ $# -gt 0 ]; do
    case "$1" in
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "_check-anchors: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# Delegate all logic to Python — avoids bash 3.x/4.x portability issues
python3 "$SCRIPT_DIR/_check-anchors.py" \
    --templates-dir "$SCRIPT_DIR" \
    --rules-dir "$REPO_ROOT/practices/rules" \
    --manifest "$REPO_ROOT/practices/upstream/_MANIFEST.yaml" \
    ${VERBOSE:+--verbose}
