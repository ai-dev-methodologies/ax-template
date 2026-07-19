#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S2.AUTHZ.FE/scenario-guards/fe_admin_action_missing_role_gate_guard.sh
#
# HAND-ROLLED — capability-gap signal.
# coverage-map.yaml's S2.AUTHZ.FE row (status: partial) already documents this
# gap: rule docs (impersonation-banner-required-when-acting-as-other-user.md,
# no-impersonation-bypass-via-helper-rename.md, audit-log-frontend-viewer-rbac
# -virtualized.md) and a real L0 primitive (use-caller-id.ts) exist, but NONE
# of the 14 ax/* ESLint rules (checked: practices-react/eslint-plugin-ax/
# rules/*.js) mechanically enforce "an admin-only action must gate ITSELF on
# useCallerRole() before rendering" — the existing catalog gate pattern
# (templates/L4/webhook .../webhooks/page.tsx: `if (role !== 'admin') return
# <EmptyState/>`) is applied only at the PAGE level, by convention, with
# nothing that would catch a smaller admin-only ACTION component shipped
# without its own gate (OWASP API5:2023 BFLA: authorize the function, not
# just the page that happens to embed it today).
#
# WHAT IT ENFORCES
# For every .tsx file under --root: if the file contains the marker comment
# `ax:admin-action` (a convention this scenario introduces to flag a JSX
# region that performs a privileged action), the file MUST ALSO:
#   1. import useCallerRole from templates/L0/fork-receiver-kit/use-caller-id
#   2. contain a role-comparison guard (`role !== 'admin'` or
#      `role === 'admin'`) on a line number STRICTLY BEFORE the marker's line
#      — i.e. the check must actually precede (gate) the marked region in
#      source order, not merely exist somewhere later/unrelated in the file.
# Absence of either means the admin-only action is NOT self-gated.
#
# ZERO_SCAN safety: root must contain at least one .tsx file, or this is an
# environment/usage problem (exit 2). A .tsx tree with NO `ax:admin-action`
# marker anywhere is a real, non-vacuous PASS (nothing privileged to gate).
#
# Exit codes:
#   0 — every ax:admin-action marker in the tree is preceded by a role gate
#   1 — at least one marked admin action lacks a preceding role gate
#       (signature: FE_ADMIN_ACTION_MISSING_ROLE_GATE)
#   2 — usage / environment error (root missing, zero-scan)
#
# Usage: bash fe_admin_action_missing_role_gate_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "fe_admin_action_missing_role_gate_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "fe_admin_action_missing_role_gate_guard: --root DIR is required" >&2
    exit 2
fi
if [ ! -d "$ROOT_OVERRIDE" ]; then
    echo "fe_admin_action_missing_role_gate_guard: no such dir $ROOT_OVERRIDE" >&2
    exit 2
fi

# Strip `//` line comments AND `/* ... */` block comments (e.g. a leading
# file-header JSDoc block) before searching for the role-gate CODE — a
# prose sentence describing the pattern ("if (role !== 'admin') return...")
# inside a header comment must never be credited as a real gate. Blanks
# comment characters in place so line numbers stay aligned with the
# original file (required for the marker-precedes-gate ordering check).
strip_comments() {
    awk '
    {
        line = $0
        n = length(line)
        out = ""
        i = 1
        while (i <= n) {
            c2 = substr(line, i, 2)
            if (!incm) {
                if (c2 == "/*") { incm = 1; out = out "  "; i += 2; continue }
                if (c2 == "//") {
                    rest_len = n - i + 1
                    out = out sprintf("%*s", rest_len, "")
                    i = n + 1
                    continue
                }
                out = out substr(line, i, 1)
                i += 1
            } else {
                if (c2 == "*/") { incm = 0; out = out "  "; i += 2; continue }
                out = out " "
                i += 1
            }
        }
        print out
    }' "$1" 2>/dev/null
}

files="$(find "$ROOT_OVERRIDE" -name '*.tsx' 2>/dev/null || true)"
files_scanned=0
markers_found=0
violations=""

while IFS= read -r f; do
    [ -z "$f" ] && continue
    files_scanned=$((files_scanned + 1))

    marker_line="$(grep -n 'ax:admin-action' "$f" 2>/dev/null | head -1 | cut -d: -f1)"
    [ -z "$marker_line" ] && continue
    markers_found=$((markers_found + 1))

    code_only="$(strip_comments "$f")"

    has_import=0
    if printf '%s\n' "$code_only" | grep -qE "import[^;]*useCallerRole[^;]*from ['\"]templates/L0/fork-receiver-kit/use-caller-id['\"]"; then
        has_import=1
    fi

    gate_before=0
    gate_line="$(printf '%s\n' "$code_only" | grep -nE "role[[:space:]]*(===|!==)[[:space:]]*['\"]admin['\"]" 2>/dev/null | head -1 | cut -d: -f1)"
    if [ -n "$gate_line" ] && [ "$gate_line" -lt "$marker_line" ]; then
        gate_before=1
    fi

    if [ "$has_import" -eq 0 ] || [ "$gate_before" -eq 0 ]; then
        reason="missing useCallerRole import"
        if [ "$has_import" -eq 1 ]; then
            reason="useCallerRole imported but no role check precedes the marker (line $marker_line)"
        fi
        violations="$violations
  $f:$marker_line: admin action not self-gated — $reason"
    fi
done <<EOF
$files
EOF

if [ "$files_scanned" -eq 0 ]; then
    echo "fe_admin_action_missing_role_gate_guard: ZERO_SCAN — no .tsx file found under $ROOT_OVERRIDE" >&2
    exit 2
fi

echo "fe_admin_action_missing_role_gate_guard: scanned $files_scanned .tsx file(s), $markers_found ax:admin-action marker(s)"

if [ -n "$violations" ]; then
    echo "VIOLATION: admin-only action renders without its own caller-role gate:" >&2
    echo "$violations" | sed 's/^/  /' >&2
    echo "" >&2
    echo "Every JSX region marked ax:admin-action MUST be preceded (in source" >&2
    echo "order, in the SAME component) by useCallerRole() AND a role !== 'admin'" >&2
    echo "/ role === 'admin' check — the component must gate itself, not rely on" >&2
    echo "an enclosing page's gate (OWASP API5:2023 BFLA)." >&2
    echo "fe_admin_action_missing_role_gate_guard: FE_ADMIN_ACTION_MISSING_ROLE_GATE — BLOCKED" >&2
    exit 1
fi

echo "fe_admin_action_missing_role_gate_guard: every ax:admin-action marker is preceded by a role gate"
exit 0
