#!/usr/bin/env bash
# skills/ax-scaffold/scripts/validate-domain-name.sh — Domain name validator.
#
# Validates that a domain name:
#   - Is non-empty
#   - Is lowercase-kebab-case only (a-z, 0-9, hyphens)
#   - Does not start or end with a hyphen
#   - Contains no consecutive hyphens
#   - Is at most 64 characters
#
# Exit 0 = valid
# Exit 1 = invalid (error code + message on stderr)
#
# Usage:
#   bash validate-domain-name.sh <domain>
set -uo pipefail

DOMAIN="${1:-}"

if [ -z "$DOMAIN" ]; then
    echo "INVALID_NAME: domain name is required" >&2
    exit 1
fi

if [ "${#DOMAIN}" -gt 64 ]; then
    echo "INVALID_NAME: domain name must be at most 64 characters (got ${#DOMAIN})" >&2
    exit 1
fi

if [[ ! "$DOMAIN" =~ ^[a-z0-9]([a-z0-9-]*[a-z0-9])?$ ]]; then
    echo "INVALID_NAME: '$DOMAIN' must be lowercase-kebab-case (a-z, 0-9, hyphens; no leading/trailing hyphens)" >&2
    exit 1
fi

if [[ "$DOMAIN" == *"--"* ]]; then
    echo "INVALID_NAME: '$DOMAIN' must not contain consecutive hyphens" >&2
    exit 1
fi

exit 0
