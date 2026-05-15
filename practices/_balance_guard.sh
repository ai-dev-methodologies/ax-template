#!/usr/bin/env bash
# practices/_balance_guard.sh
#
# Advisory category-balance check.
# Extracts the category prefix from each practices/rules/*.md:
#   1. frontmatter `tags` field — first tag value
#   2. fallback: filename slug-prefix before the first hyphen
#
# Output:
#   stdout — `## Balance` section for inclusion in report.md
#   stderr — WARN lines if any prefix exceeds 25% of total rules
#
# NEVER exits non-zero — advisory only.
# With empty rules/: exits 0 silently.
#
# Compatible with bash 3.2+ (macOS default shell).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RULES_DIR="${SCRIPT_DIR}/rules"

# Extract category prefix for a single rule file.
# Priority: frontmatter tags[0] > filename prefix before first hyphen.
get_prefix() {
    local file="$1"
    local prefix=""

    # Parse YAML frontmatter (between first two --- markers)
    prefix=$(awk '
        BEGIN { in_fm = 0; found_tags = 0 }
        NR == 1 && /^---[[:space:]]*$/ { in_fm = 1; next }
        in_fm && /^---[[:space:]]*$/ { exit }
        in_fm && !found_tags && /^tags:/ {
            found_tags = 1
            line = $0
            # Inline array: tags: [foo, bar]
            if (line ~ /\[/) {
                sub(/^[^[]*\[/, "", line)
                sub(/[],].*/, "", line)
                gsub(/^[[:space:]]+|[[:space:]]+$/, "", line)
                if (line != "") { print line; exit }
            }
            # Inline scalar: tags: foo
            sub(/^tags:[[:space:]]*/, "", line)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", line)
            if (line != "" && line !~ /^-/) { print line; exit }
            next
        }
        in_fm && found_tags && /^[[:space:]]*-/ {
            line = $0
            sub(/^[[:space:]]*-[[:space:]]*/, "", line)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", line)
            if (line != "") { print line; exit }
        }
        in_fm && found_tags && /^[^[:space:]-]/ { exit }
    ' "$file" 2>/dev/null)

    # Fallback: filename slug-prefix before first hyphen
    if [ -z "$prefix" ]; then
        local base
        base=$(basename "$file" .md)
        prefix="${base%%-*}"
    fi

    printf '%s\n' "$prefix"
}

# ── collect prefix list ───────────────────────────────────────────────────────

prefix_list=""
file_count=0

for f in "${RULES_DIR}"/*.md; do
    [ -f "$f" ] || continue   # handles empty glob (no .md files)
    prefix_list="${prefix_list}$(get_prefix "$f")"$'\n'
    file_count=$(( file_count + 1 ))
done

# With no rules: silent success (no output, no WARN)
if [ "$file_count" -eq 0 ]; then
    exit 0
fi

# ── count, emit report section, and warn via awk ─────────────────────────────
# Input is sorted so awk keys array preserves insertion (sorted) order.

printf '%s' "$prefix_list" | sort | awk -v total="$file_count" '
    /^[[:space:]]*$/ { next }
    {
        prefix = $0
        counts[prefix]++
        if (!(prefix in seen)) {
            keys[++nkeys] = prefix
            seen[prefix] = 1
        }
    }
    END {
        print "## Balance"
        print ""
        print "| Category prefix | Count | % of total |"
        print "|---|---|---|"

        for (i = 1; i <= nkeys; i++) {
            k = keys[i]
            n = counts[k]
            pct = int(n * 100 / total)
            printf "| `%s` | %d | %d%% |\n", k, n, pct
        }

        print ""
        printf "_Total: %d rule(s)_\n", total

        # Advisory warnings to stderr (never gates merge)
        for (i = 1; i <= nkeys; i++) {
            k = keys[i]
            n = counts[k]
            pct = int(n * 100 / total)
            if (pct > 25) {
                printf "WARN: balance — %s has %d/%d (%d%%) > 25%%\n", \
                    k, n, total, pct > "/dev/stderr"
            }
        }
    }
'

exit 0
