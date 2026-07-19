#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S2.AUDIT-PII.XB/scenario-guards/webhook_signature_status_ux_guard.sh
#
# HAND-ROLLED — capability-gap signal (confirmed absent, not just "not found
# by a quick grep"). The dogfood brief's ADDITIONAL REQUIREMENT: a FE
# component/hook that surfaces webhook-signature verification status to the
# user (a "signature could not be verified" UX), mirroring BE WEBHOOK-SIGN
# (specs/webhook-signing-l0.yaml, WebhookSigningException.Kind:
# WEBHOOK_SIGNATURE_MALFORMED / WEBHOOK_TIMESTAMP_STALE /
# WEBHOOK_SIGNATURE_INVALID / WEBHOOK_EVENT_REPLAYED). Checked for a reusable
# catalog asset:
#   - `ls templates/L4/webhook` → app/(admin)/webhooks pages exist, but list
#     deliveries without a distinct signature-verification-failed state.
#   - `grep -ril "signatureStatus\|could not be verified" templates/L2/blocks
#     practices-react/rules` → 0 hits — no L2 block, no ax/* ESLint rule id
#     for this shape.
#   - `templates/L2/blocks/status-badge.tsx` is the nearest neighbor (a
#     generic status pill) but has no VERIFICATION-FAILURE-must-be-surfaced
#     invariant — a consumer can wire any StatusKind to any state, including
#     silently mapping every signature outcome to "success".
# So this is hand-rolled here, isolated to this scenario dir, in the same
# grep-based deliberately-simple style as the sibling scenarios'
# fe_pii_telemetry_denylist_guard.sh / locale_format_guard.sh.
#
# WHAT IT ENFORCES
# For every *.tsx file under --root that references the identifier
# `signatureStatus` (a component/hook consuming a webhook signature-
# verification outcome), the SAME FILE must also contain the literal phrase
# "could not be verified" (case-insensitive) — i.e. it must render a
# user-visible failure state, not silently render a static success UI
# regardless of the value.
#
# Exit codes:
#   0 — every file referencing signatureStatus also surfaces the failure UX
#       (OR no file references signatureStatus at all — nothing to check)
#   1 — a file consumes signatureStatus with no failure-surfacing text
#       (signature: WEBHOOK_SIGNATURE_STATUS_UX_MISSING)
#   2 — usage / environment error (root missing)
#
# Usage: bash webhook_signature_status_ux_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "webhook_signature_status_ux_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "webhook_signature_status_ux_guard: --root DIR is required" >&2
    exit 2
fi
if [ ! -d "$ROOT_OVERRIDE" ]; then
    echo "webhook_signature_status_ux_guard: no such dir $ROOT_OVERRIDE" >&2
    exit 2
fi

files="$(find "$ROOT_OVERRIDE" -name '*.tsx' 2>/dev/null || true)"
count=0
if [ -n "$files" ]; then
    count="$(printf '%s\n' "$files" | grep -c . || true)"
fi
if [ "$count" -eq 0 ]; then
    echo "webhook_signature_status_ux_guard: 0 *.tsx files under $ROOT_OVERRIDE — nothing to check"
    exit 0
fi
echo "webhook_signature_status_ux_guard: scanned $count *.tsx file(s)"

consumer_count=0
violations=""

while IFS= read -r f; do
    [ -z "$f" ] && continue
    if ! grep -qE 'signatureStatus' "$f" 2>/dev/null; then
        continue
    fi
    consumer_count=$((consumer_count + 1))
    # Strip the leading /* ... */ frontmatter/header block before checking —
    # this guard must credit rendered UX text, not a comment merely
    # DESCRIBING the intended behavior.
    body="$(awk '
        BEGIN { in_comment = 0; started = 0 }
        /^\/\*/ && started == 0 { in_comment = 1; next }
        in_comment == 1 && /\*\// { in_comment = 0; next }
        in_comment == 1 { next }
        { started = 1; print }
    ' "$f")"
    if ! printf '%s' "$body" | grep -qiE 'could not be verified'; then
        violations="$violations
  $f: consumes signatureStatus but never renders a 'could not be verified' failure state"
    fi
done <<EOF
$files
EOF

if [ "$consumer_count" -eq 0 ]; then
    echo "webhook_signature_status_ux_guard: 0 file(s) reference signatureStatus — nothing to check"
    exit 0
fi

if [ -n "$violations" ]; then
    echo "VIOLATION: webhook signature-verification status consumed but never surfaced to the user:" >&2
    echo "$violations" | sed 's/^/  /' >&2
    echo "" >&2
    echo "Any non-VERIFIED signatureStatus (WEBHOOK_SIGNATURE_MALFORMED /" >&2
    echo "WEBHOOK_TIMESTAMP_STALE / WEBHOOK_SIGNATURE_INVALID /" >&2
    echo "WEBHOOK_EVENT_REPLAYED) must render a distinct, user-visible" >&2
    echo "'Signature could not be verified' state — never the same success UI." >&2
    echo "webhook_signature_status_ux_guard: WEBHOOK_SIGNATURE_STATUS_UX_MISSING — BLOCKED" >&2
    exit 1
fi

echo "webhook_signature_status_ux_guard: every signatureStatus consumer surfaces the failure UX ($consumer_count file(s))"
exit 0
