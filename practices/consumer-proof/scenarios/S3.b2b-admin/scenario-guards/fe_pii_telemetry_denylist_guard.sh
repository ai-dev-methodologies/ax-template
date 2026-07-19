#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S3.b2b-admin/scenario-guards/fe_pii_telemetry_denylist_guard.sh
#
# HAND-ROLLED — capability-gap signal (confirmed absent, not just "not found
# by us"). The dogfood brief for this cell adds a requirement: "an enforced
# FE deny-list preventing PII fields from being placed into client-side
# analytics/telemetry event payloads." Checked for a reusable catalog asset:
#   - `grep -ril "analytic\|telemetry\|pii" practices/evals/*.sh` → 0 hits
#     that scan a *.tsx analytics-call payload (phi_in_logs_guard.sh checks
#     SERVER-side log statements, a different surface).
#   - `ls templates/L2/blocks | grep -iE 'analytic|telemetry|track'` → only
#     event-stream.tsx, which is an inbound SSE/polling feed component, not
#     an outbound analytics-call payload guard.
#   - no @ax/eslint-plugin-ax rule id for this shape (react/eslint.config.mjs
#     + practices-react/rules/*.md have no telemetry/analytics-denylist rule).
# So this is hand-rolled here, isolated to this scenario dir, in the same
# grep-based deliberately-simple style as the sibling S3.e-commerce scenario's
# locale_format_guard.sh (a real fix upstream would be a proper AST-shape
# ESLint rule — out of scope for a scenario probe).
#
# WHAT IT ENFORCES
# For every *.tsx file under --root, find each outbound analytics/telemetry
# call site:  <ident>.(track|capture|logEvent)(  where <ident> is one of
# analytics / telemetry / posthog / amplitude / gtag (case-insensitive).
# Within that call's argument window (up to the next line that is just a
# closing paren, or 25 lines — whichever comes first), FORBID any object-
# literal key from the PII deny-list:
#   email, phone, ssn, rrn, address, name, fullName, userName, dob,
#   birthDate, ip, ipAddress
# (matched as `<key>:` — word-boundaried, case-insensitive).
#
# Exit codes: 0 — no forbidden key in any telemetry call payload · 1 —
# violation (signature: PII_IN_TELEMETRY_PAYLOAD) · 2 — usage/env error.
#
# Usage: bash fe_pii_telemetry_denylist_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "fe_pii_telemetry_denylist_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "fe_pii_telemetry_denylist_guard: --root DIR is required" >&2
    exit 2
fi
if [ ! -d "$ROOT_OVERRIDE" ]; then
    echo "fe_pii_telemetry_denylist_guard: no such dir $ROOT_OVERRIDE — SKIP"
    exit 0
fi

files="$(find "$ROOT_OVERRIDE" -name '*.tsx' 2>/dev/null || true)"
count=0
if [ -n "$files" ]; then
    count="$(printf '%s\n' "$files" | grep -c . || true)"
fi
if [ "$count" -eq 0 ]; then
    echo "fe_pii_telemetry_denylist_guard: 0 *.tsx files under $ROOT_OVERRIDE — nothing to check"
    exit 0
fi
echo "fe_pii_telemetry_denylist_guard: scanned $count *.tsx file(s)"

CALL_RE='(analytics|telemetry|posthog|amplitude|gtag)\.(track|capture|logEvent)\('
DENYLIST_RE='(email|phone|ssn|rrn|address|fullName|userName|name|dob|birthDate|ipAddress|ip)[[:space:]]*:'

call_site_count=0
violations=""

while IFS= read -r f; do
    [ -z "$f" ] && continue
    total_lines="$(wc -l < "$f" | tr -d ' ')"
    hits="$(grep -niE "$CALL_RE" "$f" 2>/dev/null || true)"
    [ -z "$hits" ] && continue
    while IFS= read -r hitline; do
        [ -z "$hitline" ] && continue
        start="${hitline%%:*}"
        call_site_count=$((call_site_count + 1))
        end=$((start + 25))
        if [ "$end" -gt "$total_lines" ]; then
            end="$total_lines"
        fi
        # Trim the window at the first bare closing-paren line after start.
        window="$(sed -n "${start},${end}p" "$f")"
        trimmed=""
        stop=0
        while IFS= read -r wline; do
            if [ "$stop" -eq 1 ]; then
                break
            fi
            trimmed="$trimmed
$wline"
            if printf '%s' "$wline" | grep -qE '^[[:space:]]*\)[;]?[[:space:]]*$'; then
                stop=1
            fi
        done <<EOF
$window
EOF
        pii_hits="$(printf '%s' "$trimmed" | grep -niE "$DENYLIST_RE" || true)"
        if [ -n "$pii_hits" ]; then
            while IFS= read -r ph; do
                [ -z "$ph" ] && continue
                violations="$violations
  $f:$start: telemetry payload includes denylisted key — $ph"
            done <<EOF
$pii_hits
EOF
        fi
    done <<EOF
$hits
EOF
done <<EOF
$files
EOF

if [ "$call_site_count" -eq 0 ]; then
    echo "fe_pii_telemetry_denylist_guard: 0 analytics/telemetry call sites found — nothing to check"
    exit 0
fi

if [ -n "$violations" ]; then
    echo "VIOLATION: PII field(s) placed into a client-side analytics/telemetry payload:" >&2
    echo "$violations" | sed 's/^/  /' >&2
    echo "" >&2
    echo "Only non-PII identifiers (a hash, an opaque entity ref, an enum status) may" >&2
    echo "reach an outbound analytics/telemetry call. Strip or hash the field before" >&2
    echo "the .track()/.capture()/.logEvent() call." >&2
    echo "fe_pii_telemetry_denylist_guard: PII_IN_TELEMETRY_PAYLOAD — BLOCKED" >&2
    exit 1
fi

echo "fe_pii_telemetry_denylist_guard: no PII field found in any telemetry call payload ($call_site_count call site(s))"
exit 0
