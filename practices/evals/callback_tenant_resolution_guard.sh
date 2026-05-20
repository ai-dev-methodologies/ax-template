#!/usr/bin/env bash
# callback_tenant_resolution_guard.sh — dogfood R5 GAP-R3-4 closure (38th hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# `#callback-tenant-resolution.verifier_contract`. External PG callback
# verifiers (NICE / Toss / KakaoPay) in multi-tenant fork-receivers MUST
# atomically pair signature verification with tenant resolution:
#
#   (a) The verifier interface MUST expose exactly one operation that
#       returns the resolved tenant_id (java.util.UUID). Splitting the
#       call into separate `verify(...)` + `resolveTenant(...)` methods
#       lets a bug accept a signature signed by tenant B's secret while
#       resolving TenantContext from tenant A's forgeable orderId.
#
#   (b) The signature MUST include the raw body (byte[]) — NOT the
#       parsed JSON or the orderId scalar — so verification runs on
#       the wire payload before any normalization. A `String body`
#       parameter type is rejected because String would force decoding
#       the body for verification, opening normalization-attack vectors.
#
#   (c) Two distinct exception types MUST exist:
#         - CallbackSignatureMismatchException (no match → orphan audit row)
#         - AmbiguousTenantResolutionException (multiple match → ops alert)
#       Collapsing the two into a single exception (or worse, a boolean
#       return) loses the operational signal that distinguishes "attacker
#       sent garbage" from "catalog has duplicate secrets".
#
# Algorithm:
#   1. For each candidate fixture (default: the multi-tenant-aop passing
#      fixture's TenantAwareCallbackVerifier.java), parse the file and
#      verify:
#        - clause (a): the interface declares a method returning UUID,
#          AND the method takes raw bytes (byte[]) AND a signature
#          header AND a provider key,
#        - clause (b): the body parameter is byte[] not String,
#        - clause (c): the two exception classes are present in the
#          same package.
#   2. With --fixtures, ALSO assert the failing-side variants
#      (single-call verify() returning boolean; String body; missing
#      exception types) trip the guard. Reserved for symmetric
#      extension; skips with a notice when the failing file is absent.
#
# Usage:
#   bash practices/evals/callback_tenant_resolution_guard.sh
#   bash practices/evals/callback_tenant_resolution_guard.sh --fixtures
#
# Exit codes:
#   0 — every callback verifier satisfies the three clauses
#   1 — at least one clause violated OR (with --fixtures) the failing/
#       fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#callback-tenant-resolution"

PASS_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy"
FAIL_DIR="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy"

PASS_VERIFIER="$PASS_DIR/TenantAwareCallbackVerifier.java"
PASS_MISMATCH="$PASS_DIR/CallbackSignatureMismatchException.java"
PASS_AMBIGUOUS="$PASS_DIR/AmbiguousTenantResolutionException.java"

# verify_callback_verifier VERIFIER_FILE MISMATCH_FILE AMBIGUOUS_FILE LABEL
# Returns 0 if all three clauses hold, 1 otherwise.
verify_callback_verifier() {
    local verifier="$1"
    local mismatch="$2"
    local ambiguous="$3"
    local label="$4"
    local violations=0

    if [ ! -f "$verifier" ]; then
        echo "VIOLATION [$label]: verifier interface file missing: $verifier" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.verifier_contract" >&2
        return 1
    fi

    # Clause (a): atomic UUID-returning method present.
    # The signature shape (single line, possibly multi-line) returns UUID
    # and takes (byte[], String, String). We accept method whose return
    # type token is UUID and whose name is verifyAndResolveTenant.
    if grep -qE 'UUID[[:space:]]+verifyAndResolveTenant[[:space:]]*\(' "$verifier"; then
        : # PASS clause (a) — method signature is correct shape
    else
        echo "VIOLATION [$label] clause(a): no `UUID verifyAndResolveTenant(...)` method in $verifier" >&2
        echo "  Verifier MUST expose atomic signature-verification + tenant-resolution as one call." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.verifier_contract.interface_shape" >&2
        violations=$((violations + 1))
    fi

    # Reject split-call shape: explicit `verify(...)` returning boolean,
    # OR a separate `resolveTenantFromOrderId(...)` / `resolveTenant(...)`
    # method, on a verifier interface.
    if grep -qE '\bboolean[[:space:]]+verify[[:space:]]*\(' "$verifier"; then
        echo "VIOLATION [$label] clause(a): split-call shape detected — `boolean verify(...)` present in $verifier" >&2
        echo "  Splitting verification from tenant resolution lets a bug accept tenant B's signature with tenant A's orderId." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.verifier_contract.forbidden_alternatives" >&2
        violations=$((violations + 1))
    fi
    if grep -qE '\bresolveTenant(FromOrderId|FromPath|FromBody|FromHeader)?[[:space:]]*\(' "$verifier"; then
        echo "VIOLATION [$label] clause(a): split-call shape detected — separate `resolveTenant*(...)` method present in $verifier" >&2
        echo "  Verifier MUST atomically pair verification and resolution; never expose tenant resolution as a standalone call." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.verifier_contract.forbidden_alternatives" >&2
        violations=$((violations + 1))
    fi

    # Clause (b): body parameter type is byte[] not String.
    # Pull the method signature (verifyAndResolveTenant) and inspect the
    # FIRST parameter's type. We support multi-line signatures by joining
    # lines until the first `)` after the method name.
    local sig
    sig=$(awk '
        BEGIN { capture = 0; out = "" }
        /verifyAndResolveTenant[[:space:]]*\(/ { capture = 1 }
        capture {
            out = out " " $0
            if ($0 ~ /\)/) { print out; exit }
        }
    ' "$verifier")

    if [ -n "$sig" ]; then
        # Extract first parameter type token (between `(` and the first `,` OR `)`).
        local first_param
        first_param=$(echo "$sig" | sed -E 's/.*verifyAndResolveTenant[[:space:]]*\(([^,)]*)[,)].*/\1/')
        # Trim and grab the leading type token.
        first_param=$(echo "$first_param" | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//')
        # The shape is "<type> <paramName>"; type is everything except the trailing identifier.
        local first_type
        first_type=$(echo "$first_param" | awk '{ for (i=1; i<NF; i++) printf "%s ", $i; print "" }' | sed -E 's/[[:space:]]+$//')
        if [ -z "$first_type" ]; then
            # Fallback: take the whole token if awk could not split.
            first_type="$first_param"
        fi

        if [ "$first_type" = "byte[]" ]; then
            : # PASS clause (b)
        else
            echo "VIOLATION [$label] clause(b): first parameter of verifyAndResolveTenant is '$first_type', expected 'byte[]' in $verifier" >&2
            echo "  String-typed body forces decoding before verification (normalization attack vector); raw bytes are required." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.verifier_contract.interface_shape" >&2
            violations=$((violations + 1))
        fi
    else
        echo "VIOLATION [$label] clause(b): cannot extract verifyAndResolveTenant signature from $verifier" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.verifier_contract.interface_shape" >&2
        violations=$((violations + 1))
    fi

    # Clause (c): both exception classes exist alongside the verifier.
    if [ ! -f "$mismatch" ]; then
        echo "VIOLATION [$label] clause(c): CallbackSignatureMismatchException missing at $mismatch" >&2
        echo "  No-match failure MUST be distinct from multiple-match (different operational signal)." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.verifier_contract.failure_modes.no_match" >&2
        violations=$((violations + 1))
    fi
    if [ ! -f "$ambiguous" ]; then
        echo "VIOLATION [$label] clause(c): AmbiguousTenantResolutionException missing at $ambiguous" >&2
        echo "  Multiple-match failure MUST be distinct (signals secret-reuse, not attacker garbage)." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.verifier_contract.failure_modes.multiple_match" >&2
        violations=$((violations + 1))
    fi

    if [ "$violations" -gt 0 ]; then
        echo "callback_tenant_resolution_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "callback_tenant_resolution_guard: PASS [$label] — all 3 clauses hold (atomic UUID call + byte[] body + distinct exceptions)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "callback_tenant_resolution_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# ── Default scan ────────────────────────────────────────────────────────────
# Passing fixture MUST satisfy all three clauses.
verify_callback_verifier "$PASS_VERIFIER" "$PASS_MISMATCH" "$PASS_AMBIGUOUS" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that ships a
# TenantAwareCallbackVerifier file MUST satisfy the policy. Single-tenant
# repos have no such file and SKIP.
LIVE_FOUND=0
while IFS= read -r dir; do
    if [ -f "$dir/TenantAwareCallbackVerifier.java" ]; then
        LIVE_FOUND=$((LIVE_FOUND + 1))
        verify_callback_verifier \
            "$dir/TenantAwareCallbackVerifier.java" \
            "$dir/CallbackSignatureMismatchException.java" \
            "$dir/AmbiguousTenantResolutionException.java" \
            "live:$dir" || OVERALL=1
    fi
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "callback_tenant_resolution_guard: live-repo SKIP — no .../multitenancy/TenantAwareCallbackVerifier.java (single-tenant default)"
fi

# ── --fixtures mode ─────────────────────────────────────────────────────────
if [ "$MODE" = "fixtures" ]; then
    FAIL_VERIFIER="$FAIL_DIR/TenantAwareCallbackVerifier.java"
    if [ -f "$FAIL_VERIFIER" ]; then
        if verify_callback_verifier \
                "$FAIL_VERIFIER" \
                "$FAIL_DIR/CallbackSignatureMismatchException.java" \
                "$FAIL_DIR/AmbiguousTenantResolutionException.java" \
                "failing-fixture" 2>/dev/null; then
            echo "callback_tenant_resolution_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "callback_tenant_resolution_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "callback_tenant_resolution_guard: failing-fixture SKIP — $FAIL_VERIFIER absent (symmetric extension reserved for next round)"
    fi
fi

exit "$OVERALL"
