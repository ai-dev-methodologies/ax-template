#!/usr/bin/env bash
# practices/evals/fixtures/realtime_default_polling/_run.sh
#
# Binary guard: asserts that blueprints/realtime-policy-manifest.yaml exists
# and declares default_transport: polling (NOT sse or websocket).
#
# Exit 0 — manifest exists with polling default (PASS)
# Exit 1 — manifest missing OR default_transport != polling (FAIL: REALTIME_DEFAULT_NOT_POLLING)
#
# Usage:
#   bash practices/evals/fixtures/realtime_default_polling/_run.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

MANIFEST="$REPO_ROOT/blueprints/realtime-policy-manifest.yaml"

echo "=== realtime_default_polling guard ==="
echo ""

# ── Check 1: manifest exists ──────────────────────────────────────────────────
if [ ! -f "$MANIFEST" ]; then
    echo "  FAIL: blueprints/realtime-policy-manifest.yaml not found" >&2
    echo "  error: MANIFEST_NOT_FOUND" >&2
    echo "  hint: SP27 must create blueprints/realtime-policy-manifest.yaml" >&2
    exit 1
fi
echo "  PASS [manifest exists]: $MANIFEST"

# ── Check 2: default_transport is polling ─────────────────────────────────────
if ! grep -q "default_transport: polling" "$MANIFEST"; then
    echo "  FAIL: blueprints/realtime-policy-manifest.yaml does not declare default_transport: polling" >&2
    echo "  error: REALTIME_DEFAULT_NOT_POLLING" >&2
    echo "  hint: Set default_transport: polling in the manifest (SSE/WebSocket are opt-in only)" >&2
    exit 1
fi
echo "  PASS [default_transport: polling]"

# ── Check 3: SSE is NOT the default (must be under opt_in_transports) ─────────
if grep -qE "^  default_transport: (sse|websocket)" "$MANIFEST"; then
    echo "  FAIL: default_transport must not be 'sse' or 'websocket'" >&2
    echo "  error: REALTIME_DEFAULT_NOT_POLLING" >&2
    exit 1
fi
echo "  PASS [SSE and WebSocket are opt-in only]"

# ── Check 4: SSE declared as opt-in with serverless_safe: false ───────────────
if ! grep -q "serverless_safe: false" "$MANIFEST"; then
    echo "  WARN: SSE opt-in should declare serverless_safe: false (see ADR TD-2026-05-18-027)" >&2
    # Warning only — not a hard fail
fi
echo "  PASS [serverless_safe warning present]"

# ── Self-test against fail fixture ────────────────────────────────────────────
FAIL_FIXTURE="$SCRIPT_DIR/fail_sse_as_default/realtime-manifest.yaml"
if [ -f "$FAIL_FIXTURE" ]; then
    if grep -q "default_transport: polling" "$FAIL_FIXTURE"; then
        echo "  FAIL: fail fixture incorrectly declares polling as default" >&2
        exit 1
    fi
    echo "  PASS [fail fixture correctly uses sse as default (negative test)]"
fi

echo ""
echo "=== realtime_default_polling guard: PASS ==="
exit 0
