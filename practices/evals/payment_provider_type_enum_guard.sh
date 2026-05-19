#!/usr/bin/env bash
# payment_provider_type_enum_guard.sh — iter-10 hard guard (29th).
#
# Enforces spec MUST specs/payment-l0.yaml#PAYMENT-PROVIDER-ENUM-001:
#   The scalar at blueprints/payment-manifest.yaml#provider.type MUST be a
#   member of the list at blueprints/payment-manifest.yaml#provider.type_allowed.
#
# Failure modes (any → exit 1):
#   (a) manifest file missing
#   (b) provider.type_allowed missing or empty
#   (c) provider.type missing
#   (d) provider.type not in provider.type_allowed (strict string equality;
#       no case-folding, no whitespace leniency)
#   (e) any allow-list member violates ^[a-z][a-z0-9_]*$ (snake_case ASCII)
#
# Usage: bash practices/evals/payment_provider_type_enum_guard.sh
# Exit:  0 = PASS; 1 = FAIL.
#
# Why this guard exists (P2 Round 10 NEW-NC6):
#   Before iter-10 the manifest accepted any free string at provider.type.
#   A fork-receiver edit such as `type: kgInicis` (vs intended `kg_inicis`)
#   would bypass every existing guard and either silently fall through to
#   the default mock adapter at runtime or wire a non-existent
#   PaymentProvider bean — a production-grade silent failure surface.

set -euo pipefail
cd "$(dirname "$0")/../.."

MANIFEST="blueprints/payment-manifest.yaml"
SPEC_MUST="specs/payment-l0.yaml#PAYMENT-PROVIDER-ENUM-001"

if [ ! -f "$MANIFEST" ]; then
    echo "VIOLATION [provider-enum]: manifest missing: $MANIFEST" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

# Extract provider.type — scalar under top-level "provider:" key.
PROVIDER_TYPE=$(awk '
    /^provider:[[:space:]]*$/ { intop=1; next }
    intop && /^[^[:space:]#]/ { intop=0 }
    intop && /^[[:space:]]+type:[[:space:]]*/ {
        line=$0
        sub(/^[[:space:]]+type:[[:space:]]*/, "", line)
        sub(/[[:space:]]*#.*$/, "", line)
        sub(/[[:space:]]+$/, "", line)
        print line
        exit
    }
' "$MANIFEST")

if [ -z "$PROVIDER_TYPE" ]; then
    echo "VIOLATION [provider-enum]: provider.type scalar not found in $MANIFEST" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

# Extract provider.type_allowed list members.
ALLOWED=$(awk '
    /^provider:[[:space:]]*$/ { intop=1; next }
    intop && /^[^[:space:]#]/ { intop=0 }
    intop && /^[[:space:]]+type_allowed:[[:space:]]*$/ { collect=1; next }
    collect && /^[[:space:]]+-[[:space:]]+/ {
        line=$0
        sub(/^[[:space:]]+-[[:space:]]+/, "", line)
        sub(/[[:space:]]*#.*$/, "", line)
        sub(/[[:space:]]+$/, "", line)
        print line
        next
    }
    collect && /^[[:space:]]+[a-zA-Z_]+:/ { collect=0 }
    collect && /^[^[:space:]]/             { collect=0; intop=0 }
' "$MANIFEST")

if [ -z "$ALLOWED" ]; then
    echo "VIOLATION [provider-enum]: provider.type_allowed missing or empty in $MANIFEST" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

# Validate every member matches ^[a-z][a-z0-9_]*$ (snake_case ASCII).
INVALID_MEMBERS=""
while IFS= read -r member; do
    [ -z "$member" ] && continue
    if ! echo "$member" | grep -qE '^[a-z][a-z0-9_]*$'; then
        INVALID_MEMBERS="$INVALID_MEMBERS $member"
    fi
done <<< "$ALLOWED"

if [ -n "$INVALID_MEMBERS" ]; then
    echo "VIOLATION [provider-enum]: type_allowed member(s) violate ^[a-z][a-z0-9_]*$ :$INVALID_MEMBERS" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

# Strict membership check.
MATCH=0
while IFS= read -r member; do
    [ -z "$member" ] && continue
    if [ "$PROVIDER_TYPE" = "$member" ]; then
        MATCH=1
        break
    fi
done <<< "$ALLOWED"

if [ "$MATCH" -ne 1 ]; then
    echo "VIOLATION [provider-enum]: provider.type='$PROVIDER_TYPE' not in type_allowed:" >&2
    while IFS= read -r m; do [ -n "$m" ] && echo "  - $m" >&2; done <<< "$ALLOWED"
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

echo "payment_provider_type_enum_guard: PASS — provider.type='$PROVIDER_TYPE' ∈ type_allowed"
exit 0
