#!/usr/bin/env bash
# skills/ax-verify-domain/scripts/run.sh
# Tier-2 domain-axis verifier orchestrator.
# Usage: bash run.sh <domain>
# Example: bash run.sh auth
# Steps 1-6 are binary (step 5 skipped for frontend_only domains).
# Exit 0 iff all applicable steps pass.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

DOMAIN="${1:-}"

if [ -z "$DOMAIN" ]; then
    echo "USAGE: bash run.sh <domain>" >&2
    echo "  Supported: auth, crud, payment, practices (and any new domain from ax-scaffold)" >&2
    exit 1
fi

echo "=== ax-verify-domain: run.sh [domain=$DOMAIN] ==="
echo ""

# Step 1: Confirm domain is in allowlist
echo "[1] allowlist check"
if bash "$SCRIPT_DIR/check-allowlist.sh" "$DOMAIN"; then
    echo "  PASS [allowlist]"
else
    echo "  FAIL [allowlist]" >&2
    echo "  hint: run /ax-scaffold $DOMAIN first, or add to trio_integrity_allowlist.yaml" >&2
    exit 1
fi

# Step 2: evidence_guard scoped to domain artifacts
echo ""
echo "[2] evidence_guard (domain=$DOMAIN)"
SCOPE_DIRS=()
[ -d "$REPO_ROOT/templates/L4/$DOMAIN" ] && SCOPE_DIRS+=("$REPO_ROOT/templates/L4/$DOMAIN")

if [ ${#SCOPE_DIRS[@]} -eq 0 ]; then
    echo "  SKIP [evidence_guard] templates/L4/$DOMAIN/ not yet present"
else
    for dir in "${SCOPE_DIRS[@]}"; do
        if ! bash "$REPO_ROOT/practices/evals/evidence_guard.sh" "$dir"; then
            echo "  FAIL [evidence_guard for $dir]" >&2
            echo "  hint: invoke /ax-guard-evidence for fix-loop" >&2
            exit 1
        fi
    done
    echo "  PASS [evidence_guard]"
fi

# Step 3: trio_integrity_guard (domain-scoped)
echo ""
echo "[3] trio_integrity_guard (domain=$DOMAIN)"
if bash "$REPO_ROOT/practices/evals/trio_integrity_guard.sh" --domain "$DOMAIN" 2>/dev/null; then
    echo "  PASS [trio_integrity_guard]"
elif bash "$REPO_ROOT/practices/evals/trio_integrity_guard.sh" 2>/dev/null; then
    # Fallback: full scan if --domain flag not supported yet
    echo "  PASS [trio_integrity_guard] (full scan)"
else
    echo "  FAIL [trio_integrity_guard]" >&2
    echo "  hint: invoke /ax-guard-trio-integrity; see error code table" >&2
    exit 1
fi

# Step 4: cross_trio_guard (domain-scoped)
echo ""
echo "[4] cross_trio_guard (domain=$DOMAIN)"
if [ -d "$REPO_ROOT/templates/L4/$DOMAIN" ]; then
    if bash "$REPO_ROOT/practices/evals/cross_trio_guard.sh" --domain "$DOMAIN" 2>/dev/null || \
       bash "$REPO_ROOT/practices/evals/cross_trio_guard.sh" 2>/dev/null; then
        echo "  PASS [cross_trio_guard]"
    else
        echo "  FAIL [cross_trio_guard]" >&2
        echo "  hint: add evidence: block to the named L1/L2/L3 file" >&2
        exit 1
    fi
else
    echo "  SKIP [cross_trio_guard] templates/L4/$DOMAIN/ not yet present"
fi

# Step 5: Backend Gradle tests (skip for frontend_only domains)
echo ""
echo "[5] backend tests (domain=$DOMAIN)"
if bash "$SCRIPT_DIR/run-gradle.sh" "$DOMAIN"; then
    echo "  PASS [gradle]"
else
    echo "  FAIL [gradle]" >&2
    exit 1
fi

# Step 6: Playwright E2E (domain-scoped)
echo ""
echo "[6] Playwright E2E (domain=$DOMAIN)"
if bash "$SCRIPT_DIR/run-playwright.sh" "$DOMAIN"; then
    echo "  PASS [Playwright]"
else
    echo "  FAIL [Playwright]" >&2
    echo "  hint: open trace for $DOMAIN test; fix the failing flow" >&2
    exit 1
fi

echo ""
echo "=== ax-verify-domain [$DOMAIN]: all steps PASS ==="
exit 0
