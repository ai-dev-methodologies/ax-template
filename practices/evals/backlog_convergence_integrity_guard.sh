#!/usr/bin/env bash
# practices/evals/backlog_convergence_integrity_guard.sh
# iteration-7 mechanical integrity guard for north-star #2 (BACKLOG convergence).
#
# North-star #2 (CLAUDE.md Project Vision) redefines the project's end-point as
# the P0–P3 convergence rate measured by docs/BACKLOG.md's "현재 수렴률" table.
# That metric was, until now, WHOLLY self-asserted: no guard read BACKLOG.md, so
# the per-tier 전체/closed counts, the 합계 (수렴 분모) denominator, and the
# aggregate 수렴률 could all silently rot or mis-sum the moment a maintainer
# closed an item and hand-edited the table cells. A convergence metric that lies
# about its own denominator undermines the "신규 산업 dogfood는 수렴률 ≥ 70%
# 전까지 동결" freeze rule — the freeze decision rides on a number nothing checks.
#
# Modeled on doc_headline_count_guard.sh: derive the disk-truth counts from the
# checkbox items themselves, then assert the table cells match. Counts are
# COMPUTED (never hardcoded), so a legitimate close just requires bumping the
# table cell the guard names.
#
# Disk-truth derivation (the checkbox body is the source of truth):
#   - Walk each "## P0/P1/P2/P3" section; under it, every "- [x]"/"- [ ]" line is
#     a backlog entry. A single line may name a RANGE or a LIST of item IDs:
#       "**P0-1 ~ P0-11**"                 → 11 IDs   (range expansion)
#       "P1-14~17 + P1-19"                 → 5 IDs    (range + extra)
#       "P1-39 IDW9 / P1-40 / ... / P1-45" → 7 IDs    (slash list)
#       "P3-1 ~ P3-8"                      → 8 IDs    (grouped range)
#     Lettered sub-items (e.g. P2-1a / P2-1b, marked "*분모 불변*") are NOT
#     counted — they are residual annotations of an already-counted parent and
#     the table explicitly excludes them from the denominator.
#   - A line's [x] vs [ ] decides closed vs open for ALL IDs it names.
#   - Tier total = closed + open IDs counted under that "## P<n>" section.
#
# Asserts:
#   (1) each tier row's 전체 (total) and closed cell == its checkbox-derived count
#   (2) the 합계 row's 전체 == sum of the four tier totals (blocks silent
#       denominator shrink or a dropped tier) and its closed == sum of closeds
#   (3) the 합계 row's aggregate 수렴률 == round(closed / total * 100)
#
# Exit: 0 PASS · 1 a table cell disagrees with disk truth · 2 usage/setup error.
#
# Usage:
#   bash practices/evals/backlog_convergence_integrity_guard.sh
#   bash practices/evals/backlog_convergence_integrity_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "backlog_convergence_integrity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "backlog_convergence_integrity_guard: cannot cd $REPO_ROOT" >&2; exit 2; }

BACKLOG="docs/BACKLOG.md"
if [ ! -f "$BACKLOG" ]; then
    echo "backlog_convergence_integrity_guard: $BACKLOG missing under $REPO_ROOT" >&2
    exit 2
fi

violations=0
fail() { echo "VIOLATION: $1" >&2; violations=$((violations + 1)); }

# ── disk-truth: count item IDs per tier (closed / open) from the checkboxes ─────
# awk emits four lines: "P<n> <closed> <open>" for n in 0..3 (only tiers seen).
# Range/list expansion happens here; lettered sub-items are skipped.
COUNTS="$(awk '
    # On a "## P<digit>" heading, switch the active tier; clear on any other "##".
    /^## P[0-3]([^0-9]|$)/ {
        match($0, /P[0-3]/); tier = substr($0, RSTART, RLENGTH); active = 1; next
    }
    /^## / { active = 0; next }
    active != 1 { next }
    /^- \[[ x]\]/ {
        is_closed = ($0 ~ /^- \[x\]/) ? 1 : 0
        line = $0
        # Strip markdown bold/emphasis so "**P0-1 ~ P0-11**" tokenizes cleanly.
        gsub(/[*`]/, " ", line)
        # A lettered LEADING id (P2-1a / P2-1b) is a residual sub-annotation of an
        # already-counted parent — the table marks it "분모 불변". Skip the whole
        # line so it contributes 0 IDs to the denominator.
        probe = line; sub(/^[- \t\[\]x]*/, "", probe)
        if (probe ~ /^P[0-3]-[0-9]+[a-z]/) next
        # Drop any "(...)" parentheticals so a back-reference inside them
        # (e.g. "(P2-1 잔여, 분모 불변)") is never miscounted as a fresh id.
        gsub(/\([^)]*\)/, " ", line)
        n = 0
        # Find every "<tier>-<num>" occurrence; reject a trailing letter (sub-item).
        while (match(line, /P[0-3]-[0-9]+[a-z]?/)) {
            tok = substr(line, RSTART, RLENGTH)
            rest = substr(line, RSTART + RLENGTH)
            line = rest
            # Skip any other lettered sub-items defensively (denominator-excluded).
            if (tok ~ /[a-z]$/) continue
            # Parse this tokens start number.
            split(tok, p, "-"); lo = p[2] + 0; hi = lo
            # A range? "P0-1 ~ P0-11" / "P1-14~17": next token (or bare number)
            # after a ~ extends to hi. Look at the immediate lead of rest.
            r = rest
            sub(/^[ \t]*/, "", r)
            if (r ~ /^~/) {
                sub(/^~[ \t]*/, "", r)
                if (match(r, /^P[0-3]-[0-9]+/)) {
                    seg = substr(r, RSTART, RLENGTH); split(seg, q, "-"); hi = q[2] + 0
                    line = substr(r, RSTART + RLENGTH)
                } else if (match(r, /^[0-9]+/)) {
                    hi = substr(r, RSTART, RLENGTH) + 0
                    line = substr(r, RSTART + RLENGTH)
                }
            }
            if (hi < lo) hi = lo
            n += (hi - lo + 1)
        }
        if (is_closed) closed[tier] += n; else open[tier] += n
        seen[tier] = 1
        next
    }
    END {
        split("P0 P1 P2 P3", T, " ")
        for (i = 1; i <= 4; i++) {
            t = T[i]
            if (seen[t]) printf "%s %d %d\n", t, closed[t] + 0, open[t] + 0
        }
    }
' "$BACKLOG")"

# ── parse the "현재 수렴률" table cells ────────────────────────────────────────
# Each tier row: "| P0 (...) | <total> | <closed> | <pct> |". The 합계 row is the
# one whose first cell contains "합계". Cells may be wrapped in ** **.
strip_cell() { printf '%s' "$1" | sed 's/[*` ~]//g'; }

# Find the first PIPE-TABLE row (line starts with "|") whose label cell matches
# $1. Anchoring to "^|" prevents a prose sentence that happens to mention "합계"
# / "P0" from being mistaken for the table row.
find_row() { grep -E "^\| *$1" "$BACKLOG" | head -1 || true; }

table_total() {
    local row; row="$(find_row "$1")"
    [ -z "$row" ] && { echo ""; return; }
    strip_cell "$(printf '%s' "$row" | awk -F'|' '{print $3}')"
}
table_closed() {
    local row; row="$(find_row "$1")"
    [ -z "$row" ] && { echo ""; return; }
    strip_cell "$(printf '%s' "$row" | awk -F'|' '{print $4}')"
}
table_pct() {
    local row; row="$(find_row "$1")"
    [ -z "$row" ] && { echo ""; return; }
    # extract the leading integer of the 수렴률 cell (drops ~, %, ** **)
    printf '%s' "$row" | awk -F'|' '{print $5}' | grep -oE '[0-9]+' | head -1
}

# ── (1) per-tier: 전체 / closed cells == checkbox-derived counts ───────────────
declare_sum_total=0
declare_sum_closed=0
disk_sum_total=0
disk_sum_closed=0

# tier row label patterns — matched after the leading "| " by find_row.
pat_P0='P0 '
pat_P1='P1 '
pat_P2='P2 '
pat_P3='P3 '

check_tier() {
    local tier="$1" pat="$2" disk_closed="$3" disk_open="$4"
    local disk_total=$((disk_closed + disk_open))
    disk_sum_total=$((disk_sum_total + disk_total))
    disk_sum_closed=$((disk_sum_closed + disk_closed))

    local t_total t_closed
    t_total="$(table_total "$pat")"
    t_closed="$(table_closed "$pat")"

    if [ -z "$t_total" ] || [ -z "$t_closed" ]; then
        fail "$BACKLOG 수렴률 table: ${tier} row not found (pattern ${pat})"
        return
    fi
    if [ "$t_total" != "$disk_total" ]; then
        fail "$BACKLOG ${tier} 전체: table says '${t_total}' but checkbox count is '${disk_total}'"
    fi
    if [ "$t_closed" != "$disk_closed" ]; then
        fail "$BACKLOG ${tier} closed: table says '${t_closed}' but checkbox count is '${disk_closed}'"
    fi
}

# pull disk counts into per-tier vars (bash 3.2 — no associative arrays)
P0_CLOSED=0; P0_OPEN=0; P1_CLOSED=0; P1_OPEN=0
P2_CLOSED=0; P2_OPEN=0; P3_CLOSED=0; P3_OPEN=0
while read -r tier c o; do
    [ -z "$tier" ] && continue
    case "$tier" in
        P0) P0_CLOSED=$c; P0_OPEN=$o ;;
        P1) P1_CLOSED=$c; P1_OPEN=$o ;;
        P2) P2_CLOSED=$c; P2_OPEN=$o ;;
        P3) P3_CLOSED=$c; P3_OPEN=$o ;;
    esac
done <<EOF
$COUNTS
EOF

check_tier "P0" "$pat_P0" "$P0_CLOSED" "$P0_OPEN"
check_tier "P1" "$pat_P1" "$P1_CLOSED" "$P1_OPEN"
check_tier "P2" "$pat_P2" "$P2_CLOSED" "$P2_OPEN"
check_tier "P3" "$pat_P3" "$P3_CLOSED" "$P3_OPEN"

# ── (2) 합계 row 전체 == sum of tier totals; closed == sum of tier closeds ─────
# label cell is "| **P0–P3 합계 (수렴 분모)** |" — 합계 is not flush to the pipe.
SUM_PAT='.*합계'
sum_total="$(table_total "$SUM_PAT")"
sum_closed="$(table_closed "$SUM_PAT")"
if [ -z "$sum_total" ] || [ -z "$sum_closed" ]; then
    fail "$BACKLOG 수렴률 table: 합계 (수렴 분모) row not found"
else
    if [ "$sum_total" != "$disk_sum_total" ]; then
        fail "$BACKLOG 합계 전체: table says '${sum_total}' but sum of tier totals is '${disk_sum_total}' (denominator drift)"
    fi
    if [ "$sum_closed" != "$disk_sum_closed" ]; then
        fail "$BACKLOG 합계 closed: table says '${sum_closed}' but sum of tier closeds is '${disk_sum_closed}'"
    fi

    # ── (3) aggregate 수렴률 == round(closed / total * 100) ──────────────────
    sum_pct="$(table_pct "$SUM_PAT")"
    if [ -z "$sum_pct" ]; then
        fail "$BACKLOG 합계 수렴률: no integer percentage found in the cell"
    elif [ "$disk_sum_total" -gt 0 ]; then
        # round half up: (closed*200 + total) / (2*total)
        expected_pct=$(( (disk_sum_closed * 200 + disk_sum_total) / (2 * disk_sum_total) ))
        if [ "$sum_pct" != "$expected_pct" ]; then
            fail "$BACKLOG 합계 수렴률: table says '${sum_pct}%' but round(${disk_sum_closed}/${disk_sum_total}*100) = '${expected_pct}%'"
        fi
    fi
fi

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "backlog_convergence_integrity_guard: $violations table cell(s) disagree with checkbox truth — BLOCKED" >&2
    echo "disk truth: P0 ${P0_CLOSED}/$((P0_CLOSED+P0_OPEN)) · P1 ${P1_CLOSED}/$((P1_CLOSED+P1_OPEN)) · P2 ${P2_CLOSED}/$((P2_CLOSED+P2_OPEN)) · P3 ${P3_CLOSED}/$((P3_CLOSED+P3_OPEN)) · 합계 ${disk_sum_closed}/${disk_sum_total}" >&2
    exit 1
fi

echo "backlog_convergence_integrity_guard: BACKLOG.md 수렴률 table matches checkbox truth (합계 ${disk_sum_closed}/${disk_sum_total})"
exit 0
