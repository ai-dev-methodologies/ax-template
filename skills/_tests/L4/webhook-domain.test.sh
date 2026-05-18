#!/usr/bin/env bash
# skills/_tests/L4/webhook-domain.test.sh — R9 SP45 TDD anchor.
#
# Sealed catalog-discoverability test for the webhook L4 row (NET-NEW Spec Trio
# at SP45 — first genuinely net-new L4 since billing R5).
# Asserts that a context-0 sub-agent with only templates/L4/webhook/README.md
# + practices/AGENTS.md could discover:
#   1. The L4 README exists at the documented path.
#   2. The README references all 3 Spec Trio paths (spec + contract + manifest).
#   3. The README names ≥3 of the EMIT / SIGN / RETRY / DEAD-LETTER /
#      CIRCUIT-BREAKER / IDEMPOTENCY families.
#   4. The README cites at least one documented external verbatim
#      (GitHub Webhooks OR Stripe Webhooks).
#   5. The README explains the HMAC-SHA256 cryptographic anchor reuse
#      (RFC 2104 / ASVS V13.2.6 — receiver rule shares anchor).
#   6. templates/DECISIONS.md carries the TD-2026-05-22-025 entry.
#   7. Both backend skeleton stubs exist with .skeleton suffix (not .java —
#      keeps Gradle compile path clean for forks that copy the L4 tree
#      wholesale).
#   8. The R3 / SP45 Spec Trio files (spec + contract + manifest) are intact.
#   9. The README does NOT carry an `applied_recipes:` key at SP45 (first-
#      consumer convention; SP45b births the key with `[internal-it]`).
#  10. `trio_integrity_allowlist.yaml` lists `webhook: backend_only`.
#
# Semantic anchor (PRD M4 closure):
#   Assertion-count semantics mirror skills/_tests/L4/scheduler-domain.test.sh —
#   N total assertions, FAIL if ANY assertion fails (binary gate). Harness shape
#   is no longer net-new; matches R7 SP41 / R8 SP43 precedent.
#
# This replaces `/ax-verify-domain webhook` as the SP45 binary gate
# (R7 Critic L option (c) — webhook L4 stays catalog-only per CLAUDE.md
# recipe-no-code principle; Gradle `testWebhook` task does not exist).
#
# Exit 0 = GREEN (all assertions pass)
# Exit 1 = RED (any assertion fails)
#
# Usage: bash skills/_tests/L4/webhook-domain.test.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

README="$REPO_ROOT/templates/L4/webhook/README.md"
DECISIONS="$REPO_ROOT/templates/DECISIONS.md"
SKELETON_ENDPOINT="$REPO_ROOT/templates/L4/webhook/backend/WebhookEndpoint.java.skeleton"
SKELETON_DELIVERY="$REPO_ROOT/templates/L4/webhook/backend/WebhookDelivery.java.skeleton"
SPEC="$REPO_ROOT/specs/webhook-l0.yaml"
CONTRACT="$REPO_ROOT/contracts/webhook-openapi.yaml"
MANIFEST="$REPO_ROOT/blueprints/webhook-manifest.yaml"
ALLOWLIST="$REPO_ROOT/practices/evals/trio_integrity_allowlist.yaml"

PASS=0
FAIL=0
RESULTS=()

assert_pass() {
    PASS=$((PASS + 1))
    RESULTS+=("PASS [$1]")
}

assert_fail() {
    FAIL=$((FAIL + 1))
    RESULTS+=("FAIL [$1] $2")
}

# ── 1. L4 README exists ─────────────────────────────────────────────────────
if [ -f "$README" ]; then
    assert_pass "readme-exists"
else
    assert_fail "readme-exists" "$README not found"
fi

# ── 2. README references all 3 Spec Trio files ──────────────────────────────
if [ -f "$README" ]; then
    for trio_path in \
        "specs/webhook-l0.yaml" \
        "contracts/webhook-openapi.yaml" \
        "blueprints/webhook-manifest.yaml"; do
        if grep -qF "$trio_path" "$README"; then
            assert_pass "trio-ref/$trio_path"
        else
            assert_fail "trio-ref/$trio_path" "README missing reference to $trio_path"
        fi
    done
fi

# ── 3. README names ≥3 of EMIT / SIGN / RETRY / DEAD-LETTER / CIRCUIT / IDEMPOTENCY ─
if [ -f "$README" ]; then
    families_found=0
    for family in EMIT SIGN RETRY DEAD-LETTER CIRCUIT-BREAKER CIRCUIT IDEMPOTENCY IDEMPOTENT; do
        if grep -qF "$family" "$README"; then
            families_found=$((families_found + 1))
        fi
    done
    if [ "$families_found" -ge 3 ]; then
        assert_pass "spec-families-named/$families_found"
    else
        assert_fail "spec-families-named" \
            "README names only $families_found of EMIT/SIGN/RETRY/DEAD-LETTER/CIRCUIT-BREAKER/IDEMPOTENCY families (need ≥3)"
    fi
fi

# ── 4. README cites documented external verbatim (GitHub OR Stripe webhooks) ─
if [ -f "$README" ]; then
    if grep -qiE 'docs\.github\.com/en/webhooks|docs\.stripe\.com/webhooks' "$README"; then
        assert_pass "external-verbatim-cited"
    else
        assert_fail "external-verbatim-cited" \
            "README must cite GitHub Webhooks OR Stripe Webhooks verbatim URL"
    fi
fi

# ── 5. README explains HMAC anchor reuse (RFC 2104 / ASVS V13.2.6) ──────────
if [ -f "$README" ]; then
    if grep -qE 'RFC 2104|ASVS V13\.2\.6' "$README"; then
        assert_pass "hmac-anchor-reuse-explained"
    else
        assert_fail "hmac-anchor-reuse-explained" \
            "README must explain HMAC-SHA256 anchor reuse (RFC 2104 / OWASP ASVS V13.2.6 — receiver rule shares anchor)"
    fi
fi

# ── 6. TD-2026-05-22-025 in templates/DECISIONS.md ──────────────────────────
if [ -f "$DECISIONS" ]; then
    if grep -qF 'TD-2026-05-22-025' "$DECISIONS"; then
        assert_pass "td-025-present"
    else
        assert_fail "td-025-present" "templates/DECISIONS.md missing TD-2026-05-22-025 entry"
    fi
else
    assert_fail "td-025-present" "templates/DECISIONS.md not found"
fi

# ── 7. Backend skeleton stubs exist with .skeleton suffix ───────────────────
for stub in "$SKELETON_ENDPOINT" "$SKELETON_DELIVERY"; do
    if [ -f "$stub" ]; then
        assert_pass "skeleton-stub-exists/$(basename "$stub")"
    else
        assert_fail "skeleton-stub-exists/$(basename "$stub")" "$stub not found"
    fi
done

# ── 8. SP45-shipped Spec Trio files present (NET-NEW — must land together) ──
for f in "$SPEC" "$CONTRACT" "$MANIFEST"; do
    if [ -f "$f" ]; then
        assert_pass "trio-intact/$(basename "$f")"
    else
        assert_fail "trio-intact/$(basename "$f")" "missing SP45 Spec Trio artifact: $f"
    fi
done

# ── 9. README has NO applied_recipes: key at SP45 (first-consumer convention)
if [ -f "$README" ]; then
    if grep -qE '^applied_recipes:' "$README"; then
        assert_fail "applied-recipes-key-absent-at-SP45" \
            "README must NOT carry applied_recipes: key at SP45 introduction (SP45b births key [internal-it] per TD-2026-05-21-024 first-consumer-arrival convention)"
    else
        assert_pass "applied-recipes-key-absent-at-SP45"
    fi
fi

# ── 10. trio_integrity_allowlist.yaml lists webhook: backend_only ───────────
if [ -f "$ALLOWLIST" ]; then
    if grep -qE '^\s*webhook:\s*backend_only' "$ALLOWLIST"; then
        assert_pass "allowlist-webhook-backend-only"
    else
        assert_fail "allowlist-webhook-backend-only" \
            "practices/evals/trio_integrity_allowlist.yaml must list 'webhook: backend_only'"
    fi
else
    assert_fail "allowlist-webhook-backend-only" "$ALLOWLIST not found"
fi

# ── Summary ─────────────────────────────────────────────────────────────────
echo "=== webhook-domain.test.sh — R9 SP45 TDD anchor ==="
echo ""
for r in "${RESULTS[@]}"; do
    echo "  $r"
done
echo ""
echo "Total: $PASS passed, $FAIL failed"

if [ "$FAIL" -gt 0 ]; then
    echo "webhook-domain: FAIL — $FAIL assertion(s) did not pass" >&2
    exit 1
fi

echo "webhook-domain: PASS — context-0 sub-agent can discover the webhook L4 row"
exit 0
