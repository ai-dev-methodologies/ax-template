#!/usr/bin/env bash
# skills/ax-verify/scripts/explain.sh -- F15: rule explanation lookup.
#
# AI agents invoke this to get a concise, structured explanation of a
# catalog rule: what it requires, why it matters, and Correct/Incorrect examples.
#
# Usage:
#   explain.sh PRACTICES-PERS-005          # look up by spec_id
#   explain.sh soft-delete                 # look up by keyword (title / tags / filename)
#   explain.sh --list                      # list all rule IDs with titles
#   explain.sh --format json PRACTICES-ERR-001
#
# Exit codes:
#   0 -- rule found; explanation printed
#   1 -- rule not found
#   2 -- invalid arguments

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
RULES_DIR="$REPO_ROOT/practices/rules"

# -- Argument parsing ----------------------------------------------------------
FORMAT="text"
LIST_MODE=false
QUERY=""

while [ $# -gt 0 ]; do
    case "$1" in
        --list)       LIST_MODE=true; shift ;;
        --format)     FORMAT="$2"; shift 2 ;;
        --format=*)   FORMAT="${1#--format=}"; shift ;;
        -h|--help)
            sed -n '2,20p' "$0" | grep '^#' | sed 's/^# *//'
            exit 0 ;;
        -*)
            echo "explain: unknown option: $1" >&2; exit 2 ;;
        *)
            QUERY="$1"; shift ;;
    esac
done

if [ ! -d "$RULES_DIR" ]; then
    echo "explain: rules directory not found: $RULES_DIR" >&2
    exit 2
fi

# -- List mode -----------------------------------------------------------------
if [ "$LIST_MODE" = true ]; then
    python3 "$SCRIPT_DIR/_explain_helper.py" list "$RULES_DIR" text
    exit $?
fi

if [ -z "$QUERY" ]; then
    echo "explain: provide a rule ID, keyword, or --list" >&2
    exit 2
fi

# -- Lookup + render -----------------------------------------------------------
python3 "$SCRIPT_DIR/_explain_helper.py" explain "$RULES_DIR" "$QUERY" "$FORMAT"
exit $?
