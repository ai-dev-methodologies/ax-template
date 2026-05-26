#!/usr/bin/env bash
# practices/evals/l4_frontend_domain_mode_guard.sh
# R59 (41st hard guard) — mechanises the rule at
# practices/rules/spec-domain-mode-gates-frontend-trio.md.
#
# Enforces that any L4 directory shipping a frontend trio
# (templates/L4/<domain>/app/) has a matching spec file at
# specs/<domain>-l0.yaml whose `domain_mode` field permits the trio.
#
# Permitted modes  : full_trio, frontend_only
# Refused modes    : backend_only, missing, unknown
#
# When the field is absent from the spec, the guard treats it as
# backend_only (per the R58 rule's anti-pattern: "absent is a design
# signal, not silent permission").
#
# Exit codes:
#   0 — every templates/L4/<domain>/app/ has a permissive domain_mode
#   1 — at least one violation
#   2 — usage error / missing required source files
#
# Usage:
#   bash practices/evals/l4_frontend_domain_mode_guard.sh
#   bash practices/evals/l4_frontend_domain_mode_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "l4_frontend_domain_mode_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "cannot cd to $REPO_ROOT" >&2; exit 2; }

L4_DIR="templates/L4"
SPECS_DIR="specs"

if [ ! -d "$L4_DIR" ]; then
    echo "l4_frontend_domain_mode_guard: $L4_DIR not found" >&2
    exit 2
fi

violations=0

# Iterate domain dirs under templates/L4 that have an app/ subdirectory.
for app_dir in "$L4_DIR"/*/app; do
    [ -d "$app_dir" ] || continue
    domain_dir="$(dirname "$app_dir")"
    domain="$(basename "$domain_dir")"

    # Practices is an INFRA L4 (per specs/l4-domain-classification.yaml#infra)
    # and intentionally has no -l0.yaml spec. Skip.
    if [ "$domain" = "practices" ]; then
        continue
    fi

    # Spec file lookup — try canonical name first, then known alternates.
    # auth uses auth-frontend-l0.yaml (split from auth-asvs-l1.yaml).
    # crud uses crud-frontend-l0.yaml (split from crud-security.yaml).
    spec_file="$SPECS_DIR/${domain}-l0.yaml"
    if [ ! -f "$spec_file" ]; then
        alt_spec="$SPECS_DIR/${domain}-frontend-l0.yaml"
        if [ -f "$alt_spec" ]; then
            spec_file="$alt_spec"
        else
            echo "VIOLATION [$domain]:"
            echo "  - $app_dir/ exists but $SPECS_DIR/${domain}-l0.yaml is missing"
            echo "  - tried alternate: $alt_spec (also missing)"
            violations=$((violations + 1))
            continue
        fi
    fi

    # Extract domain_mode value. Accept yaml shapes:
    #   domain_mode: backend_only
    #   domain_mode: backend_only   # comment
    #   domain_mode: "full_trio"
    #   domain_mode: 'frontend_only'
    mode_line="$(grep -E '^[[:space:]]*domain_mode[[:space:]]*:' "$spec_file" | head -1 || true)"
    if [ -z "$mode_line" ]; then
        # Absent is a design signal; treat as backend_only.
        echo "VIOLATION [$domain]:"
        echo "  - $app_dir/ exists but $spec_file has no domain_mode field"
        echo "  - the catalog treats absent domain_mode as backend_only (R58)"
        violations=$((violations + 1))
        continue
    fi

    mode="$(echo "$mode_line" \
        | sed -E 's/^[[:space:]]*domain_mode[[:space:]]*:[[:space:]]*//' \
        | sed -E 's/[[:space:]]*#.*$//' \
        | sed -E 's/^["'\'']//' \
        | sed -E 's/["'\'']$//' \
        | sed -E 's/[[:space:]]+$//')"

    case "$mode" in
        full_trio|frontend_only)
            : # ok
            ;;
        backend_only)
            echo "VIOLATION [$domain]:"
            echo "  - $app_dir/ exists but $spec_file declares domain_mode: backend_only"
            echo "  - re-scope to backend residual closure or amend the spec's domain_mode"
            violations=$((violations + 1))
            ;;
        *)
            echo "VIOLATION [$domain]:"
            echo "  - $spec_file has unknown domain_mode value: '$mode'"
            echo "  - expected one of: backend_only / full_trio / frontend_only"
            violations=$((violations + 1))
            ;;
    esac
done

if [ "$violations" -gt 0 ]; then
    echo "l4_frontend_domain_mode_guard: $violations violation(s) — merge BLOCKED" >&2
    exit 1
fi

echo "l4_frontend_domain_mode_guard: every templates/L4/<domain>/app/ has a permissive domain_mode"
exit 0
