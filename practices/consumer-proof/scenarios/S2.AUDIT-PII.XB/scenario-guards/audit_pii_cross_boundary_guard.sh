#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S2.AUDIT-PII.XB/scenario-guards/audit_pii_cross_boundary_guard.sh
#
# HAND-ROLLED — capability-gap signal.
# This dogfood cell's GAP: an audit/error entity with a PII field (e.g.
# actorEmail) crosses the BE->FE boundary through a *Response/*Dto factory
# method with NO redaction. The catalog HAS PII-handling assets —
# templates/backend/audit-log/AuditLogPiiRedactor.java masks actorIp at
# WRITE time, and common/AuditPiiHelper#piiHash exists for exactly this
# hash-before-crossing use — but NOTHING enforces that every PII getter
# reaching a FE-facing DTO factory method is actually routed through one of
# them. Verified absent: `grep -ril "Response\|Dto" practices/evals/*.sh`
# → 0 hits that scan a DTO factory method body for an unredacted PII getter
# (phi_in_logs_guard.sh checks LOG statements, a different surface — a
# @Phi getter reaching a DTO untouched is invisible to it). No ArchUnit rule
# in the real backend targets DTO-mapping call sites either. So this is
# hand-rolled here, isolated to this scenario dir, in the same grep-based
# style as the sibling S3.b2b-admin scenario's fe_pii_telemetry_denylist_guard.sh.
#
# WHAT IT ENFORCES
# For every *Response.java / *Dto.java file (excluding *Entity.java) under
# --root, find every line that calls a PII-shaped getter —
#   .get(Email|Phone|Ssn|Rrn|Name|FullName|Dob|BirthDate|Address)()
# — matched case-sensitively on the getter name. If that SAME line has no
# redaction wrapper token (piiHash|redact|Redact|mask|Mask), the raw PII
# value is being passed straight into the FE-facing DTO: a cross-boundary
# leak. (A getter wrapped on the same line, e.g.
# `AuditPiiHelper.piiHash(entry.getActorEmail())`, is credited as redacted —
# this is a scenario probe, not a full dataflow analyzer.)
#
# ZERO_SCAN safety: root must contain at least one *Response.java/*Dto.java
# file with at least one PII-shaped getter call, or this is an environment/
# usage problem (exit 2) — a consumer with no such DTOs should not invoke
# this guard expecting a meaningful signal.
#
# Exit codes:
#   0 — every PII-shaped getter reaching a DTO factory is redacted on-line
#   1 — at least one raw PII getter crosses into a *Response/*Dto
#       (signature: AUDIT_PII_CROSS_BOUNDARY_UNREDACTED)
#   2 — usage / environment error (root missing, zero-scan)
#
# Usage: bash audit_pii_cross_boundary_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "audit_pii_cross_boundary_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "audit_pii_cross_boundary_guard: --root DIR is required" >&2
    exit 2
fi
if [ ! -d "$ROOT_OVERRIDE" ]; then
    echo "audit_pii_cross_boundary_guard: no such dir $ROOT_OVERRIDE" >&2
    exit 2
fi

# DTO-shaped files: *Response.java or *Dto.java, but never *Entity.java.
mapfile_workaround="$(find "$ROOT_OVERRIDE" \( -name '*Response.java' -o -name '*Dto.java' \) ! -name '*Entity.java' 2>/dev/null || true)"
files_scanned=0
pii_getter_calls=0
violations=""

GETTER_RE='\.get[A-Za-z0-9]*(Email|Phone|Ssn|Rrn|Name|Dob|BirthDate|Address)\(\)'
WRAPPER_RE='piiHash|[Rr]edact|[Mm]ask'

if [ -n "$mapfile_workaround" ]; then
    while IFS= read -r f; do
        [ -z "$f" ] && continue
        files_scanned=$((files_scanned + 1))
        hits="$(grep -nE "$GETTER_RE" "$f" 2>/dev/null || true)"
        [ -z "$hits" ] && continue
        while IFS= read -r hitline; do
            [ -z "$hitline" ] && continue
            # Skip comment lines (javadoc '*' continuation, '//', '/**') —
            # this guard scans call sites, not prose mentioning a getter name.
            content="${hitline#*:}"
            trimmed_content="$(printf '%s' "$content" | sed -e 's/^[[:space:]]*//')"
            case "$trimmed_content" in
                '*'*|'//'*|'/**'*) continue ;;
            esac
            pii_getter_calls=$((pii_getter_calls + 1))
            if ! printf '%s' "$hitline" | grep -qE "$WRAPPER_RE"; then
                lineno="${hitline%%:*}"
                violations="$violations
  $f:$lineno: raw PII getter crosses into FE-facing DTO unredacted — $(printf '%s' "$hitline" | sed 's/^[0-9]*://' | sed 's/^ *//')"
            fi
        done <<EOF
$hits
EOF
    done <<EOF
$mapfile_workaround
EOF
fi

if [ "$files_scanned" -eq 0 ] || [ "$pii_getter_calls" -eq 0 ]; then
    echo "audit_pii_cross_boundary_guard: ZERO_SCAN — no *Response.java/*Dto.java with a PII-shaped getter call found under $ROOT_OVERRIDE" >&2
    exit 2
fi

echo "audit_pii_cross_boundary_guard: scanned $files_scanned DTO file(s), $pii_getter_calls PII-shaped getter call(s)"

if [ -n "$violations" ]; then
    echo "VIOLATION: raw PII getter crosses the BE->FE boundary unredacted:" >&2
    echo "$violations" | sed 's/^/  /' >&2
    echo "" >&2
    echo "Every PII-shaped getter (email/phone/ssn/rrn/name/dob/address) reaching a" >&2
    echo "*Response/*Dto factory method must be wrapped in a redaction call" >&2
    echo "(AuditPiiHelper.piiHash(...) or an equivalent redact/mask helper) on the" >&2
    echo "same line — never passed straight through." >&2
    echo "audit_pii_cross_boundary_guard: AUDIT_PII_CROSS_BOUNDARY_UNREDACTED — BLOCKED" >&2
    exit 1
fi

echo "audit_pii_cross_boundary_guard: every PII-shaped getter reaching a DTO factory is redacted"
exit 0
