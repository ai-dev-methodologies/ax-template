#!/usr/bin/env bash
# skills/ax-verify-domain/scripts/run-gradle.sh
# Runs ./gradlew test<Domain> for a given domain.
# Usage: bash run-gradle.sh <domain>
# For frontend_only domains (practices): skips Gradle.
# Exit 0 on pass or skip; exit 1 on Gradle failure.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

DOMAIN_LOWER="${1:-}"
if [ -z "$DOMAIN_LOWER" ]; then
    echo "USAGE: bash run-gradle.sh <domain>" >&2
    exit 1
fi

ALLOWLIST="$REPO_ROOT/practices/evals/trio_integrity_allowlist.yaml"

# Check if domain is frontend_only (skip Gradle for those)
if [ -f "$ALLOWLIST" ] && grep -A3 "domain: $DOMAIN_LOWER" "$ALLOWLIST" 2>/dev/null | grep -q "frontend_only"; then
    echo "  SKIP [gradle] domain=$DOMAIN_LOWER is frontend_only — no Gradle task"
    exit 0
fi
if [ "$DOMAIN_LOWER" = "practices" ]; then
    echo "  SKIP [gradle] practices domain is frontend_only — no Gradle task"
    exit 0
fi

# Domain → Gradle task mapping.
# Some domains use a different Gradle task name than the simple "test<Domain>" convention.
# Hyphenated domain names (e.g., audit-log, file-storage) are camel-cased: testAuditLog.
case "$DOMAIN_LOWER" in
    auth)         GRADLE_TASK="testAsvs" ;;       # auth ASVS tests
    crud)         GRADLE_TASK="testCrud" ;;
    payment)      GRADLE_TASK="testPayment" ;;
    ratelimit)    GRADLE_TASK="testRatelimit" ;;
    audit-log)    GRADLE_TASK="testAuditLog" ;;
    file-storage) GRADLE_TASK="testFileStorage" ;;
    notification)    GRADLE_TASK="testNotification" ;;
    feature-flags)   GRADLE_TASK="testFeatureFlags" ;;
    *)
        # Generic: capitalise first letter of each hyphen-separated segment and join
        DOMAIN_PASCAL="$(echo "$DOMAIN_LOWER" | awk -F'-' '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) substr($i,2); OFS=""; print}')"
        GRADLE_TASK="test${DOMAIN_PASCAL}"
        ;;
esac

if [ ! -d "$REPO_ROOT/backend" ]; then
    echo "  SKIP [gradle] backend/ not present"
    exit 0
fi

echo "  Running: cd backend && ./gradlew $GRADLE_TASK"
cd "$REPO_ROOT/backend" && ./gradlew "$GRADLE_TASK"
