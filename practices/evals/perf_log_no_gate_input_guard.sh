#!/usr/bin/env bash
# practices/evals/perf_log_no_gate_input_guard.sh — guards the "perf.jsonl is observability
# only" invariant (D-11 / BACKLOG:420; see docs/VERIFICATION-PERF-AND-SHARDING.md §4 and the
# D-11 comment block in practices/scripts/verify-completion.sh where the sidecar is written).
#
# THE INVARIANT (binary): .ax-verify/perf.jsonl is a NEW, independent time-series sidecar
# written by verify-completion.sh so verification cost can be OBSERVED rather than guessed. It
# is deliberately NOT runs.jsonl (whose schema is pinned byte-for-byte by
# completion_checklist_recency_guard.sh and re-checked by a PRIOR RELEASE's copy of that guard
# at push time — see the "THIS printf IS THE AUDIT SCHEMA" comment in verify-completion.sh). It
# MUST NEVER become a gate INPUT the way runs.jsonl is: nothing under practices/evals/*.sh,
# practices/scripts/*.sh or .githooks/* may READ it. This mirrors quick_verify_no_audit_guard.sh
# in shape — that guard keeps a fast dev-loop convenience from masquerading as the R25 gate;
# this one keeps an observability-only sidecar from quietly becoming a second one.
#
# BOUNDARY, stated once (same class of limit as NOOP_PLACEHOLDER_EXIT in verify-completion.sh
# and the RRN word-boundary note in no_rrn_in_log_guard.sh): this is a grep-based heuristic over
# a FINITE set of read-shaped constructs — input redirection (`< … perf.jsonl`), CLI read tools
# piped to/from it (cat/head/tail/jq/less/more), and the common Python read idioms
# (`open(…perf.jsonl…)`, `…perf.jsonl….read_text(`, `json.load(…perf.jsonl…)`). It is not a full
# static analyzer and cannot see a path built by runtime string concatenation or an indirect
# variable. What it buys is narrow and real: perf.jsonl cannot be wired into a gate using any of
# the common, direct idioms without this guard BLOCKing the commit that adds it.
#
# NOT REGISTERED in run-all-guards.sh (deliberately, per this task's scope) — see the report.
#
# Usage:
#   bash practices/evals/perf_log_no_gate_input_guard.sh
#   bash practices/evals/perf_log_no_gate_input_guard.sh --root DIR   # fixture mode: DIR mirrors
#                                                                     # REPO_ROOT (DIR/practices/…,
#                                                                     # DIR/.githooks/…)
# Exit 0 = no read reference found anywhere in scope. Exit 1 = perf.jsonl is read somewhere it
# must not be (BLOCK). Exit 2 = usage error.

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ROOT="$DEFAULT_ROOT"

while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT="$2"; shift 2 ;;
        --root=*) ROOT="${1#--root=}"; shift ;;
        *) echo "perf_log_no_gate_input_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# This guard's own source necessarily mentions "perf.jsonl" (in this comment block and in the
# READ_RE definitions below) and would otherwise flag itself when scanning practices/evals/*.sh
# against the live tree. Excluded by basename, exactly the way a fixture directory's copy of it
# (if one is ever added) would also need to be excluded — a self-referential false positive is
# not evidence of a real violation.
SELF="$(basename "${BASH_SOURCE[0]}")"

# Read-shaped constructs mentioning perf.jsonl on the same line. Built as alternation over ERE
# fragments so each fragment stays readable; see the BOUNDARY note above for what this does and
# does not claim to catch.
READ_RE='(<[[:space:]]*[^|]*perf\.jsonl)'
READ_RE="$READ_RE"'|(\b(cat|head|tail|jq|less|more)\b[^#]*perf\.jsonl)'
READ_RE="$READ_RE"'|(perf\.jsonl[^#]*\|[[:space:]]*(cat|head|tail|jq|python3?|grep))'
READ_RE="$READ_RE"'|(open\([^)]*perf\.jsonl)'
READ_RE="$READ_RE"'|(perf\.jsonl[^#]*\.read_text\()'
READ_RE="$READ_RE"'|(json\.load\([^)]*perf\.jsonl)'

FAIL=0
HITS=""

# scan_dir <dir> <glob> — grep every top-level file matching <glob> directly under <dir> (no
# recursion — matches the task's scope: practices/evals/*.sh, practices/scripts/*.sh, .githooks/*)
# for READ_RE, skipping this guard's own file.
scan_dir() {
    local dir="$1" glob="$2" f base m
    [ -d "$dir" ] || return 0
    for f in "$dir"/$glob; do
        [ -f "$f" ] || continue
        base="$(basename "$f")"
        [ "$base" = "$SELF" ] && continue
        m="$(grep -nE "$READ_RE" "$f" 2>/dev/null || true)"
        if [ -n "$m" ]; then
            FAIL=1
            HITS="${HITS}${f}:
$(printf '%s\n' "$m" | sed 's/^/  /')
"
        fi
    done
}

scan_dir "$ROOT/practices/evals" "*.sh"
scan_dir "$ROOT/practices/scripts" "*.sh"
scan_dir "$ROOT/.githooks" "*"

if [ "$FAIL" -ne 0 ]; then
    echo "perf_log_no_gate_input_guard: FAIL — perf.jsonl (the D-11 observability sidecar) is" >&2
    echo "  READ by code that must never depend on it as a gate input:" >&2
    printf '%s' "$HITS" | sed 's/^/  /' >&2
    echo "" >&2
    echo "  perf.jsonl is observability only (docs/VERIFICATION-PERF-AND-SHARDING.md §4) — no" >&2
    echo "  gate, hook or guard may branch on its content, the way runs.jsonl legitimately is" >&2
    echo "  read by completion_checklist_recency_guard.sh. Remove the read." >&2
    exit 1
fi

echo "perf_log_no_gate_input_guard: PASS — perf.jsonl has 0 read references under practices/evals, practices/scripts, .githooks"
exit 0
