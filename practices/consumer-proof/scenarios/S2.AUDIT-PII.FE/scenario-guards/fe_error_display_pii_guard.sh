#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S2.AUDIT-PII.FE/scenario-guards/fe_error_display_pii_guard.sh
#
# HAND-ROLLED — capability-gap signal.
# This dogfood cell's GAP: a FE component reads a failed request's JSON
# ProblemDetail body directly (`await res.json(); body.detail || body.message`)
# and renders it, instead of routing it through the catalog's
# templates/L0/fork-receiver-kit/parse-error.ts `parseError()` helper. Even
# though this dogfood ALSO closed a real defect inside parseError() itself
# (its deny-list previously skipped the JSON branch — see
# frontend/tests/parse-error-denylist.vitest.ts), that library fix cannot
# help a consumer who never calls the helper at all. Nothing in the catalog
# stops an AI agent from hand-reading `res.json()` on the failure path — a
# very natural shortcut — which bypasses sanitizeStoredError() entirely.
# Verified absent: `grep -ril "res.json\|parseError" practices/evals/*.sh
# practices-react/eslint-plugin-ax/rules/*.js` returns 0 hits that check a FE
# error-display call site for a parseError()/sanitizeStoredError() wrapper —
# no ESLint ax/* rule and no shell guard targets this seam.
#
# WHAT IT ENFORCES
# For every .tsx file under --root: if the file calls `res.json()` (or reads
# `.json()` off ANY variable) and ALSO reads a ProblemDetail-shaped field
# (`.detail` or `.message`) from the parsed body, that file MUST also call
# `parseError(` somewhere — the catalog's single sanctioned seam for turning
# a failed Response into a safe-to-render Error. Absence of `parseError(`
# means the file is doing its own unsanitized body extraction.
#
# ZERO_SCAN safety: root must contain at least one .tsx file, or this is an
# environment/usage problem (exit 2). NOTE this deliberately does NOT require
# json_read_files > 0 — a correctly-written consumer legitimately delegates
# res.json() to parseError() itself and never touches .json()/.detail in its
# own component source, so "0 direct reads found" is a real, non-vacuous PASS
# for that file, not a sign the guard failed to run.
#
# Exit codes:
#   0 — every res.json()-based error read in the tree is routed through parseError()
#   1 — at least one file reads body.detail/body.message off .json() without
#       ever calling parseError() (signature: FE_ERROR_DISPLAY_PII_UNSANITIZED)
#   2 — usage / environment error (root missing, zero-scan)
#
# Usage: bash fe_error_display_pii_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "fe_error_display_pii_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "fe_error_display_pii_guard: --root DIR is required" >&2
    exit 2
fi
if [ ! -d "$ROOT_OVERRIDE" ]; then
    echo "fe_error_display_pii_guard: no such dir $ROOT_OVERRIDE" >&2
    exit 2
fi

files="$(find "$ROOT_OVERRIDE" -name '*.tsx' 2>/dev/null || true)"
files_scanned=0
json_read_files=0
violations=""

while IFS= read -r f; do
    [ -z "$f" ] && continue
    files_scanned=$((files_scanned + 1))
    if ! grep -qE '\.json\(\)' "$f" 2>/dev/null; then
        continue
    fi
    if ! grep -qE '\.(detail|message)\b' "$f" 2>/dev/null; then
        continue
    fi
    json_read_files=$((json_read_files + 1))
    # Strip // line comments before checking for a real parseError( call —
    # a file merely MENTIONING parseError in prose (e.g. explaining why it
    # should be used) must not be credited as calling it.
    code_only="$(sed -E 's#//.*$##' "$f" 2>/dev/null)"
    if ! printf '%s' "$code_only" | grep -qF 'parseError('; then
        violations="$violations
  $f: reads .detail/.message off res.json() without ever calling parseError()"
    fi
done <<EOF
$files
EOF

if [ "$files_scanned" -eq 0 ]; then
    echo "fe_error_display_pii_guard: ZERO_SCAN — no .tsx file found under $ROOT_OVERRIDE" >&2
    exit 2
fi

echo "fe_error_display_pii_guard: scanned $files_scanned .tsx file(s), $json_read_files reading a ProblemDetail field off .json()"

if [ -n "$violations" ]; then
    echo "VIOLATION: FE error path reads ProblemDetail body without parseError():" >&2
    echo "$violations" | sed 's/^/  /' >&2
    echo "" >&2
    echo "Every FE read of a failed request's JSON body (detail/message) MUST go" >&2
    echo "through templates/L0/fork-receiver-kit/parse-error.ts's parseError() —" >&2
    echo "the only seam that applies the PII deny-list (sanitizeStoredError)." >&2
    echo "fe_error_display_pii_guard: FE_ERROR_DISPLAY_PII_UNSANITIZED — BLOCKED" >&2
    exit 1
fi

echo "fe_error_display_pii_guard: every ProblemDetail body read is routed through parseError()"
exit 0
