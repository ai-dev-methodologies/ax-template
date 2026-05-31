#!/usr/bin/env bash
# practices/evals/no_rrn_in_log_guard.sh
# 2026-06-01 adversarial audit (verification-teeth lens) — mechanizes the CRITICAL
# rule no-rrn-logging.md, which shipped as type:review with a described grep but NO
# guard, AND left an orphan failing fixture (fail_rrn_in_log/) that NO guard caught.
# phi_in_logs_guard.sh does NOT cover it (it keys exclusively on @Phi-tagged getters,
# of which the backend has zero real applications).
#
# THE INVARIANT (개인정보보호법 §24 — RRN is 고유식별정보; it must never reach a log sink):
#   no `log.<level>(...)` statement in production code may reference a raw RRN — the
#   bare token `rrn` (word-bounded) or the Korean `주민`. Hashed / masked forms
#   (rrnHash, rrnMasked, rrnToken, …) are explicitly ALLOWED: the word boundary means
#   `rrn` followed by more identifier chars does not match.
#
# Scope: the Java backend production tree (default backend/src/main). The rule's own
# verification.notes describe exactly this grep; this guard makes it binary and adds the
# word-boundary precision the naive grep lacks (the pass fixture logs rrnHash, which a
# naive `grep -i rrn` would false-positive on).
#
# Exit: 0 no raw RRN in any log statement · 1 at least one violation · 2 usage/setup.
#
# Usage:
#   bash practices/evals/no_rrn_in_log_guard.sh
#   bash practices/evals/no_rrn_in_log_guard.sh --root DIR     # e.g. a fixture dir

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT="$REPO_ROOT/backend/src/main"
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT="$2"; shift 2 ;;
        --root=*) ROOT="${1#--root=}"; shift ;;
        *) echo "no_rrn_in_log_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -d "$ROOT" ] || { echo "no_rrn_in_log_guard: root '$ROOT' not found — nothing to check"; exit 0; }

# A log statement line: log.<level>( ... )  (SLF4J / Logback / Log4j2 conventions).
LOG_RE='log\.(trace|debug|info|warn|error)\('
# Raw RRN token, word-bounded so rrnHash / rrnMasked / rrnToken are NOT flagged.
RRN_RE='(^|[^A-Za-z0-9_])[rR][rR][nN]([^A-Za-z0-9_]|$)'

violations=0
while IFS= read -r f; do
    # Each log-call line in the file; check it for a raw-RRN reference.
    while IFS=: read -r lineno line; do
        [ -z "$lineno" ] && continue
        if printf '%s' "$line" | grep -qE "$RRN_RE" || printf '%s' "$line" | grep -q '주민'; then
            echo "VIOLATION: ${f#$REPO_ROOT/}:$lineno — log statement references a raw RRN (주민등록번호); hash or mask it before logging" >&2
            echo "    $(printf '%s' "$line" | sed -E 's/^[[:space:]]*//')" >&2
            violations=$((violations + 1))
        fi
    done < <(grep -nE "$LOG_RE" "$f" 2>/dev/null)
done < <(find "$ROOT" -name '*.java')

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "no_rrn_in_log_guard: $violations log statement(s) expose a raw RRN — BLOCKED (개인정보보호법 §24)" >&2
    echo "Fix: never log the raw RRN; log a one-way hash (rrnHash) or a masked token instead." >&2
    exit 1
fi

echo "no_rrn_in_log_guard: PASS — no raw RRN (rrn / 주민) in any log statement under ${ROOT#$REPO_ROOT/}"
exit 0
