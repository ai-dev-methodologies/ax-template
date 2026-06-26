#!/usr/bin/env bash
# practices/evals/quick_verify_no_audit_guard.sh — guards the ITERATION-ONLY quick-verify boundary.
#
# THE INVARIANT (binary): verify/quick-verify.sh is a fast dev-loop convenience, NOT the R25
# completion gate. It MUST NOT be capable of producing the artifact the pre-push recency guard
# requires — so it MUST NOT write the .ax-verify/runs.jsonl audit log, and MUST NOT invoke
# verify-completion.sh (the audit writer). This makes the "full R25 is the sole gate / no --skip"
# discipline enforced BY CONSTRUCTION at the push boundary, not by trusting a banner. It MUST also
# print the ITERATION-ONLY banner. Asserts:
#   1. verify/quick-verify.sh exists;
#   2. it does NOT write runs.jsonl  (no `>>`/`>` redirect into ...runs.jsonl);
#   3. it does NOT invoke verify-completion.sh (which writes the audit);
#   4. it prints an ITERATION-ONLY / NOT-the-gate banner.
#
# Usage:
#   bash practices/evals/quick_verify_no_audit_guard.sh
#   bash practices/evals/quick_verify_no_audit_guard.sh --root DIR   # fixture mode (DIR/quick-verify.sh)
# Exit 0 = boundary intact. Exit 1 = quick-verify could be mistaken for the gate (BLOCK).

set -u

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        *) echo "quick_verify_no_audit_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ -n "$ROOT_OVERRIDE" ]; then
    SCRIPT="$ROOT_OVERRIDE/quick-verify.sh"
else
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
    SCRIPT="$REPO_ROOT/verify/quick-verify.sh"
fi

[ -f "$SCRIPT" ] || { echo "quick_verify_no_audit_guard: FAIL — $SCRIPT not found" >&2; exit 1; }

FAIL=0

# 2. no write to runs.jsonl (a redirect into the audit log)
if grep -nE '>>?[[:space:]]*[^|]*runs\.jsonl' "$SCRIPT" >/dev/null 2>&1; then
    echo "quick_verify_no_audit_guard: FAIL — quick-verify writes the audit log (runs.jsonl):" >&2
    grep -nE '>>?[[:space:]]*[^|]*runs\.jsonl' "$SCRIPT" | sed 's/^/  /' >&2
    FAIL=1
fi

# 3. no INVOCATION of verify-completion.sh (the audit writer). Mentions in the banner/comments
#    that TELL the user to run R25 are allowed — only an actual command invocation is forbidden
#    (a non-comment, non-echo line that runs it: `bash .../verify-completion.sh`, `./...`, `source ...`).
INVOKE="$(grep -nE 'verify-completion\.sh' "$SCRIPT" \
    | grep -vE '^[0-9]+:[[:space:]]*#' \
    | grep -vE '\becho\b' \
    | grep -E '(bash|sh|source|\./|exec)[^#]*verify-completion\.sh' || true)"
if [ -n "$INVOKE" ]; then
    echo "quick_verify_no_audit_guard: FAIL — quick-verify INVOKES verify-completion.sh (the audit writer); it must stay a strict subset that does not write the audit:" >&2
    echo "$INVOKE" | sed 's/^/  /' >&2
    FAIL=1
fi

# 4. prints the ITERATION-ONLY / NOT-the-gate banner
if ! grep -qiE 'ITERATION-ONLY|NOT the R25 completion gate|not the .* gate' "$SCRIPT"; then
    echo "quick_verify_no_audit_guard: FAIL — quick-verify must print an ITERATION-ONLY / NOT-the-gate banner so it is never mistaken for R25" >&2
    FAIL=1
fi

if [ "$FAIL" -ne 0 ]; then
    echo "" >&2
    echo "  verify/quick-verify.sh is iteration-only. Keep it from writing .ax-verify/runs.jsonl and from" >&2
    echo "  invoking verify-completion.sh, and keep its ITERATION-ONLY banner — so the full R25 stays the" >&2
    echo "  sole completion gate (the pre-push recency guard blocks any push behind quick-verify)." >&2
    exit 1
fi

echo "quick_verify_no_audit_guard: PASS — quick-verify is iteration-only (no audit write, banner present)"
exit 0
