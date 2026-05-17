#!/usr/bin/env bash
# verify/blueprint-completeness.sh — generic blueprint completeness checker.
#
# Usage:  bash verify/blueprint-completeness.sh <blueprint_name>
#
# Reads:  docs/blueprints/<blueprint_name>/blueprint-manifest.txt
#         (one entry per line, comments start with #)
#
# Manifest line formats:
#   FILE <path>                  — assert path exists and is non-empty
#   FILE_OPTIONAL <path>         — assert path exists (may be empty)
#   DIR <path>                   — assert directory exists and is non-empty
#   CMD <command>                — run command (in repo root), assert exit 0
#   COUNT <path> <min>           — assert wc -l of path is >= min (e.g. min spec items)
#
# Exit:   0 if all manifest entries pass
#         1 if any FILE / DIR / CMD / COUNT entry fails
#         2 if invocation error (missing arg, blueprint not registered, manifest malformed)
#
# The script is INTENTIONALLY agnostic to the blueprint's contents — each
# blueprint declares its own completeness via blueprint-manifest.txt.
#
# Used by:
#   - P7.5 of any blueprint (as a binary "complete" gate)
#   - Ralph PRD acceptance check for "blueprint complete" stories
#   - cold-start agents to assess "is this blueprint done?"
#
# Discovered patterns: Spring Boot blueprints typically declare ~14 entries
# (Spec Trio × 3 + tests + impl + gradle task + rules + snapshots + AGENTS regen
# + docs dir + methodology appendix + sealed L4 + completeness/cold-start scripts).

set -uo pipefail

# ── Argument parsing ─────────────────────────────────────────────────────────

if [ $# -ne 1 ]; then
    echo "Usage: $0 <blueprint_name>" >&2
    echo "Example: $0 payment" >&2
    exit 2
fi

BLUEPRINT="$1"

if [ -z "$BLUEPRINT" ]; then
    echo "ERROR: blueprint name is empty" >&2
    exit 2
fi

# Resolve repo root (script may be run from any subdir).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

MANIFEST_PATH="docs/blueprints/${BLUEPRINT}/blueprint-manifest.txt"

if [ ! -f "$MANIFEST_PATH" ]; then
    echo "ERROR: blueprint '${BLUEPRINT}' not registered (no $MANIFEST_PATH)" >&2
    echo "Hint: each blueprint must declare its artifacts in $MANIFEST_PATH" >&2
    exit 2
fi

# ── Header ───────────────────────────────────────────────────────────────────

echo "blueprint-completeness: ${BLUEPRINT}"
echo "manifest: ${MANIFEST_PATH}"
echo ""

# ── Iterate manifest ─────────────────────────────────────────────────────────

TOTAL=0
FAIL=0
PASS=0
declare -a FAILURES

while IFS= read -r line || [ -n "$line" ]; do
    # Skip blank lines and comments.
    case "$line" in
        ""|\#*) continue ;;
    esac

    TOTAL=$((TOTAL + 1))

    # First token = kind, rest = body.
    kind="${line%% *}"
    body="${line#* }"

    case "$kind" in
        FILE)
            if [ -s "$body" ]; then
                echo "  PASS  FILE  $body"
                PASS=$((PASS + 1))
            else
                if [ -e "$body" ]; then
                    msg="exists but empty"
                else
                    msg="not found"
                fi
                echo "  FAIL  FILE  $body  ($msg)"
                FAILURES+=("FILE $body ($msg)")
                FAIL=$((FAIL + 1))
            fi
            ;;

        FILE_OPTIONAL)
            if [ -e "$body" ]; then
                echo "  PASS  FILE_OPTIONAL  $body"
                PASS=$((PASS + 1))
            else
                echo "  FAIL  FILE_OPTIONAL  $body  (not found)"
                FAILURES+=("FILE_OPTIONAL $body (not found)")
                FAIL=$((FAIL + 1))
            fi
            ;;

        DIR)
            if [ -d "$body" ] && [ -n "$(ls -A "$body" 2>/dev/null || true)" ]; then
                echo "  PASS  DIR   $body"
                PASS=$((PASS + 1))
            else
                if [ -d "$body" ]; then
                    msg="exists but empty"
                else
                    msg="not found"
                fi
                echo "  FAIL  DIR   $body  ($msg)"
                FAILURES+=("DIR $body ($msg)")
                FAIL=$((FAIL + 1))
            fi
            ;;

        CMD)
            # Run command; capture exit code. Run in subshell for isolation.
            if (eval "$body") >/dev/null 2>&1; then
                echo "  PASS  CMD   $body"
                PASS=$((PASS + 1))
            else
                rc=$?
                echo "  FAIL  CMD   $body  (exit $rc)"
                FAILURES+=("CMD $body (exit $rc)")
                FAIL=$((FAIL + 1))
            fi
            ;;

        COUNT)
            # body is: <path> <min>
            path="${body%% *}"
            min="${body##* }"
            if [ ! -f "$path" ]; then
                echo "  FAIL  COUNT $path (min=$min)  (file not found)"
                FAILURES+=("COUNT $path (file not found)")
                FAIL=$((FAIL + 1))
            else
                actual=$(wc -l < "$path" | tr -d ' ')
                if [ "$actual" -ge "$min" ] 2>/dev/null; then
                    echo "  PASS  COUNT $path (≥$min lines, actual=$actual)"
                    PASS=$((PASS + 1))
                else
                    echo "  FAIL  COUNT $path (≥$min lines, actual=$actual)"
                    FAILURES+=("COUNT $path (≥$min, actual=$actual)")
                    FAIL=$((FAIL + 1))
                fi
            fi
            ;;

        *)
            echo "  FAIL  ?     $line  (unknown manifest kind '$kind')"
            FAILURES+=("unknown kind: $line")
            FAIL=$((FAIL + 1))
            ;;
    esac
done < "$MANIFEST_PATH"

# ── Summary ──────────────────────────────────────────────────────────────────

echo ""
echo "blueprint-completeness: ${BLUEPRINT}  —  ${PASS}/${TOTAL} pass, ${FAIL} fail"

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo "Failures:"
    for f in "${FAILURES[@]}"; do
        echo "  - $f"
    done
    exit 1
fi

exit 0
